package jif.lang;

import java.util.*;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.IntegPolicy Policy}.
 *  
 */
public class LabelUtil
{
    protected LabelUtil() { }
    protected static LabelUtil singleton;
    static {
        singleton = new LabelUtil();
    }
    public static LabelUtil singleton() {
        return singleton;
    }
    
    /*
     * fields and class for collecting timing statistics.
     * timing statistics are collected on a per-thread basis.
     */
    private static class Stats {
        private long totalTime = 0;
        private long enterStartTime = 0;
        private int callStackCount = 0;
        private int callCount = 0;
    }
    private ThreadLocal statsPerThread = new ThreadLocal() {
        protected Object initialValue() {
            return new Stats();
        }   
    };
    private static final boolean COUNT_TIME = false;
    
    static final boolean USE_CACHING = true;

    // caches
    private Set/*<Pair>*/ cacheTrueLabelRelabels = new HashSet();
    private Set/*<Pair>*/ cacheFalseLabelRelabels = new HashSet();
    private Map/*<DelegationPair to Set<Pair>>*/ cacheTrueLabelRelabelsDependencies= new HashMap();
    private Map/*<Pair> to Set<DelegationPair>*/ cacheTruePolicyRelabels = new HashMap();
    private Set/*<Pair>*/ cacheFalsePolicyRelabels = new HashSet();
    private Map/*<DelegationPair to Set<Pair>>*/ cacheTruePolicyRelabelsDependencies = new HashMap();
    private Map cacheLabelJoins = new HashMap();
    private Map cacheLabelMeets = new HashMap();
    private Map/*<DelegationPair to Set<Pair>>*/ cacheLabelJoinDependencies = new HashMap();
    private Map/*<DelegationPair to Set<Pair>>*/ cacheLabelMeetDependencies = new HashMap();
    
    /*
     * Record that we are entering a section of code that we want to
     * record the timing of.
     */
    void enterTiming() {
        if (COUNT_TIME) {
            Stats stats = (Stats)statsPerThread.get();
            stats.callCount++;
            if (stats.callStackCount++ == 0) {
                stats.enterStartTime = System.currentTimeMillis();          
            }
        }
    }

    /*
     * Record that we are exiting a section of code that we want to
     * record the timing of.
     */
    void exitTiming() {
        if (COUNT_TIME) {
            Stats stats = (Stats)statsPerThread.get();
            if ((--stats.callStackCount) == 0) {
                stats.totalTime += (System.currentTimeMillis() - stats.enterStartTime);
            }
        }
    }
    /*
     * Return the total time spent by the current thread in code
     * we recorded the timing of, since the last time this method was called,
     * and/or the thread created (whichever was last). Also clears the total
     * time recorded for this thread.
     */
    public long getAndClearTime() {
        long r = -1;
        if (COUNT_TIME) {
            Stats stats = (Stats)statsPerThread.get();
            r = stats.totalTime;
            stats.totalTime = 0;
        }
        return r;        
    }
    
    /*
     * Return the total count of calls to enterTiming() 
     * since the last time this method was called,
     * and/or the thread created (whichever was last). Also clears the total
     * time recorded for this thread.
     */
    public long getAndClearCount() {
        long r = -1;
        if (COUNT_TIME) {
            Stats stats = (Stats)statsPerThread.get();
            r = stats.callCount;
            stats.callCount = 0;
        }
        return r;        
    }
    


    private final ConfPolicy BOTTOM_CONF;
    private final IntegPolicy TOP_INTEG;
    private final Label NO_COMPONENTS; 
    
    {
        BOTTOM_CONF = new ReaderPolicy(this, null, null);
        TOP_INTEG = new WriterPolicy(this, null, null);
        NO_COMPONENTS = new PairLabel(this, BOTTOM_CONF, TOP_INTEG);
    }
    
    public Label noComponents() {
        return NO_COMPONENTS;
    }
    
