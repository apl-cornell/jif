package jif.types.hierarchy;

import java.util.*;
import java.util.Map.Entry;

import jif.Topics;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
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

    /**
     * A map from AccessPath to representatives of the
     * equivalent set of the AcessPath. No mapping if the
     * element is its own representative.
     */
    protected final Map accessPathEquivReps;

    protected final LabelEnv_c parent; // a more general (i.e., fewer assertions) LabelEnv, used only for cache lookup.
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
        this(ts, new PrincipalHierarchy(), new LinkedList(), "", false, useCache, new LinkedHashMap(), null);
    }
    protected LabelEnv_c(JifTypeSystem ts, PrincipalHierarchy ph, List assertions, String displayLabelAssertions, boolean hasVariables, boolean useCache, Map accessPathEquivReps, LabelEnv_c parent) {
        this.ph = ph;
        this.labelAssertions = assertions;
        this.accessPathEquivReps = accessPathEquivReps;
        this.displayLabelAssertions = new StringBuffer(displayLabelAssertions);
        this.hasVariables = false;
        this.solver = null;        
        this.hasVariables = hasVariables;
        this.ts = ts;
        this.useCache = useCache;
        this.parent = parent;
        this.cacheTrue = new HashSet();
        this.cacheFalse = new HashSet();        
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
        // clear the cache of false leq results, since this may let us prove more results
        cacheFalse.clear();
        ph.add(p1, p2);
    }

    public void addEquiv(Principal p1, Principal p2) {
        // clear the cache of false leq results, since this may let us prove more results
        cacheFalse.clear();
        ph.add(p1, p2);
        ph.add(p2, p1);
    }
    
    public void addAssertionLE(Label L1, Label L2) {
        addAssertionLE(L1, L2, true);
    }
    private boolean addAssertionLE(Label L1, Label L2, boolean updateDisplayString) {
        // clear the cache of false leq results, since this may let us prove more results
        cacheFalse.clear();
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
                    !(this.leq(L1, L2, freshSearchState()))) {
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
    
    public LabelEnv_c copy() {
        return new LabelEnv_c(ts, ph.copy(), new LinkedList(labelAssertions), 
                              displayLabelAssertions.toString(), 
                              hasVariables, useCache, 
                              new LinkedHashMap(this.accessPathEquivReps), this);
    }
    
    public boolean actsFor(Principal p, Principal q) {
        if (p instanceof DynamicPrincipal && q instanceof DynamicPrincipal) {
            DynamicPrincipal dp = (DynamicPrincipal)p;
            DynamicPrincipal dq = (DynamicPrincipal)q;
            if (equivalentAccessPaths(dp.path(), dq.path())) return true;
        }
        return ph.actsFor(p, q);
    }
    
    public boolean leq(Label L1, Label L2) { 
        if (Report.should_report(topics, 1))
            Report.report(1, "Testing " + L1 + " <= " + L2);
        
        return leq(L1, L2, 
                   new SearchState_c(new AssertionUseCount()));
    }
    
    public boolean equivalentAccessPaths(AccessPath p, AccessPath q) {
        if (p == q) return true;
        if (findAccessPathRepr(p).equals(findAccessPathRepr(q))) {
            return true;
        }
        return p.equivalentTo(q, this);
    }

    /**
     * Returns the representative of the equivalence class of p. 
     * Provided p is not-null, returns a not-null value.
     */
    private AccessPath findAccessPathRepr(AccessPath p) {
        AccessPath last = p;
        AccessPath next = (AccessPath)accessPathEquivReps.get(last);
        while (next != null) {
            last = next;
            next = (AccessPath)accessPathEquivReps.get(last);
        }
        return last;
    }
    public void addEquiv(AccessPath p, AccessPath q) {
        // clear the cache of false leq results, since this may let us prove more results
        cacheFalse.clear();
        accessPathEquivReps.put(p, findAccessPathRepr(q));
    }
    /*
     * Cache the results of leq(Label, Label, SearchState), when we are
     * using assertions only. Note that the rest of the
     * search state contains only information for pruning the search,
     * and so we can ignore it and consider only L1 and L2 when caching 
     * the results. 
     */
    private final Set cacheTrue;
    private final Set cacheFalse;
    
    protected final boolean useCache;
    
    private static class LeqGoal {
        final int hash;
        final Object lhs;
        final Object rhs;
        LeqGoal(Policy lhs, Policy rhs) { 
            this.lhs = lhs; this.rhs = rhs;
            if (lhs == null || rhs == null) 
                throw new InternalCompilerError("Null policy!");

            int lhash = lhs.hashCode(); 
            int rhash = rhs.hashCode();
            if (lhash == rhash)
                this.hash = lhash;
            else
                this.hash = lhash ^ rhash;
        }
        LeqGoal(Label lhs, Label rhs) { 
            this.lhs = lhs; this.rhs = rhs;
            if (lhs == null || rhs == null) 
                throw new InternalCompilerError("Null label!");
            int lhash = lhs.hashCode(); 
            int rhash = rhs.hashCode();
            if (lhash == rhash)
                this.hash = lhash;
            else
                this.hash = lhash ^ rhash;
        }
        public int hashCode() {
            return hash;
        }
        public boolean equals(Object o) {
            if (o instanceof LeqGoal) {
                LeqGoal that = (LeqGoal)o;
                return this.hash == that.hash &&
                     this.lhs.equals(that.lhs) && this.rhs.equals(that.rhs);
                
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
            return leqImpl(L1, L2, (SearchState_c)state);
        }

        // only use the cache if we are using assertions, and there are no
        // variables
        LeqGoal g = new LeqGoal(L1, L2);
    
        Boolean b = checkCache(g);
        if (b != null) {
            if (Report.should_report(topics, 3))
                Report.report(3, "Found cache value for " + L1 + " <= " + L2 
                              + " : " + b.booleanValue());
            return b.booleanValue();
        }        
        boolean result = leqImpl(L1, L2, (SearchState_c)state);
        cacheResult(g, state, result);
        return result;
    }
    
    protected Boolean checkCache(LeqGoal g) {
        if (!useCache || this.hasVariables()) {
            return null;
        }
        if (cacheTrue.contains(g)) return Boolean.TRUE;
        if (cacheFalse.contains(g)) return Boolean.FALSE;
        
        // try looking in the trueCache of more general label envs
        LabelEnv_c ancestor = this.parent;
        while (ancestor != null) {
            if (!ancestor.useCache || ancestor.hasVariables()) {
                break;
            }
            
            if (ancestor.cacheTrue.contains(g)) {
                this.cacheTrue.add(g);
                return Boolean.TRUE;
            }
            
            ancestor = ancestor.parent;
        }
        return null;
    }
    
    protected void cacheResult(LeqGoal g, SearchState s, boolean result) {
        if (!useCache || this.hasVariables() || !((SearchState_c)s).auc.allZero()) {
            return;
        }
        
        // add the result to the correct cache.
        (result?cacheTrue:cacheFalse).add(g); 
    }
        
    /**
     * Non-caching implementation of L1 <= L2
     */
    private boolean leqImpl(Label L1, Label L2, SearchState_c state) {
        AssertionUseCount auc = state.auc;
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
                 
        // check the current goals, to make sure we don't go into an infinite
        // recursion...
        LeqGoal newGoal = new LeqGoal(L1, L2);
        if (state.containsGoal(newGoal)) {
            // already have this subgoal on the stack
            if (Report.should_report(topics, 3))
                Report.report(3, "Goal " + L1 + " <= " + L2 + " already on goal stack");
            return false;
        }
        state = new SearchState_c(auc, state, newGoal);        
        
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
            SearchState newState = new SearchState_c(newAUC, state, null);

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
        return leq(p1.simplify(), p2.simplify(), new SearchState_c(new AssertionUseCount()));
    }
    public boolean leq(Policy p1, Policy p2, SearchState state_) {
        // check the current goals
        SearchState_c state = (SearchState_c)state_;
        AssertionUseCount auc = state.auc;
        LeqGoal newGoal = new LeqGoal(p1, p2);
        if (state.containsGoal(newGoal)) {
            // already have this subgoal on the stack
            return false;
        }
        state = new SearchState_c(auc, state, newGoal);        

        
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
        if (L instanceof VarLabel_c) {
            // cant do anything.
            return L;
        }        
        
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
        if (Report.should_report(Report.debug, 1) && !accessPathEquivReps.isEmpty()) {
            for (Iterator iter = accessPathEquivReps.entrySet().iterator(); iter.hasNext(); ) {
                if (sb.length() > 1) sb.append(", ");

                Map.Entry e = (Entry)iter.next();
                sb.append(((AccessPath)e.getKey()).exprString());
                sb.append("==");
                sb.append(((AccessPath)e.getValue()).exprString());
            }
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
            return label.subst(subst).simplify();
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
        
        public boolean allZero() {
            return tally.isEmpty();
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
    protected SearchState freshSearchState() {
        return new SearchState_c(null, null, null);
    }
    private static class SearchState_c implements SearchState {
        public final AssertionUseCount auc;
        public final LeqGoal currentGoal;
        public final SearchState_c prevState;
        public final boolean useAssertions;
        SearchState_c(AssertionUseCount auc, SearchState_c prevState, LeqGoal currentGoal) {
            this.useAssertions = (auc != null);
            this.auc = auc;
            this.prevState = prevState;
            this.currentGoal = currentGoal;
        }
        public SearchState_c(AssertionUseCount auc) {
            this(auc, null, null);
        }
        public boolean containsGoal(LeqGoal g) {
            if (currentGoal != null && currentGoal.equals(g)) {
                return true;
            }
            if (prevState != null) {
                return prevState.containsGoal(g);
            }
            return false;
        }
    }
}
