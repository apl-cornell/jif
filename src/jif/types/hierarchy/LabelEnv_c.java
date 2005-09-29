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
        // clear the cache of leq results
        cache.clear();
        ph.add(p1, p2);
    }

    public void addEquiv(Principal p1, Principal p2) {
        // clear the cache of leq results
        cache.clear();
        ph.add(p1, p2);
        ph.add(p2, p1);
    }
    
    public void addAssertionLE(Label L1, Label L2) {
        // clear the cache of leq results
        cache.clear();
        // break up the components
        for (Iterator c = L1.components().iterator(); c.hasNext(); ) {
            Label cmp = (Label)c.next();

            // don't bother adding the assertion if we already know 
            // cmp is less than L2. However, if it has variables, we
            // need to add it regardless.
            if (cmp.hasVariables() || L2.hasVariables() || 
                    !(this.leq(cmp, L2, new SearchState_c(new HashSet())))) {
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
        return leq(L1.simplify(), L2.simplify(), 
                   new SearchState_c(new AssertionUseCount(), new HashSet()));
    }

    /**
     * Cache the results of leq(Label, Label, SearchState), when we are
     * using assertions only. Note that the rest of the
     * search state contains only information for pruning the search,
     * and so we can ignore it and consider only L1 and L2 when caching 
     * the results. 
     */
    private final Map cache = new HashMap();
    
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
        public String toString() {
            return lhs + "<=" + rhs;
        }
    }
    
    /** 
     * Recursive implementation of L1 <= L2.
     */
    public boolean leq(Label L1, Label L2, SearchState state) {  
        if (!((SearchState_c)state).useAssertions) {
            return leqImpl(L1.simplify(), L2.simplify(), state);
        }

        // only use the cache if we are using assertions.
        LeqGoal g = new LeqGoal(L1, L2);
    
        Boolean b = (Boolean)cache.get(g);
        if (b != null) {
            return b.booleanValue();
        }        
        boolean result = leqImpl(L1, L2, state);
        cache.put(g, Boolean.valueOf(result));
        return result;
    }
    
    /**
     * Non-caching implementation of L1 <= L2
     */
    private boolean leqImpl(Label L1, Label L2, SearchState state) {
        AssertionUseCount auc = ((SearchState_c)state).auc;
        Set currentGoals = ((SearchState_c)state).currentGoals;
        
        if (L1.isSingleton()) L1 = L1.singletonComponent();
        if (L2.isSingleton()) L2 = L2.singletonComponent();

        if (! L1.isComparable() || ! L2.isComparable()) {
            throw new InternalCompilerError("Cannot compare " + L1 +
                                            " with " + L2 + ".");
        }
        
                
        // do some easy tests firsts.
        if (L1.isBottom()) return true;
        //if (L2.isBottom()) return false;
        
        if (L2.isTop()) return true;
        if (L1.isTop()) return false;
        
        if (L2 instanceof RuntimeLabel) return L1.isRuntimeRepresentable();
        if (L1 instanceof RuntimeLabel) return false; // <= RT and TOP only.
         
        // check the current goals, to make sure we don't go into an infinite
        // recursion...
        LeqGoal newGoal = new LeqGoal(L1, L2);
        if (currentGoals.contains(newGoal)) {
            // already have this subgoal on the stack
            return false;
        }
        currentGoals = new HashSet(currentGoals);
        currentGoals.add(newGoal);
        state = new SearchState_c(auc, currentGoals);        
        
        if (L1.equals(L2)) return true;        

        // L1 <= L2 if there for all components of L1, there is one component
        // of L2 that is greater.  We need to filter out all L1, and L2
        // that are not enumerable.        
        if (! L1.isEnumerable()) {
            return L1.leq_(L2, this, state);        
        }
        if (! L1.isEnumerable() || ! L2.isEnumerable()) {
            throw new InternalCompilerError("Cannot compare " + L1 +
                                            " <= " + L2);
        }
                
        if (L2.isSingleton()) {
            L2 = L2.singletonComponent();
            boolean result = L1.leq_(L2, this, state);            
            if (!result && L1 instanceof ArgLabel) {
                ArgLabel al = (ArgLabel)L1;
                // recurse on upper bound.
                result = leq(al.upperBound(), L2, state);
            }
            
            // try to use assertions
            return result || leqApplyAssertions(L1, L2, (SearchState_c)state, true);
            // try again, being dumb?
        }
        else if (L1.isSingleton()) {
            // if the components of L2 are connected by joins, 
            // then L1 <= L2 if there exists a component cj of L2 
            // such that L1 <= cj
            for (Iterator j = L2.components().iterator(); j.hasNext(); ) {
                Label cj = (Label) j.next();
                if (leq(L1, cj, state)) {
                    return true;
                }
            }
            
            // haven't been able to prove it yet.
            // try testing L1 against all of L2. This is needed
            // if, say, L1 is an arg label with upper bound L join L',
            // and L2 = L join L'.
            if (L1.leq_(L2, this, state)) {
                return true;
            }
            
            if (L1 instanceof ArgLabel) {
                ArgLabel al = (ArgLabel)L1;
                // recurse on upper bound.
                if (leq(al.upperBound(), L2, state)) {
                    return true;
                }
            }

            // haven't been able to prove it yet. Try the assertions
            return leqApplyAssertions(L1, L2, (SearchState_c)state, true);
        }
        else {
            // L1 is not a singleton, and neither is L2.

            // We need to break L1 down...
            // if the components of L1 are connected by joins, 
            // then L1 <= L2 if for all components ci of L1 
            // we have ci <= L2
            for (Iterator i = L1.components().iterator(); i.hasNext(); ) {
                Label ci = (Label) i.next();
                if (!leq(ci, L2, state)) {
                    return false;
                }
            }	
            return true;
        }            
    }
    
    /**
     * Bound the number of times any particular assertion can be used; this bounds
     * the search in leqImpl.
     */
    private static final int ASSERTION_USE_BOUND = 2;

    /**
     * Bound the number different assertions that can be used; this bounds
     * the search in leqImpl.
     */
    private static final int ASSERTION_TOTAL_BOUND = 12;
        
    private boolean leqApplyAssertions(Label L1, Label L2, SearchState_c state, boolean beSmart) {
        AssertionUseCount auc = state.auc;
        if (!state.useAssertions || auc.size() >= ASSERTION_TOTAL_BOUND) return false;

        for (Iterator i = assertions.iterator(); i.hasNext();) { 
            LabelLeAssertion c = (LabelLeAssertion)i.next();

            if (auc.get(c) >= ASSERTION_USE_BOUND) {
                continue;
            }
            AssertionUseCount newAUC = new AssertionUseCount(auc);
            newAUC.use(c);
            SearchState newState = new SearchState_c(newAUC, state.currentGoals);

            Label cLHS = c.lhs();
            if (cLHS.hasVariables()) { 
                cLHS = this.solver.applyBoundsTo(c.lhs());
            }
            Label cRHS = c.rhs();
            if (cRHS.hasVariables()) { 
                cRHS = this.solver.applyBoundsTo(c.rhs());
            }
            if (beSmart) {
                // only use assertions that match one or the other of our labels
                if (!L1.equals(cLHS) && !L2.equals(cRHS)) {
                    continue;
                }
            }
            if (leq(L1, cLHS, newState) && 
                    leq(cRHS, L2, newState)) {
                return true;
            }
        }
        return false;

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
    
    /**
     * Class used to keep track of how many times each constraint has been used 
     * during the search to solve label inequality.
     */
    private static class AssertionUseCount {
        private final Map tally;
        AssertionUseCount() {
            this.tally = new HashMap();
        }
        AssertionUseCount(AssertionUseCount auc) {
            this.tally = new HashMap(auc.tally);
        }
        
        public int get(Assertion a) {
            Integer i = (Integer)tally.get(a);
            return i==null?0:i.intValue();
        }
        public void use(Assertion a) {
            tally.put(a, new Integer(get(a) + 1)); 
        }
        public int size() {
            return tally.size();
        }
        public String toString() {
            return tally.toString();
        }
    }
    private static class SearchState_c implements SearchState {
        public final AssertionUseCount auc;
        public final Set currentGoals;
        public final boolean useAssertions;
        SearchState_c(AssertionUseCount auc, Set currentGoals) {
            this.useAssertions = (auc != null);
            this.auc = auc;
            this.currentGoals = currentGoals;
        }
        SearchState_c(Set currentGoals) {
            this(null, currentGoals);
        }        
    }
}