    public ConfPolicy bottomConf() {
        return BOTTOM_CONF;
    }
    public IntegPolicy topInteg() {
        return TOP_INTEG;
    }

    public ConfPolicy readerPolicy(Principal owner, Principal reader) {
        try {
            enterTiming();
            return new ReaderPolicy(this, owner, reader);
        }
        finally {
            exitTiming();
        }
    }
    public ConfPolicy readerPolicy(Principal owner, Collection readers) {
        try {
            enterTiming();
            return new ReaderPolicy(this, owner, PrincipalUtil.disjunction(readers));
        }
        finally {
            exitTiming();
        }
    }
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public ConfPolicy readerPolicy(Label lbl, Principal owner, Principal[] readers) {
        try {
            enterTiming();
            if (readers == null) return readerPolicy(owner, Collections.EMPTY_SET);
            return readerPolicy(owner, Arrays.asList(readers));
        }
        finally {
            exitTiming();
        }
    }
    
    public ConfPolicy readerPolicy(Principal owner, PrincipalSet writers) {
        try {
            enterTiming();
            return readerPolicy(owner, writers.getSet());
        }
        finally {
            exitTiming();
        }
    }
    
    public Label readerPolicyLabel(Principal owner, Principal reader) {
        try {
            enterTiming();
            return toLabel(new ReaderPolicy(this, owner, reader));
        }
        finally {
            exitTiming();
        }
    }
    public Label readerPolicyLabel(Principal owner, Collection readers) {        
        try {
            enterTiming();
            Label l = toLabel(new ReaderPolicy(this, owner, PrincipalUtil.disjunction(readers)));
            return l;
        }
        finally {
            exitTiming();
        }
    }
    
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public Label readerPolicyLabel(Label lbl, Principal owner, Principal[] readers) {
        try {
            enterTiming();
            if (readers == null) return readerPolicyLabel(owner, Collections.EMPTY_SET);
            return readerPolicyLabel(owner, Arrays.asList(readers));
        }
        finally {
            exitTiming();
        }
    }
    
    public Label readerPolicyLabel(Principal owner, PrincipalSet readers) {
        try {
            enterTiming();
            return readerPolicyLabel(owner, PrincipalUtil.disjunction(readers.getSet()));
        }
        finally {
            exitTiming();
        }
    }
    
    public IntegPolicy writerPolicy(Principal owner, Principal writer) {
        try {
            enterTiming();
            return new WriterPolicy(this, owner, writer);
        }
        finally {
            exitTiming();
        }
    }
    public IntegPolicy writerPolicy(Principal owner, Collection writers) {
        try {
            enterTiming();
            return new WriterPolicy(this, owner, PrincipalUtil.disjunction(writers));
        }
        finally {
            exitTiming();
        }
    }
    public Label writerPolicyLabel(Principal owner, Principal writer) {
        try {
            enterTiming();
            return toLabel(new WriterPolicy(this, owner, writer));
        }
        finally {
            exitTiming();
        }
    }
    public Label writerPolicyLabel(Principal owner, Collection writers) {
        try {
            enterTiming();
            return toLabel(new WriterPolicy(this, owner, PrincipalUtil.disjunction(writers)));
        }
        finally {
            exitTiming();
        }
    }
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public Label writerPolicyLabel(Label lbl, Principal owner, Principal[] writers) {
        try {
            enterTiming();
            if (writers == null) return writerPolicyLabel(owner, Collections.EMPTY_SET);
            return writerPolicyLabel(owner, Arrays.asList(writers));
        }
        finally {
            exitTiming();
        }
    }
    
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public IntegPolicy writerPolicy(Label lbl, Principal owner, Principal[] writers) {
        try {
            enterTiming();
            if (writers == null) return writerPolicy(owner, Collections.EMPTY_SET);
            return writerPolicy(owner, Arrays.asList(writers));
        }
        finally {
            exitTiming();
        }
    }
    
    public IntegPolicy writerPolicy(Principal owner, PrincipalSet writers) {
        try {
            enterTiming();
            return writerPolicy(owner, writers.getSet());
        }
        finally {
            exitTiming();
        }
    }
    
