package jif.types.hierarchy;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.util.InternalCompilerError;

/**
 * The wrapper of a set of assumptions that can be used to decide
 * whether L1 &lt;= L2. 
 */
public class LabelEnv_c implements LabelEnv
{
    private final PrincipalHierarchy ph;
    private final List assertions;
    private JifTypeSystem ts;
    private Solver solver;
    
    /**
     * Do any of the assertions have variables in them?
     */
    private boolean hasVariables;
    
    public LabelEnv_c(JifTypeSystem ts) {
        this(ts, new PrincipalHierarchy(), new LinkedList(), false);
    }
    private LabelEnv_c() {
        this(null);
    }
    private LabelEnv_c(JifTypeSystem ts, PrincipalHierarchy ph, List assertions, boolean hasVariables) {
        this.ph = ph;
        this.assertions = assertions;
        this.hasVariables = false;
        this.solver = null;        
        this.hasVariables = hasVariables;
        this.ts = ts;
    }
    
    public void setSolver(Solver s) {
        if (this.solver == null) {
            this.solver = s;
        }
        else if (this.solver != s) {
            throw new InternalCompilerError("LabelEnv given two different solvers");
        }
    }
    
    public PrincipalHierarchy ph() {
        return ph;
    }
    
    public boolean hasVariables() {
        return this.hasVariables;
    }
    
    public void addActsFor(Principal p1, Principal p2) {
        ph.add(p1, p2);
    }
    
    public void addAssertionLE(Label L1, Label L2) {
        // break up the components
        for (Iterator c = L1.components().iterator(); c.hasNext(); ) {
            Label cmp = (Label)c.next();

            // don't bother adding the assertion if we already know 
            // cmp is less than L2. However, if it has variables, we
            // need to add it regardless.
            if (cmp.hasVariables() || L2.hasVariables() || !(this.leq(cmp, L2, false, new HashSet()))) {
                assertions.add(new LabelLeAssertion_c(ts, cmp, L2));
                if (!this.hasVariables && (cmp.hasVariables() || L2.hasVariables())) {
                    // at least one assertion in this label env has a variable.
                    this.hasVariables = true;
                }
            }            
        }
    }
    
    public LabelEnv copy() {
        return new LabelEnv_c(ts, ph.copy(), new LinkedList(assertions), hasVariables);
    }
    
    public boolean leq(Label L1, Label L2) {
        return leq(L1, L2, true, new HashSet());
    }
    
