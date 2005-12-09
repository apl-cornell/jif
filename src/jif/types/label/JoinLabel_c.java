package jif.types.label;

import java.util.*;

import jif.translate.JoinLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JoinLabel</code> interface. 
 */
public class JoinLabel_c extends Label_c implements JoinLabel
{
    private Set components;
    
    public JoinLabel_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos, new JoinLabelToJavaExpr_c());
        this.components = Collections.unmodifiableSet(new LinkedHashSet(flatten(components)));
        
        if (this.components.isEmpty()) {
            throw new InternalCompilerError("Join label must be nonempty");
        }
    }
    
    public boolean isRuntimeRepresentable() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isRuntimeRepresentable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean isCanonical() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isCanonical()) {
                return false;
            }
        }
        
        return true;
    }
    
    protected boolean isDisambiguatedImpl() {
        return true;
    }
    /**
     * @return true iff this label is covariant.
     *
     * A label is covariant if it contains at least one covariant component.
     */
    public boolean isCovariant() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (c.isCovariant()) {
                return true;
            }
        }
        
        return false;
    }
    public boolean isComparable() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isComparable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean isEnumerable() {
        return true;
    }
    
    public boolean isBottom() {
        if (components.isEmpty()) return false;
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isBottom()) {
                return false;
            }
        }
        // all components are bottom.
        return true;
    }

    public boolean isTop() {
        if (components.isEmpty()) return false;
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isTop()) {
                return false;
            }
        }
        // all components are top
        return true;
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof JoinLabel_c) {
            JoinLabel_c that = (JoinLabel_c)o;
            return this.components.equals(that.components);
        }
        if (o instanceof Label) {
            // see if it matches a singleton
            return this.components.equals(Collections.singleton(o));
        }
        return false;
    }
    public int hashCode() {
        return components.hashCode();
    }
    
    public String componentString(Set printedLabels) {
        String s = "";
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            s += c.componentString(printedLabels);
            
            if (i.hasNext()) {
                s += "; ";
            }
        }
        
        return s;
    }
    
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        if (! L.isSingleton() || ! L.isComparable() || ! L.isEnumerable())
            throw new InternalCompilerError("Cannot compare " + L);
        
        // If this = { .. Pi .. } and L = { .. Pj' .. }, check if for all i,
        // there exists a j, such that Pi <= Pj'
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label ci = (Label) i.next();
            
            if (! env.leq(ci, L, state)) {
                return false;
            }
        }
        
        return true;
    }
    
    public Collection components() {
        return Collections.unmodifiableCollection(components);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    public Label simplify() {
        if (!this.isDisambiguated() || components.isEmpty()) {
            return this;
        }

        Collection comps = flatten(components);
        Set needed = new LinkedHashSet();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Label ci = ((Label) i.next()).simplify();
            
            if (ci.hasVariables()) {
                needed.add(ci);
            }
            else {
                boolean subsumed = false;
                
                for (Iterator j = needed.iterator(); j.hasNext(); ) {
                    Label cj = (Label) j.next();
                    
                    if (cj.hasVariables()) {
                        continue;
                    }

                    if (jts.leq(ci, cj)) {
                        subsumed = true;
                        break;
                    }
                    
                    if (jts.leq(cj, ci)) { 
                        j.remove();
                    }
                }
                
                if (! subsumed)
                    needed.add(ci);
            }
        }
        
        if (needed.equals(components)) {
            return this;
        }
        if (needed.size() == 1) {
            return (Label)needed.iterator().next();
        }

        return new JoinLabel_c(needed, (JifTypeSystem)ts, position());
    }
    
    private static Collection flatten(Collection comps) {
        Collection c = new LinkedHashSet();
        
        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Label L = (Label) i.next();
            
            if (L.isTop()) {
                return Collections.singleton(L);
            }
            
            if (L instanceof JoinLabel_c) {
                Collection lComps = flatten(L.components());
                
                for (Iterator j = lComps.iterator(); j.hasNext(); ) {
                    Label Lj = (Label) j.next();
                    
                    if (Lj.isTop()) {
                        return Collections.singleton(Lj);
                    }
                    
                    c.add(Lj);
                }
            }
            else {
                c.add(L);
            }
        }
        
        return c;
    }

    public List throwTypes(TypeSystem ts) {
        List throwTypes = new ArrayList();
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label L = (Label) i.next();
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes; 
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {        
        if (components.isEmpty() || substitution.stackContains(this)) {
            return substitution.substLabel(this);
        }
        substitution.pushLabel(this);
        boolean changed = false;
        Set s = new LinkedHashSet();
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            Label newc = c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }
        
        substitution.popLabel(this);
        
        if (!changed) return substitution.substLabel(this);
        
        JifTypeSystem ts = (JifTypeSystem)this.typeSystem();
        Label newJoinLabel = ts.joinLabel(this.position(), flatten(s));
        return substitution.substLabel(newJoinLabel).simplify();
    }

    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());
        
        if (components.isEmpty()) {
            return X;
        }

        A = (JifContext)A.pushBlock();
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            A.setPc(X.N());
            Label c = (Label) i.next();
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);            
        }
        return X;
    }
    
}