    public Label toLabel(ConfPolicy cPolicy, IntegPolicy iPolicy) {
        try {
            enterTiming();
            return new PairLabel(this, cPolicy, iPolicy);        
        }
        finally {
            exitTiming();
        }
    }
    public Label toLabel(ConfPolicy policy) {
        try {
            enterTiming();
            return new PairLabel(this, policy, TOP_INTEG);
        }
        finally {
            exitTiming();
        }
    }
    public Label toLabel(IntegPolicy policy) {
        try {
            enterTiming();
            return new PairLabel(this, BOTTOM_CONF, policy);
        }
        finally {
            exitTiming();
        }
    }
    
    
    public Label join(Label l1, Label l2) {
        try {
            enterTiming();
            if (l1 == null) return l2;
            if (l2 == null) return l1;
            
            if (l1 instanceof PairLabel && l2 instanceof PairLabel) {
                Label result = null;
                Pair pair = new Pair(l1, l2);            
                if (USE_CACHING) {
                    result = (Label)cacheLabelJoins.get(pair);
                }
                if (result == null) {
                    PairLabel pl1 = (PairLabel)l1;
                    PairLabel pl2 = (PairLabel)l2;
                    Set dependencies = new HashSet();
                    result = new PairLabel(this, pl1.confPolicy().join(pl2.confPolicy(), dependencies),
                                           pl1.integPolicy().join(pl2.integPolicy(), dependencies));
                    if (USE_CACHING) {
                        // add dependencies from delegations to the cache result
                        // i.e., what dependencies does this result rely on?
                        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                            DelegationPair del = (DelegationPair)iter.next();
                            Set deps = (Set)cacheLabelJoinDependencies.get(del);
                            if (deps == null) {
                                deps = new HashSet();
                                cacheLabelJoinDependencies.put(del, deps);
                            }
                            deps.add(pair);
                        }
                        cacheLabelJoins.put(pair, result);
                    }
                }
                return result;            
                
            }
            // error! non pair labels!
            return null;
        }
        finally {
            exitTiming();
        }
    }
    public Label meetLbl(Label l1, Label l2) {
        try {
            enterTiming();
            return meet(l1, l2);
        }
        finally {
            exitTiming();
        }
    }
    public Label meet(Label l1, Label l2) {
        try {
            enterTiming();
            if (l1 == null) return l2;
            if (l2 == null) return l1;
            
            if (l1 instanceof PairLabel && l2 instanceof PairLabel) {
                Label result = null;
                Pair pair = new Pair(l1, l2);
                if (USE_CACHING) {
                    result = (Label)cacheLabelMeets.get(pair);
                }
                if (result == null) {
                    PairLabel pl1 = (PairLabel)l1;
                    PairLabel pl2 = (PairLabel)l2;
                    Set dependencies = new HashSet();
                    result = new PairLabel(this, pl1.confPolicy().meet(pl2.confPolicy(), dependencies),
                                           pl1.integPolicy().meet(pl2.integPolicy(), dependencies));
                    if (USE_CACHING) {
                        // add dependencies from delegations to the cache result
                        // i.e., what dependencies does this result rely on?
                        for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                            DelegationPair del = (DelegationPair)iter.next();
                            Set deps = (Set)cacheLabelMeetDependencies.get(del);
                            if (deps == null) {
                                deps = new HashSet();
                                cacheLabelMeetDependencies.put(del, deps);
                            }
                            deps.add(pair);
                        }
                        cacheLabelMeets.put(pair, result);
                    }
                }
                return result;                            
            }

            // error! non pair labels!
            return null;
        }
        finally {
            exitTiming();
        }
    }
    public ConfPolicy join(ConfPolicy p1, ConfPolicy p2) {        
        try {
            enterTiming();
            return join(p1, p2, new HashSet());
        }
        finally {
            exitTiming();
        }
    }
    protected ConfPolicy join(ConfPolicy p1, ConfPolicy p2, Set s) {        
        try {
            enterTiming();
            Set comps = new LinkedHashSet();
            if (p1 instanceof JoinConfPolicy) {
                comps.addAll(((JoinConfPolicy)p1).joinComponents());
            }
            else {
                comps.add(p1);
            }
            if (p2 instanceof JoinConfPolicy) {
                comps.addAll(((JoinConfPolicy)p2).joinComponents());
            }
            else {
                comps.add(p2);
            }
            comps = simplifyJoin(comps, s);
            
            if (comps.size() == 1) {
                return (ConfPolicy)comps.iterator().next();
            }
            return new JoinConfPolicy(this, comps);
        }
        finally {
            exitTiming();
        }
        
    }
    public IntegPolicy join(IntegPolicy p1, IntegPolicy p2) {
        try {
            enterTiming();
            return join(p1, p2, new HashSet());
        }
        finally {
            exitTiming();
        }        
    }
    IntegPolicy join(IntegPolicy p1, IntegPolicy p2, Set s) {        
        try {
            enterTiming();
            Set comps = new LinkedHashSet();
            if (p1 instanceof JoinIntegPolicy) {
                comps.addAll(((JoinIntegPolicy)p1).joinComponents());
            }
            else {
                comps.add(p1);
            }
            if (p2 instanceof JoinIntegPolicy) {
                comps.addAll(((JoinIntegPolicy)p2).joinComponents());
            }
            else {
                comps.add(p2);
            }
            comps = simplifyJoin(comps, s);
            
            if (comps.size() == 1) {
                return (IntegPolicy)comps.iterator().next();
            }
            return new JoinIntegPolicy(this, comps);
        }
        finally {
            exitTiming();
        }
        
    }
    public ConfPolicy meetPol(ConfPolicy p1, ConfPolicy p2) {
        try {
            enterTiming();
            return meet(p1, p2, new HashSet());
        }
        finally {
            exitTiming();
        }
    }
    protected ConfPolicy meet(ConfPolicy p1, ConfPolicy p2, Set s) {        
        try {
            enterTiming();
            Set comps = new LinkedHashSet();
            if (p1 instanceof MeetConfPolicy) {
                comps.addAll(((MeetConfPolicy)p1).meetComponents());
            }
            else {
                comps.add(p1);
            }
            if (p2 instanceof MeetConfPolicy) {
                comps.addAll(((MeetConfPolicy)p2).meetComponents());
            }
            else {
                comps.add(p2);
            }
            comps = simplifyMeet(comps, s);
            
            if (comps.size() == 1) {
                return (ConfPolicy)comps.iterator().next();
            }
            return new MeetConfPolicy(this, comps);
        }
        finally {
            exitTiming();
        }
    }
    public IntegPolicy meetPol(IntegPolicy p1, IntegPolicy p2) {
        try {
            enterTiming();
            return meet(p1, p2, new HashSet());
        }
        finally {
            exitTiming();
        }
    }
     IntegPolicy meet(IntegPolicy p1, IntegPolicy p2, Set s) {        
        try {
            enterTiming();
            Set comps = new LinkedHashSet();
            if (p1 instanceof MeetIntegPolicy) {
                comps.addAll(((MeetIntegPolicy)p1).meetComponents());
            }
            else {
                comps.add(p1);
            }
            if (p2 instanceof MeetIntegPolicy) {
                comps.addAll(((MeetIntegPolicy)p2).meetComponents());
            }
            else {
                comps.add(p2);
            }
            comps = simplifyMeet(comps, s);
            
            if (comps.size() == 1) {
                return (IntegPolicy)comps.iterator().next();
            }
            return new MeetIntegPolicy(this, comps);
        }
        finally {
            exitTiming();
        }
        
    }
    
    
    
    public boolean equivalentTo(Label l1, Label l2) {
        try {
            enterTiming();
            if (l1 == l2 || (l1 != null && l1.equals(l2))) return true;
            return relabelsTo(l1, l2) && relabelsTo(l2, l1);
        }
        finally {
            exitTiming();
        }
    }
    
    public boolean isReadableBy(Label lbl, Principal p) {
        try {
            enterTiming();
            Label L = toLabel(PrincipalUtil.readableByPrinPolicy(p));
            return relabelsTo(lbl, L);
        }
        finally {
            exitTiming();
        }
    }
    
    public boolean relabelsTo(Label from, Label to) {
        try {
            enterTiming();
            if (from == null || to == null) return false;
            if (from == to || from.equals(to)) return true;
            Pair pair = new Pair(from, to);
            if (USE_CACHING) {
                if (cacheTrueLabelRelabels.contains(pair)) return true;
                if (cacheFalseLabelRelabels.contains(pair)) return false;
            }
            Set dependencies = new HashSet();
            boolean result = from != null && from.relabelsTo(to, dependencies);
            if (USE_CACHING) {
                if (!result) {
                    cacheFalseLabelRelabels.add(pair);
                }
                else {
                    cacheTrueLabelRelabels.add(pair);
                    // add dependencies from delegations to the cache result
                    // i.e., what dependencies does this result rely on?
                    for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                        DelegationPair del = (DelegationPair)iter.next();
                        Set deps = (Set)cacheTrueLabelRelabelsDependencies.get(del);
                        if (deps == null) {
                            deps = new HashSet();
                            cacheTrueLabelRelabelsDependencies.put(del, deps);
                        }
                        deps.add(pair);
                    }
                }
            }
            return result;            
        }
        finally {
            exitTiming();
        }
    }

    public boolean relabelsTo(Policy from, Policy to) {
        try {
            enterTiming();
            return relabelsTo(from, to, new HashSet());
        }
        finally {
            exitTiming();
        }
    }

    public boolean relabelsTo(Policy from, Policy to, Set s) {
        try {
            enterTiming();
            if (from == null || to == null) return false;
            if (from == to || from.equals(to)) return true;
            Pair pair = new Pair(from, to);
            if (USE_CACHING) {
                if (cacheTruePolicyRelabels.containsKey(pair)) {
                    s.addAll((Set)cacheTruePolicyRelabels.get(pair));
                    return true;
                }
                if (cacheFalsePolicyRelabels.contains(pair)) return false;
            }
            Set dependencies = new HashSet();
            boolean result = from.relabelsTo(to, dependencies);
            if (USE_CACHING) {
                if (!result) {
                    cacheFalsePolicyRelabels.add(pair);
                }
                else {
                    cacheTruePolicyRelabels.put(pair, dependencies);
                    // add dependencies from delegations to the cache result
                    // i.e., what dependencies does this result rely on?
                    for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                        DelegationPair del = (DelegationPair)iter.next();
                        Set deps = (Set)cacheTruePolicyRelabelsDependencies.get(del);
                        if (deps == null) {
                            deps = new HashSet();
                            cacheTruePolicyRelabelsDependencies.put(del, deps);
                        }
                        deps.add(pair);
                    }
                    s.addAll(dependencies);
                }
            }
            return result;            
        }
        finally {
            exitTiming();
        }
    }
    
    public String stringValue(Label lb) {
        try {
            enterTiming();
            if (lb == null) return "<null>";
            return lb.toString();
        }
        finally {
            exitTiming();
        }
    }
    
    public String toString(Label lb) {
        try {
            enterTiming();
            return stringValue(lb);
        }
        finally {
            exitTiming();
        }
    }
    
    public int hashCode(Label lb) {
        try {
            enterTiming();
            if (lb == null) return 0;
            return lb.hashCode();
        }
        finally {
            exitTiming();
        }
    }
        
    private Set simplifyJoin(Set policies, Set dependencies) {
        Set needed = new LinkedHashSet();
        for (Iterator i = policies.iterator(); i.hasNext(); ) {
            Policy ci = (Policy)i.next();
            
            boolean subsumed = (ci == null); // null components are always subsumed.
            for (Iterator j = needed.iterator(); !subsumed && j.hasNext(); ) {
                Policy cj = (Policy) j.next();
                if (relabelsTo(ci, cj, dependencies)) {
                    subsumed = true;
                    break;
                }
                
                if (relabelsTo(cj, ci, dependencies)) { 
                    j.remove();
                }
            }
            
            if (!subsumed) needed.add(ci);
        }
        
        return needed;        
    }
    private Set simplifyMeet(Set policies, Set dependencies) {
        Set needed = new LinkedHashSet();
        for (Iterator i = policies.iterator(); i.hasNext(); ) {
            Policy ci = (Policy)i.next();
            
            boolean subsumed = (ci == null); // null components are always subsumed.
            for (Iterator j = needed.iterator(); !subsumed && j.hasNext(); ) {
                Policy cj = (Policy) j.next();
                if (relabelsTo(cj, ci, dependencies)) {
                    subsumed = true;
                    break;
                }
                
                if (relabelsTo(ci, cj, dependencies)) { 
                    j.remove();
                }
            }
            
            if (!subsumed) needed.add(ci);
        }
        
        return needed;        
    }

    /**
     * Internal representation of a pair of objects, used for the caches
     */
    private class Pair {
        final Object left; // must be non null
        final Object right; // must be non null
        public Pair(Object left, Object right) {
            this.left = left;
            this.right = right;
        }
        public int hashCode() {
            return left.hashCode() ^ right.hashCode();
        }
        public boolean equals(Object o) {
            if (o instanceof Pair) {
                Pair that = (Pair)o;
                return (this.left == that.left || this.left.equals(that.left)) && 
                       (this.right == that.right || this.right.equals(that.right));
            }
            return false;
        }
        public String toString() {
            return left + "-" + right;
        }
    }

    void notifyNewDelegation(Principal granter, Principal superior) {
        try {
            enterTiming();
            if (USE_CACHING) {
                // XXX for the moment, just clear out the caches.
                cacheFalseLabelRelabels.clear();
                cacheFalsePolicyRelabels.clear();

                // the label meets and joins can be soundly left, they just
                // may not be as simplified as they could be. However, to maintain
                // compatability with previous behavior, we will clear the caches
                cacheLabelJoins.clear();
                cacheLabelMeets.clear();
                cacheLabelJoinDependencies.clear();
                cacheLabelMeetDependencies.clear();
            }
        }
        finally {
            exitTiming();
        }
    }
    void notifyRevokeDelegation(Principal granter, Principal superior) {
        try {
            enterTiming();
            if (USE_CACHING) {
                DelegationPair del = new DelegationPair(superior, granter);
                Set deps = (Set)cacheTrueLabelRelabelsDependencies.remove(del);
                if (deps != null) {
                    for (Iterator iter = deps.iterator(); iter.hasNext();) {
                        Pair afp = (Pair)iter.next();
                        cacheTrueLabelRelabels.remove(afp);
                    }
                }
                deps = (Set)cacheTruePolicyRelabelsDependencies.remove(del);
                if (deps != null) {
                    for (Iterator iter = deps.iterator(); iter.hasNext();) {
                        Pair afp = (Pair)iter.next();
                        cacheTruePolicyRelabels.remove(afp);
                    }
                }
                deps = (Set)cacheLabelJoinDependencies.remove(del);
                if (deps != null) {
                    for (Iterator iter = deps.iterator(); iter.hasNext();) {
                        Pair afp = (Pair)iter.next();
                        cacheLabelJoins.remove(afp);
                    }
                }
                deps = (Set)cacheLabelMeetDependencies.remove(del);
                if (deps != null) {
                    for (Iterator iter = deps.iterator(); iter.hasNext();) {
                        Pair afp = (Pair)iter.next();
                        cacheLabelMeets.remove(afp);
                    }
                }
            }
        }
        finally {
            exitTiming();
        }
    }
}
