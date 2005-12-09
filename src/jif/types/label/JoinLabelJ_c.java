package jif.types.label;

import java.util.*;

import jif.translate.JifToJavaRewriter;
import jif.translate.JoinLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JoinLabelJ</code> interface. 
 */
public class JoinLabelJ_c extends LabelJ_c implements JoinLabelJ
{
    private Set components;
    
    public JoinLabelJ_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.components = Collections.unmodifiableSet(new LinkedHashSet(flatten(components)));        
    }
    
    public boolean isRuntimeRepresentable() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelJ c = (LabelJ) i.next();
            
            if (! c.isRuntimeRepresentable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean isCanonical() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelJ c = (LabelJ) i.next();
            
            if (! c.isCanonical()) {
                return false;
            }
        }
        
        return true;
    }
    
    protected boolean isDisambiguatedImpl() {
        return true;
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof JoinLabelJ_c) {
            JoinLabelJ_c that = (JoinLabelJ_c)o;
            return this.components.equals(that.components);
        }
        if (o instanceof LabelJ) {
            // see if it matches a singleton
            return this.components.equals(Collections.singleton(o));
        }
        return false;
    }
    
    public boolean isBottom() {
        return components.isEmpty();
    }
    public int hashCode() {
        return components.hashCode();
    }
    
    public String componentString(Set printedLabels) {
        String s = "";
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelJ c = (LabelJ) i.next();
            s += c.componentString(printedLabels);
            
            if (i.hasNext()) {
                s += "; ";
            }
        }
        
        return s;
    }
    
    public boolean leq_(LabelJ L, LabelEnv env) {
        // If this = { .. Pi .. } and L = { .. Pj' .. }, check if for all i,
        // there exists a j, such that Pi <= Pj'
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelJ ci = (LabelJ) i.next();
            
            if (! env.leq(ci, L)) {
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
    public LabelJ simplify() {
        if (!this.isDisambiguated() || components.isEmpty()) {
            return this;
        }

        Collection comps = flatten(components);
        Set needed = new LinkedHashSet();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            LabelJ ci = ((LabelJ) i.next()).simplify();
            
            boolean subsumed = false;
            
            for (Iterator j = needed.iterator(); j.hasNext(); ) {
                LabelJ cj = (LabelJ) j.next();
                
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
        
        if (needed.equals(components)) {
            return this;
        }
        if (needed.size() == 1) {
            return (LabelJ)needed.iterator().next();
        }

        return new JoinLabelJ_c(needed, (JifTypeSystem)ts, position());
    }
    
    private static Collection flatten(Collection comps) {
        Collection c = new LinkedHashSet();
        
        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            LabelJ L = (LabelJ) i.next();
            
            if (L.isTop()) {
                return Collections.singleton(L);
            }
            
            if (L instanceof JoinLabelJ_c) {
                Collection lComps = flatten(((JoinLabelJ_c)L).components());
                
                for (Iterator j = lComps.iterator(); j.hasNext(); ) {
                    LabelJ Lj = (LabelJ) j.next();
                    
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
            LabelJ L = (LabelJ) i.next();
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes; 
    }

    public LabelJ subst(LabelSubstitution substitution) throws SemanticException {        
        if (components.isEmpty() || substitution.stackContains(this)) {
            return substitution.substLabelJ(this);
        }
        substitution.pushLabel(this);
        boolean changed = false;
        Set s = new LinkedHashSet();
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelJ c = (LabelJ) i.next();
            LabelJ newc = c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }
        
        substitution.popLabel(this);
        
        if (!changed) return substitution.substLabelJ(this);
        
        JifTypeSystem ts = (JifTypeSystem)this.typeSystem();
        LabelJ newJoinLabelJ = new JoinLabelJ_c(flatten(s), ts, position());
        return substitution.substLabelJ(newJoinLabelJ);
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
            LabelJ c = (LabelJ) i.next();
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);            
        }
        return X;
    }
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava((JoinLabelJ)this, rw);
    }    
}