    private static class LeqGoal {
        final Label lhs;
        final Label rhs;
        LeqGoal(Label lhs, Label rhs) { 
            this.lhs = lhs;
            this.rhs = rhs;
        }
        public int hashCode() {
            return lhs.hashCode() + rhs.hashCode();
        }
        public boolean equals(Object o) {
            if (o instanceof LeqGoal) {
                LeqGoal that = (LeqGoal)o;
                return this.lhs.equals(that.lhs) && this.rhs.equals(that.rhs);
                
            }
            return false;
        }
    }
    /** Indirect through the TS so extensions can support new label types.
     * Label.leq does not handle non-singleton or non-enumerable labels. */
    protected boolean leq(Label Lb1, Label Lb2, boolean useAssertions, Set currentGoals
    /*boolean boundVars*/) {
        // simplify the two labels
        Label L1 = Lb1.simplify();
        Label L2 = Lb2.simplify();
        
        if (L1.isSingleton()) L1 = L1.singletonComponent();
        if (L2.isSingleton()) L2 = L2.singletonComponent();

        if (! L1.isComparable() || ! L2.isComparable()) {
            throw new InternalCompilerError("Cannot compare " + L1 +
                                            " with " + L2 + ".");
        }
        
        // L1 <= L2 if there for all components of L1, there is one component
        // of L2 that is greater.  We need to filter out all L1, and L2
        // that are not enumerable.
        
        if (L1.isBottom()) return true;
        //if (L2.isBottom()) return false;
        
        if (L2.isTop()) return true;
        if (L1.isTop()) return false;
        
        if (L2 instanceof RuntimeLabel) return L1.isRuntimeRepresentable();
        if (L1 instanceof RuntimeLabel) return false; // <= RT and TOP only.
                
        if (L1.equals(L2)) return true;
        
        if (! L1.isEnumerable()) return L1.leq_(L2, this);
        
        if (! L1.isEnumerable() || ! L2.isEnumerable()) {
            throw new InternalCompilerError("Cannot compare " + L1 +
                                            " <= " + L2);
        }
        
        LeqGoal newGoal = new LeqGoal(L1, L2);
        if (currentGoals.contains(newGoal)) {
            // already have this subgoal on the stack
            return false;
        }
        else {
            currentGoals = new HashSet(currentGoals);
            currentGoals.add(newGoal);
        }
        if (L2.isSingleton()) {
            L2 = L2.singletonComponent();
            boolean result = L1.leq_(L2, this);
            
            if (result == true) { return true; }
            
            if (L1 instanceof ArgLabel) {
                ArgLabel al = (ArgLabel)L1;
                // recurse on upper bound.
                result = leq(al.upperBound(), L2, useAssertions, currentGoals);
            }
            
            if (result == true || !useAssertions) 
                return result;
            
            // try to use assertions
            for (Iterator i = assertions.iterator(); i.hasNext();) { 
                LabelLeAssertion c = (LabelLeAssertion) i.next();
                // FIXME: keep check of the visited constraints to avoid
                // infinite loops.
                    Label cLHS = c.lhs();
                if (cLHS.hasVariables()) 
                    cLHS = this.solver.applyBoundsTo(c.lhs());
                    Label cRHS = c.rhs();
                if (cRHS.hasVariables()) 
                    cRHS = this.solver.applyBoundsTo(c.rhs());
                    
                if (leq(L1, cLHS, false, currentGoals) && leq(cRHS, L2, false, currentGoals)) {
                        return true;
                    }                    
                }
            
            return false;
        }
        
        if (L1.isSingleton()) {
            // if the components of L2 are connected by joins, 
            // then L1 <= L2 if there exists a component cj of L2 
            // such that L1 <= cj
            for (Iterator j = L2.components().iterator(); j.hasNext(); ) {
                Label cj = (Label) j.next();
                if (leq(L1, cj, useAssertions, currentGoals)) {
                    return true;
                }
            }
            
            // haven't been able to prove it yet.
            // try testing L1 against all of L2. This is needed
            // if, say, L1 is an arg label with upper bound L join L',
            // and L2 = L join L'.
            if (L1.leq_(L2, this)) {
                return true;
            }
            
            if (L1 instanceof ArgLabel) {
                ArgLabel al = (ArgLabel)L1;
                // recurse on upper bound.
                if (leq(al.upperBound(), L2, useAssertions, currentGoals)) {
                    return true;
                }
            }

            // haven't been able to prove it yet.
            // Try to use the constraints to show that L1 <= L2, even
            // though L2 is not a singleton. This is useful in cases 
            // where the constraints do not have singleton RHS, e.g.
            // trying to prove {L1} <= {L2;L3} when the environment 
            // contains {L1} <= {L2;L3}.
            for (Iterator i = assertions.iterator(); i.hasNext();) { 
                LabelLeAssertion c = (LabelLeAssertion)i.next();
                Label cLHS = c.lhs();
                if (cLHS.hasVariables()) { 
                    cLHS = this.solver.applyBoundsTo(c.lhs());
                }
                Label cRHS = c.rhs();
                if (cRHS.hasVariables()) { 
                    cRHS = this.solver.applyBoundsTo(c.rhs());
                }
                if (leq(L1, cLHS, false, currentGoals) && leq(cRHS, L2, false, currentGoals)) {
                    return true;
                }
            }
            return false;
        }
        else {
            // L1 is not a singleton, and neither is L2. 
            // We need to break L1 down...
            // if the components of L1 are connected by joins, 
            // then L1 <= L2 if for all components ci of L1 
            // we have ci <= L2
            for (Iterator i = L1.components().iterator(); i.hasNext(); ) {
                Label ci = (Label) i.next();
                if (!leq(ci, L2, useAssertions, currentGoals)) {
                    return false;
                }
            }	
            return true;
        }            
    }
    
    /**
     * Is this enviornment empty, or does is contain some constraints?
     */
    public boolean isEmpty() {
        return assertions.isEmpty() && ph.isEmpty();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (Iterator i = assertions.iterator(); i.hasNext(); ) {
            LabelLeAssertion c = (LabelLeAssertion) i.next();
            sb.append(c.lhs());
            sb.append(" <= ");
            sb.append(c.rhs());
            if (i.hasNext())
                sb.append(", ");
        }
        if (!ph().isEmpty()) {
            if (!assertions.isEmpty()) {
                sb.append(", ");
            }
            sb.append(ph().actsForString());
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Returns a Map of Strings to List[String]s which is the descriptions of any 
     * components that appear in the environment. This map is used for verbose 
     * output to the user, to help explain the meaning of constraints and 
     * labels.
     * 
     * Seen components is a Set of Labels whose definitions will not be 
     * displayed.
     */
    public Map definitions(VarMap bounds, Set seenComponents) {
        Map defns = new LinkedHashMap();
        
        Set labelComponents = new LinkedHashSet();
        for (Iterator iter = assertions.iterator(); iter.hasNext(); ) {
            LabelLeAssertion c = (LabelLeAssertion) iter.next();
            Label bound = bounds.applyTo(c.lhs());
            if (bound.isEnumerable() && !bound.components().isEmpty()) {                
                for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                    Label l = (Label)i.next();
                    labelComponents.add(l);
                }
            }
            else {
                labelComponents.add(bound);                
            }
            
            bound = bounds.applyTo(c.rhs());
            if (bound.isEnumerable() && !bound.components().isEmpty()) {                
                for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                    Label l = (Label)i.next();
                    labelComponents.add(l);
                }
            }            
            else {
                labelComponents.add(bound);                
            }
        }
        
        labelComponents.removeAll(seenComponents);
        
        for (Iterator iter = labelComponents.iterator(); iter.hasNext(); ) {
            Label l = (Label)iter.next();
            if (l.description() != null) {
                String s = l.componentString();
                if (s.length() == 0)
                    s = l.toString();
                defns.put(s, Collections.singletonList(l.description()));
            }
        } 
        
        return defns;
    }    
}
