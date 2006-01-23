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
    private final Set components;
    
    public JoinLabel_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos, new JoinLabelToJavaExpr_c());
        this.components = Collections.unmodifiableSet(new LinkedHashSet(flatten(components)));
        if (this.components.isEmpty()) throw new InternalCompilerError("No empty joins");
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
        return (components.isEmpty());
    }

    public boolean isTop() {
        if (components.isEmpty()) return false;
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            if (c.isTop()) {
                return true;
            }
        }
        return false;
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
    
    public String toString() {
        if (isTop()) return "<top>";
        return super.toString();
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
        if (! L.isComparable() || ! L.isEnumerable())
            throw new InternalCompilerError("Cannot compare " + L);
        
        // If this = {c1 join ... join cn } , check if for all i,
        // we have Pi <= L
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label ci = (Label) i.next();
            
            if (! env.leq(ci, L, state)) {
                return false;
            }
        }
        
        return true;
    }
    
    public Collection joinComponents() {
        return Collections.unmodifiableCollection(components);
    }

    public Label normalize() {
        if (components.size() == 1) {
            return (Label)components.iterator().next();
        }
        // if there is more than one PairLabel, combine them.
        JifTypeSystem ts = (JifTypeSystem)typeSystem();
        PairLabel pl = null;
        boolean combinedPL = false;
        Set nonPairLabels = new LinkedHashSet();
        for (Iterator iter = joinComponents().iterator(); iter.hasNext();) {
            Label lbl = (Label)iter.next();
            if (lbl instanceof PairLabel) {
                PairLabel p = (PairLabel)lbl;
                if (pl == null) {
                    pl = p;
                }
                else {
                    combinedPL = true;
                    pl = ts.pairLabel(position(),
                                      pl.confPolicy().join(p.confPolicy()),
                                      pl.integPolicy().join(p.integPolicy()));                    
                }
            }
            else {
                nonPairLabels.add(lbl);
            }
        }
        if (combinedPL) {
            nonPairLabels.add(pl);
            return ts.joinLabel(position(), nonPairLabels);
        }
        return this;
    }
    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    protected Label simplifyImpl() {
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
            
            if (L instanceof JoinLabel) {
                Collection lComps = flatten(((JoinLabel)L).joinComponents());
                
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

    public Set variableComponents() {
        Set s = new LinkedHashSet();
        for (Iterator iter = joinComponents().iterator(); iter.hasNext();) {
            Label ci = (Label)iter.next();
            s.addAll(ci.variableComponents());
        }
        return s;
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
