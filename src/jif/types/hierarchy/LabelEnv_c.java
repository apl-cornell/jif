package jif.types.hierarchy;

import java.util.*;

import jif.Topics;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.util.*;

/**
 * The wrapper of a set of assumptions that can be used to decide
 * whether L1 &lt;= L2. 
 */
public class LabelEnv_c implements LabelEnv
{
    protected final PrincipalHierarchy ph;
    protected final List labelAssertions; // a list of LabelLeAssertions
    protected final StringBuffer displayLabelAssertions; 
    protected final JifTypeSystem ts;
    protected Solver solver;
    
    /**
     * Do any of the assertions have variables in them?
     */
    protected boolean hasVariables;

    /**
     * Topics to report
     */
    protected static Collection topics = CollectionUtil.list(Topics.jif, Topics.labelEnv);

    public LabelEnv_c(JifTypeSystem ts, boolean useCache) {
        this(ts, new PrincipalHierarchy(), new LinkedList(), "", false, useCache);
    }
    protected LabelEnv_c(JifTypeSystem ts, PrincipalHierarchy ph, List assertions, String displayLabelAssertions, boolean hasVariables, boolean useCache) {
        this.ph = ph;
        this.labelAssertions = assertions;
        this.displayLabelAssertions = new StringBuffer(displayLabelAssertions);
        this.hasVariables = false;
        this.solver = null;        
        this.hasVariables = hasVariables;
        this.ts = ts;
        this.useCache = useCache;
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
        addAssertionLE(L1, L2, true);
    }
    public boolean addAssertionLE(Label L1, Label L2, boolean updateDisplayString) {
        // clear the cache of leq results
        cache.clear();
        boolean added = false;
        // break up the components
        if (L1 instanceof JoinLabel) {
            for (Iterator c = ((JoinLabel)L1).joinComponents().iterator(); c.hasNext(); ) {
                Label cmp = (Label)c.next();
                added = addAssertionLE(cmp, L2, false) || added;                
            }            
        }
        else if (L2 instanceof MeetLabel) {
            for (Iterator c = ((MeetLabel)L2).meetComponents().iterator(); c.hasNext(); ) {
                Label cmp = (Label)c.next();
                added = addAssertionLE(L1, cmp, false) || added;                
            }                        
        }
        else {
            // don't bother adding the assertion if we already know 
            // L1 is less than L2. However, if it has variables, we
            // need to add it regardless.
            if (L1.hasVariables() || L2.hasVariables() || 
                    !(this.leq(L1, L2, new SearchState_c(new LinkedHashSet())))) {
                labelAssertions.add(new LabelLeAssertion_c(ts, L1, L2, Position.COMPILER_GENERATED));
                added = true;
                if (!this.hasVariables && (L1.hasVariables() || L2.hasVariables())) {
                    // at least one assertion in this label env has a variable.
                    this.hasVariables = true;
                }
            }            
        }
        
        if (updateDisplayString && added) {
            if (displayLabelAssertions.length() > 0) {
                displayLabelAssertions.append(", ");
            }
            displayLabelAssertions.append(L1 + " <= " + L2);
        }
        return added;
    }

    public void addEquiv(Label L1, Label L2) {
        addAssertionLE(L1, L2, false);
        addAssertionLE(L2, L1, false);
        if (displayLabelAssertions.length() > 0) {
            displayLabelAssertions.append(", ");
        }
        displayLabelAssertions.append(L1 + " equiv " + L2);        
    }
    
    public LabelEnv copy() {
        return new LabelEnv_c(ts, ph.copy(), new LinkedList(labelAssertions), 
                              displayLabelAssertions.toString(), 
                              hasVariables, useCache);
    }
    
    public boolean leq(Label L1, Label L2) { 
        if (Report.should_report(topics, 1))
            Report.report(1, "Testing " + L1 + " <= " + L2);
        
        return leq(L1, L2, 
                   new SearchState_c(new AssertionUseCount(), new LinkedHashSet()));
    }

