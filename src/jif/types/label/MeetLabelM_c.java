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

/** An implementation of the <code>MeetLabelM</code> interface. 
 */
public class MeetLabelM_c extends LabelM_c implements MeetLabelM
{
    private Set components;
    
    public MeetLabelM_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.components = Collections.unmodifiableSet(new LinkedHashSet(flatten(components)));        
    }
    
    public boolean isRuntimeRepresentable() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelM c = (LabelM) i.next();
            
            if (! c.isRuntimeRepresentable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean isCanonical() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelM c = (LabelM) i.next();
            
            if (! c.isCanonical()) {
                return false;
            }
        }
        
        return true;
    }
    
    protected boolean isDisambiguatedImpl() {
        return true;
    }

    public boolean isTop() {
        return components.isEmpty();
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof MeetLabelM_c) {
            MeetLabelM_c that = (MeetLabelM_c)o;
            return this.components.equals(that.components);
        }
        if (o instanceof LabelM) {
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
            LabelM c = (LabelM) i.next();
            s += c.componentString(printedLabels);
            
            if (i.hasNext()) {
                s += "; ";
            }
        }
        
        return s;
    }
    
    public boolean leq_(LabelM L, LabelEnv env) {
        throw new InternalCompilerError("Should never be called");
    }
    
    public Collection components() {
        return Collections.unmodifiableCollection(components);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    public LabelM simplify() {        
        if (!this.isDisambiguated() || components.isEmpty()) {
            return this;
        }
        
        Collection comps = flatten(components);
        Set needed = new LinkedHashSet();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            LabelM ci = ((LabelM) i.next()).simplify();
            
            boolean subsumed = false;
            
            for (Iterator j = needed.iterator(); j.hasNext(); ) {
                LabelM cj = (LabelM) j.next();
                
                if (jts.leq(cj, ci)) {
                    subsumed = true;
                    break;
                }
                
                if (jts.leq(ci, cj)) { 
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
            return (LabelM)needed.iterator().next();
        }

        return new MeetLabelM_c(needed, (JifTypeSystem)ts, position());
    }
    
    private static Collection flatten(Collection comps) {
        Collection c = new LinkedHashSet();
        
        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            LabelM L = (LabelM) i.next();
            
            if (L.isBottom()) {
                return Collections.singleton(L);
            }
            
            if (L instanceof MeetLabelM_c) {
                Collection lComps = flatten(((MeetLabelM_c)L).components());
                
                for (Iterator j = lComps.iterator(); j.hasNext(); ) {
                    LabelM Lj = (LabelM) j.next();
                    
                    if (Lj.isBottom()) {
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
            LabelM L = (LabelM) i.next();
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes; 
    }

    public LabelM subst(LabelSubstitution substitution) throws SemanticException {        
        if (components.isEmpty() || substitution.stackContains(this)) {
            return substitution.substLabelM(this);
        }
        substitution.pushLabel(this);
        boolean changed = false;
        Set s = new LinkedHashSet();
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelM c = (LabelM) i.next();
            LabelM newc = c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }
        
        substitution.popLabel(this);
        
        if (!changed) return substitution.substLabelM(this);
        
        JifTypeSystem ts = (JifTypeSystem)this.typeSystem();
        LabelM newMeetLabelM = new MeetLabelM_c(flatten(s), ts, position());
        return substitution.substLabelM(newMeetLabelM);
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
            LabelM c = (LabelM) i.next();
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);            
        }
        return X;
    }
    
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava((MeetLabelM)this, rw);
    }    
    
}
