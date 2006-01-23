package jif.types.label;

import java.util.*;

import jif.translate.JoinLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** Represents the meet of a number of policies. 
 */
public abstract class MeetPolicy_c extends Policy_c {
    private Set meetComponents;
    
    public MeetPolicy_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.meetComponents = Collections.unmodifiableSet(new LinkedHashSet(flatten(components)));
        if (this.meetComponents.isEmpty()) {
            throw new InternalCompilerError("Empty collection!");
        }
    }
    
    public boolean isSingleton() {
        return meetComponents.size() == 1;
    }
    public boolean isCanonical() {
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy c = (Policy) i.next();            
            if (! c.isCanonical()) {
                return false;
            }
        }        
        return true;
    }
    public boolean isRuntimeRepresentable() {
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy c = (Policy) i.next();            
            if (! c.isRuntimeRepresentable()) {
                return false;
            }
        }        
        return true;
    }
            
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof MeetPolicy_c) {
            MeetPolicy_c that = (MeetPolicy_c)o;
            return this.meetComponents.equals(that.meetComponents);
        }
        if (o instanceof Policy) {
            // see if it matches a singleton
            return this.meetComponents.equals(Collections.singleton(o));
        }
        return false;
    }
    public int hashCode() {
        return meetComponents.hashCode();
    }
    
    public String toString() {
        String s = "";
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy c = (Policy) i.next();
            s += c.toString();
            
            if (i.hasNext()) {
                s += " meet ";
            }
        }
        
        return s;
    }
    
    protected boolean leq_(Policy p, LabelEnv env) {
        // If this = { .. Pi .. , check there exists an i
        // such that Pi <= p
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy pi = (Policy) i.next();
            
            if (env.leq(pi, p)) {
                return true;
            }
        }
        
        return false;
    }
    
    public Collection meetComponents() {
        return Collections.unmodifiableCollection(meetComponents);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    public Policy simplify() {
        if (meetComponents.isEmpty()) {
            return this;
        }
        if (meetComponents.size() == 1) {
            return ((Policy)meetComponents.iterator().next()).simplify();
        }

        Collection comps = flatten(meetComponents);
        Set needed = new LinkedHashSet();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Policy ci = ((Policy) i.next()).simplify();
            
            boolean subsumed = false;
            
            for (Iterator j = needed.iterator(); j.hasNext(); ) {
                Policy cj = (Policy) j.next();
                
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
        
        if (needed.equals(meetComponents)) {
            return this;
        }
        if (needed.size() == 1) {
            return (Policy)needed.iterator().next();
        }

        return constructMeetPolicy(needed, position);
    }
    
    protected abstract Policy constructMeetPolicy(Collection components, Position pos);
    
    private static Collection flatten(Collection comps) {
        Collection c = new LinkedHashSet();        
        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Policy p = (Policy) i.next();            
            if (p.isBottom()) {
                return Collections.singleton(p);
            }            
            if (p instanceof MeetPolicy_c) {
                Collection lComps = flatten(((MeetPolicy_c)p).meetComponents());                
                for (Iterator j = lComps.iterator(); j.hasNext(); ) {
                    Policy pj = (Policy) j.next();                    
                    if (pj.isBottom()) {
                        return Collections.singleton(pj);
                    }                    
                    c.add(pj);
                }
            }
            else {
                c.add(p);
            }
        }
        
        return c;
    }

    public List throwTypes(TypeSystem ts) {
        List throwTypes = new ArrayList();
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy L = (Policy) i.next();
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes; 
    }

    public Policy subst(LabelSubstitution substitution) throws SemanticException {        
        if (meetComponents.isEmpty()) {
            return substitution.substPolicy(this).simplify();
        }
        boolean changed = false;
        Set s = new LinkedHashSet();
        
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy c = (Policy) i.next();
            Policy newc = c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }
        
        if (!changed) return substitution.substPolicy(this).simplify();
        
        Policy newMeetPolicy = constructMeetPolicy(flatten(s), position);
        return substitution.substPolicy(newMeetPolicy).simplify();
    }

    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());
        
        if (meetComponents.isEmpty()) {
            return X;
        }

        A = (JifContext)A.pushBlock();
        
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            A.setPc(X.N());
            Policy c = (Policy) i.next();
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);            
        }
        return X;
    }

    public boolean isTop() {
        // top if all policies is top
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy c = (Policy) i.next();            
            if (!c.isTop()) {
                return false;
            }
        }        
        return true;
    }
    public boolean isBottom() {
        // bottom if any policy is bottom
        for (Iterator i = meetComponents.iterator(); i.hasNext(); ) {
            Policy c = (Policy) i.next();            
            if (c.isBottom()) {
                return true;
            }
        }        
        return false;
    }
}