    /*
     * Cache the results of leq(Label, Label, SearchState), when we are
     * using assertions only. Note that the rest of the
     * search state contains only information for pruning the search,
     * and so we can ignore it and consider only L1 and L2 when caching 
     * the results. 
     */
    private final Map cache = new HashMap();
    
    protected final boolean useCache;
    
    private static class LeqGoal {
        final Object lhs;
        final Object rhs;
        LeqGoal(Policy lhs, Policy rhs) { 
            this.lhs = lhs; this.rhs = rhs;
            if (lhs == null || rhs == null) 
                throw new InternalCompilerError("Null policy!");
        }
        LeqGoal(Label lhs, Label rhs) { 
            this.lhs = lhs; this.rhs = rhs;
            if (lhs == null || rhs == null) 
                throw new InternalCompilerError("Null label!");
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
        if (!useCache || 
                !((SearchState_c)state).useAssertions || 
                this.hasVariables()) {
            if (Report.should_report(topics, 3))
                Report.report(3, "Not using cache for " + L1 + " <= " + L2 + 
                              " : useCache = " + useCache + 
                              "; state.useAssertions = " +((SearchState_c)state).useAssertions + 
                              "; this.hasVariables() = " + this.hasVariables());
            return leqImpl(L1, L2, state);
        }

        // only use the cache if we are using assertions, and there are no
        // variables
        LeqGoal g = new LeqGoal(L1, L2);
    
        Boolean b = (Boolean)cache.get(g);
        if (b != null) {
            if (Report.should_report(topics, 3))
                Report.report(3, "Found cache value for " + L1 + " <= " + L2 
                              + " : " + b.booleanValue());
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
        
        L1 = L1.normalize();
        L2 = L2.normalize();

        if (L1 instanceof WritersToReadersLabel) {
            Label tL1 = triggerTransforms(L1).normalize();
            if (Report.should_report(topics, 3))
                Report.report(3, "Transforming " + L1 + " to " + tL1);
            if (!L1.equals(tL1)) return leq(tL1, L2, state); 
        }
        if (L2 instanceof WritersToReadersLabel) {
            Label tL2 = triggerTransforms(L2).normalize(); 
            if (Report.should_report(topics, 3))
                Report.report(3, "Transforming " + L2 + " to " + tL2);
            if (!L2.equals(tL2)) return leq(L1, tL2, state); 
        }

        if (! L1.isComparable() || ! L2.isComparable()) {
            if (Report.should_report(topics, 3))
                Report.report(3, "Goal " + L1 + " <= " + L2 + " already on goal stack");
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
            if (Report.should_report(topics, 3))
                Report.report(3, "Goal " + L1 + " <= " + L2 + " already on goal stack");
            return false;
        }
        currentGoals = new LinkedHashSet(currentGoals);
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
        
        if (L2 instanceof MeetLabel) {
            // L1 <= C1 meet ... meet Cn if
            // for all j L1 <= Cj
            MeetLabel ml = (MeetLabel)L2;
            boolean allSat = true;
            for (Iterator j = ml.meetComponents().iterator(); j.hasNext(); ) {
                Label cj = (Label) j.next();
                if (!leq(L1, cj, state)) {
                    allSat = false;
                    break;
                }
            }
            if (allSat) return true;            
        }
        if (L2 instanceof JoinLabel) {
            // L1 <= c1 join ... join cn if there exists a cj 
            // such that L1 <= cj
            JoinLabel jl = (JoinLabel)L2;
            for (Iterator j = jl.joinComponents().iterator(); j.hasNext(); ) {
                Label cj = (Label) j.next();
                if (leq(L1, cj, state)) {
                    return true;
                }
            }            
        }
        
        if (L1.leq_(L2, this, state)) {
            return true;
        }
        
        if (L1 instanceof ArgLabel) {
            ArgLabel al = (ArgLabel)L1;
            // recurse on upper bound.
            if (leq(al.upperBound(), L2, state))
                return true;
        }
        
        if (L1 instanceof MeetLabel || L1 instanceof JoinLabel ||
                L2 instanceof MeetLabel || L2 instanceof JoinLabel) {
            // see if using a conf and integ projections will work
            ConfPolicy conf1 = ts.confProjection(L1);
            ConfPolicy conf2 = ts.confProjection(L2);
            IntegPolicy integ1 = ts.integProjection(L1);
            IntegPolicy integ2 = ts.integProjection(L2);
                        
            if (leq(conf1, conf2, state) && leq(integ1, integ2, state)) {
                    return true;
            }
        }
        
        // try to use assertions
        return leqApplyAssertions(L1, L2, (SearchState_c)state, true);
        
    }
    
    static int j = 0;
    
//    /**
//     * Non-caching implementation of L1 <= L2
//     */
//    private boolean leqImpl(LabelJ L1, LabelJ L2, SearchState state) {
//        AssertionUseCount auc = ((SearchState_c)state).auc;
//        Set currentGoals = ((SearchState_c)state).currentGoals;
//        
//        if (L1.isSingleton()) L1 = L1.singletonComponent();
//        if (L2.isSingleton()) L2 = L2.singletonComponent();
//
//        if (! L1.isComparable() || ! L2.isComparable()) {
//            throw new InternalCompilerError("Cannot compare " + L1 +
//                                            " with " + L2 + ".");
//        }
//        
//                
//        // do some easy tests firsts.
//        if (L1.isBottom()) return true;
//        //if (L2.isBottom()) return false;
//        
//        if (L2.isTop()) return true;
//        if (L1.isTop()) return false;
//        
//        if (L2 instanceof RuntimeLabel) return L1.isRuntimeRepresentable();
//        if (L1 instanceof RuntimeLabel) return false; // <= RT and TOP only.
//         
//        // check the current goals, to make sure we don't go into an infinite
//        // recursion...
//        LeqGoal newGoal = new LeqGoal(L1, L2);
//        if (currentGoals.contains(newGoal)) {
//            // already have this subgoal on the stack
//            return false;
//        }
//        currentGoals = new LinkedHashSet(currentGoals);
//        currentGoals.add(newGoal);
//        state = new SearchState_c(auc, currentGoals);        
//        
//        if (L1.equals(L2)) return true;        
//
//        // L1 <= L2 if there for all components of L1, there is one component
//        // of L2 that is greater.  We need to filter out all L1, and L2
//        // that are not enumerable.        
//        if (! L1.isEnumerable()) {
//            return L1.leq_(L2, this, state);        
//        }
//        if (! L1.isEnumerable() || ! L2.isEnumerable()) {
//            throw new InternalCompilerError("Cannot compare " + L1 +
//                                            " <= " + L2);
//        }
//                
//        if (L2.isSingleton()) {
//            L2 = L2.singletonComponent();
//            boolean result = L1.leq_(L2, this, state);            
//            if (!result && L1 instanceof ArgLabel) {
//                ArgLabel al = (ArgLabel)L1;
//                // recurse on upper bound.
//                result = leq(al.upperBound(), L2, state);
//            }
//            
//            // try to use assertions
//            return result || leqApplyAssertions(L1, L2, (SearchState_c)state, true);
//            // try again, being dumb?
//        }
//        else if (L1.isSingleton()) {
//            // if the components of L2 are connected by joins, 
//            // then L1 <= L2 if there exists a component cj of L2 
//            // such that L1 <= cj
//            for (Iterator j = L2.components().iterator(); j.hasNext(); ) {
//                Label cj = (Label) j.next();
//                if (leq(L1, cj, state)) {
//                    return true;
//                }
//            }
//            
//            // haven't been able to prove it yet.
//            // try testing L1 against all of L2. This is needed
//            // if, say, L1 is an arg label with upper bound L join L',
//            // and L2 = L join L'.
//            if (L1.leq_(L2, this, state)) {
//                return true;
//            }
//            
//            if (L1 instanceof ArgLabel) {
//                ArgLabel al = (ArgLabel)L1;
//                // recurse on upper bound.
//                if (leq(al.upperBound(), L2, state)) {
//                    return true;
//                }
//            }
//
//            // haven't been able to prove it yet. Try the assertions
//            return leqApplyAssertions(L1, L2, (SearchState_c)state, true);
//        }
//        else {
//            // L1 is not a singleton, and neither is L2.
//
//            // We need to break L1 down...
//            // if the components of L1 are connected by joins, 
//            // then L1 <= L2 if for all components ci of L1 
//            // we have ci <= L2
//            for (Iterator i = L1.components().iterator(); i.hasNext(); ) {
//                Label ci = (Label) i.next();
//                if (!leq(ci, L2, state)) {
//                    return false;
//                }
//            }   
//            return true;
//        }            
//    }

    /**
     * Bound the number of times any particular assertion can be used; this bounds
     * the search in leqImpl.
     */
    private static final int ASSERTION_USE_BOUND = 1;

    /**
     * Bound the number different assertions that can be used; this bounds
     * the search in leqImpl.
     */
    private static final int ASSERTION_TOTAL_BOUND = 6;
        
    private boolean leqApplyAssertions(Label L1, Label L2, SearchState_c state, boolean beSmart) {
        AssertionUseCount auc = state.auc;
        if (!state.useAssertions || auc.size() >= ASSERTION_TOTAL_BOUND) return false;
        if (Report.should_report(topics, 2))
            Report.report(2, "Applying assertions for " + L1 + " <= " + L2);

        for (Iterator i = labelAssertions.iterator(); i.hasNext();) { 
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
            if (Report.should_report(topics, 4))
                Report.report(4, "Considering assertion " + c + " for " + L1 + " <= " + L2);

            if (beSmart) {
                // only use assertions that match one or the other of our labels
                if (!L1.equals(cLHS) && !L2.equals(cRHS)) {
                    continue;
                }
            }
            if (Report.should_report(topics, 3))
                Report.report(3, "Trying assertion " + c + " for " + L1 + " <= " + L2);
            if (leq(L1, cLHS, newState) && 
                    leq(cRHS, L2, newState)) {
                return true;
            }
        }
        return false;

    }
    
    
    public boolean leq(Policy p1, Policy p2) {
        return leq(p1.simplify(), p2.simplify(), new SearchState_c(new AssertionUseCount(), new LinkedHashSet()));
    }
    public boolean leq(Policy p1, Policy p2, SearchState state) {
        // check the current goals
        AssertionUseCount auc = ((SearchState_c)state).auc;
        Set currentGoals = ((SearchState_c)state).currentGoals;
        LeqGoal newGoal = new LeqGoal(p1, p2);
        if (currentGoals.contains(newGoal)) {
            // already have this subgoal on the stack
            return false;
        }
        currentGoals = new LinkedHashSet(currentGoals);
        currentGoals.add(newGoal);
        state = new SearchState_c(auc, currentGoals);        

        
        if (p1 instanceof ConfPolicy && p2 instanceof ConfPolicy) {
            return leq((ConfPolicy)p1, (ConfPolicy)p2, state);
        }
        if (p1 instanceof IntegPolicy && p2 instanceof IntegPolicy) {
            return leq((IntegPolicy)p1, (IntegPolicy)p2, state);
        }
        return false;
    }
    
    public boolean leq(ConfPolicy p1, ConfPolicy p2, SearchState state) {
        if (p2.isSingleton() || !p1.isSingleton()) {
            if (p1.leq_(p2, this, state)) return true;
        }
        if (p2 instanceof JoinPolicy_c) {
            // we need to find one element ci of p2 such that p1 <= ci
            JoinPolicy_c jp = (JoinPolicy_c)p2;
            for (Iterator i = jp.joinComponents().iterator(); i.hasNext(); ) {
                ConfPolicy ci = (ConfPolicy) i.next();
                
                if (leq(p1, ci, state)) {
                    return true;
                }
            }
        }
        else if (p2 instanceof MeetPolicy_c) {
            // for all elements ci of p2 we require p1 <= ci             
            MeetPolicy_c mp = (MeetPolicy_c)p2;
            boolean allSat = true;
            for (Iterator i = mp.meetComponents().iterator(); i.hasNext(); ) {
                ConfPolicy ci = (ConfPolicy) i.next();                
                if (!leq(p1, ci, state)) {
                    allSat = false;
                    break;
                }
            }
            if (allSat) return true;
        }
        if (p2.isSingleton() || !p1.isSingleton()) return false;
        return p1.leq_(p2, this, state);
    }
    public boolean leq(IntegPolicy p1, IntegPolicy p2, SearchState state) {
        if (p2.isSingleton() || !p1.isSingleton()) {
            if (p1.leq_(p2, this, state)) return true;
        }
        if (p2 instanceof JoinPolicy_c) {
            // we need to find one element ci of p2 such that p1 <= ci
            JoinPolicy_c jp = (JoinPolicy_c)p2;
            for (Iterator i = jp.joinComponents().iterator(); i.hasNext(); ) {
                IntegPolicy ci = (IntegPolicy) i.next();
                
                if (leq(p1, ci, state)) {
                    return true;
                }
            }
        }
        else if (p2 instanceof MeetPolicy_c) {
            // for all elements ci of p2 we require p1 <= ci
            MeetPolicy_c mp = (MeetPolicy_c)p2;
            boolean allSat = true;
            for (Iterator i = mp.meetComponents().iterator(); i.hasNext(); ) {
                IntegPolicy ci = (IntegPolicy) i.next();                
                if (!leq(p1, ci, state)) {
                    allSat = false;
                    break;
                }
            }
            if (allSat) return true;
        }
        if (p2.isSingleton() || !p1.isSingleton()) return false;
        return p1.leq_(p2, this, state);        
    }
    /**
     * Is this enviornment empty, or does is contain some constraints?
     */
    public boolean isEmpty() {
        return labelAssertions.isEmpty() && ph.isEmpty();
    }
    
    
    /**
     * Finds a PairLabel upper bound. It does not use leq
     *
     */
    public Label findUpperBound(Label L) {
        return findUpperBound(L, Collections.EMPTY_SET);
    }
    private Label findUpperBound(Label L, Collection seen) {        
        // L is a pair label.
        if (L instanceof PairLabel) return L;
        
        if (seen.contains(L)) return ts.topLabel();
        
        Collection newSeen = new ArrayList(seen.size() + 1);
        newSeen.addAll(seen);
        newSeen.add(L);
                
        Set allBounds = new LinkedHashSet();
        if (L instanceof JoinLabel) {
            JoinLabel jl = (JoinLabel)L;
            Label ret = ts.bottomLabel();
            for (Iterator iter = jl.joinComponents().iterator(); iter.hasNext();) {
                Label comp = (Label)iter.next();
                ret = ts.join(ret, this.findUpperBound(comp, newSeen));
            }
            allBounds.add(ret);
        }
        if (L instanceof MeetLabel) {
            MeetLabel ml = (MeetLabel)L;
            Label ret = ts.topLabel();
            for (Iterator iter = ml.meetComponents().iterator(); iter.hasNext();) {
                Label comp = (Label)iter.next();
                ret = ts.meet(ret, this.findUpperBound(comp, newSeen));
            }
            allBounds.add(ret);
        }
                
        // check the assertions
        for (Iterator i = labelAssertions.iterator(); i.hasNext();) { 
            LabelLeAssertion c = (LabelLeAssertion)i.next();

            Label cLHS = c.lhs();
            if (cLHS.hasVariables()) { 
                cLHS = this.solver.applyBoundsTo(c.lhs());
            }
            Label cRHS = c.rhs();
            if (cRHS.hasVariables()) { 
                cRHS = this.solver.applyBoundsTo(c.rhs());
            }
            if (L.equals(cLHS)) {
                allBounds.add(findUpperBound(cRHS, newSeen));
            }
        }

        // assertions didn't help
        if (L instanceof ArgLabel) {
            ArgLabel al = (ArgLabel)L;
            // we want to make sure that we don't end up recursing.
            // Check that al.upperbound() is not recursively defined.            
            if (!argLabelBoundRecursive(al)) {
                allBounds.add(findUpperBound(al.upperBound(), newSeen));
            }
        }
        
        if (!allBounds.isEmpty()) {
            Label upperBound;
            if (allBounds.size() == 1) {
                upperBound = (Label)allBounds.iterator().next();
            }
            else {
                upperBound = ts.meetLabel(L.position(), allBounds);
            }
            if (Report.should_report(topics, 4))
                Report.report(4, "Using " + upperBound + " as upper bound for " + L);
            return upperBound;
        }

        if (Report.should_report(topics, 4))
            Report.report(4, "Using top as upper bound for " + L);
        return ts.topLabel();
    }
    
    private boolean argLabelBoundRecursive(ArgLabel al) {
        ArgLabelGatherer alg = new ArgLabelGatherer();
        try {
            al.upperBound().subst(alg);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpcted SemanticError");
        }
        return alg.argLabels.contains(al);        
    }
    private static class ArgLabelGatherer extends LabelSubstitution {
        private final Set argLabels = new LinkedHashSet();
        
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof ArgLabel) {
                argLabels.add(L);
            }
            return L;
        }        
    }        
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append(this.displayLabelAssertions);
//        for (Iterator i = labelAssertions.iterator(); i.hasNext(); ) {
//            LabelLeAssertion c = (LabelLeAssertion) i.next();
//            sb.append(c.lhs());
//            sb.append(" <= ");
//            sb.append(c.rhs());
//            if (i.hasNext())
//                sb.append(", ");
//        }
        if (!ph().isEmpty()) {
            if (!labelAssertions.isEmpty()) {
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
        for (Iterator iter = labelAssertions.iterator(); iter.hasNext(); ) {
            LabelLeAssertion c = (LabelLeAssertion) iter.next();
            Label bound = bounds.applyTo(c.lhs());
            Collection components;
            if (bound instanceof JoinLabel) {
                components = ((JoinLabel)bound).joinComponents();
            }
            else if (bound instanceof MeetLabel) {
                components = ((MeetLabel)bound).meetComponents();
            }
            else {
                components = Collections.singleton(bound);
            }
            
            for (Iterator i = components.iterator(); i.hasNext(); ) {
                Label l = (Label)i.next();
                labelComponents.add(l);
            }
            
            bound = bounds.applyTo(c.rhs());
            if (bound instanceof JoinLabel) {
                components = ((JoinLabel)bound).joinComponents();
            }
            else if (bound instanceof MeetLabel) {
                components = ((MeetLabel)bound).meetComponents();
            }
            else {
                components = Collections.singleton(bound);
            }
            for (Iterator i = components.iterator(); i.hasNext(); ) {
                Label l = (Label)i.next();
                labelComponents.add(l);
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
     * Trigger the transformation of WritersToReaders labels. Not guaranteed
     * to remove all writersToReaders labels. 
     * @param label
     * @param env
     * @return
     */
    public Label triggerTransforms(Label label) {
        LabelSubstitution subst = new LabelSubstitution() {
            public Label substLabel(Label L) throws SemanticException {
                if (L instanceof WritersToReadersLabel) {
                    return ((WritersToReadersLabel)L).transform(LabelEnv_c.this);
                }
                return L;
            }            
        };
        
        try {
            return label.subst(subst);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }
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
