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
    private static long totalTime = 0;
    private static long enterStartTime = 0;
    private static int callStackCount = 0;
    private static boolean COUNT_TIME = false;
    
    // caches
    private static Set/*<Pair>*/ cacheTrueLabelRelabels = new HashSet();
    private static Set/*<Pair>*/ cacheFalseLabelRelabels = new HashSet();
    private static Map/*<DelegationPair to Set<Pair>>*/ cacheTrueLabelRelabelsDependencies= new HashMap();
    private static Map/*<Pair> to Set<DelegationPair>*/ cacheTruePolicyRelabels = new HashMap();
    private static Set/*<Pair>*/ cacheFalsePolicyRelabels = new HashSet();
    private static Map/*<DelegationPair to Set<Pair>>*/ cacheTruePolicyRelabelsDependencies = new HashMap();
    private static Map cacheLabelJoins = new HashMap();
    private static Map cacheLabelMeets = new HashMap();
    private static Map/*<DelegationPair to Set<Pair>>*/ cacheLabelJoinDependencies = new HashMap();
    private static Map/*<DelegationPair to Set<Pair>>*/ cacheLabelMeetDependencies = new HashMap();
    
    static void enterTiming() {
        if (COUNT_TIME) {
            synchronized (LabelUtil.class) {
                if (callStackCount++ == 0) {
                    enterStartTime = System.currentTimeMillis();
                }
            }
        }
    }
    static void exitTiming() {
        if (COUNT_TIME) {
            synchronized (LabelUtil.class) {
                if ((--callStackCount) == 0) {
                    totalTime += (System.currentTimeMillis() - enterStartTime);
                }
            }
        }
    }
    public static long getAndClearTime() {
        long r = -1;
        if (COUNT_TIME) {
            synchronized (LabelUtil.class) {
                r = totalTime;
                totalTime = 0;
            }
        }
        return r;        
    }
    
    private static final ConfPolicy BOTTOM_CONF;
    private static final IntegPolicy TOP_INTEG;
    private static final Label NO_COMPONENTS; 
    
    static {
        BOTTOM_CONF = new ReaderPolicy(null, null);
        TOP_INTEG = new WriterPolicy(null, null);
        NO_COMPONENTS = new PairLabel(BOTTOM_CONF, TOP_INTEG);
    }
    
    protected LabelUtil() { }
    
    public static Label noComponents() {
        return NO_COMPONENTS;
    }
    
    public static ConfPolicy readerPolicy(Principal owner, Principal reader) {
        try {
            enterTiming();
            return new ReaderPolicy(owner, reader);
        }
        finally {
            exitTiming();
        }
    }
    public static ConfPolicy readerPolicy(Principal owner, Collection readers) {
        try {
            enterTiming();
            return new ReaderPolicy(owner, PrincipalUtil.disjunction(readers));
        }
        finally {
            exitTiming();
        }
    }
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static ConfPolicy readerPolicy(Label lbl, Principal owner, Principal[] readers) {
        try {
            enterTiming();
            if (readers == null) return readerPolicy(owner, Collections.EMPTY_SET);
            return readerPolicy(owner, Arrays.asList(readers));
        }
        finally {
            exitTiming();
        }
    }
    
    public static ConfPolicy readerPolicy(Principal owner, PrincipalSet writers) {
        try {
            enterTiming();
            return readerPolicy(owner, writers.getSet());
        }
        finally {
            exitTiming();
        }
    }
    
    public static Label readerPolicyLabel(Principal owner, Principal reader) {
        try {
            enterTiming();
            return toLabel(new ReaderPolicy(owner, reader));
        }
        finally {
            exitTiming();
        }
    }
    public static Label readerPolicyLabel(Principal owner, Collection readers) {        
        try {
            enterTiming();
            Label l = toLabel(new ReaderPolicy(owner, PrincipalUtil.disjunction(readers)));
            return l;
        }
        finally {
            exitTiming();
        }
    }
    
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static Label readerPolicyLabel(Label lbl, Principal owner, Principal[] readers) {
        try {
            enterTiming();
            if (readers == null) return readerPolicyLabel(owner, Collections.EMPTY_SET);
            return readerPolicyLabel(owner, Arrays.asList(readers));
        }
        finally {
            exitTiming();
        }
    }
    
    public static Label readerPolicyLabel(Principal owner, PrincipalSet readers) {
        try {
            enterTiming();
            return readerPolicyLabel(owner, PrincipalUtil.disjunction(readers.getSet()));
        }
        finally {
            exitTiming();
        }
    }
    
    public static IntegPolicy writerPolicy(Principal owner, Principal writer) {
        try {
            enterTiming();
            return new WriterPolicy(owner, writer);
        }
        finally {
            exitTiming();
        }
    }
    public static IntegPolicy writerPolicy(Principal owner, Collection writers) {
        try {
            enterTiming();
            return new WriterPolicy(owner, PrincipalUtil.disjunction(writers));
        }
        finally {
            exitTiming();
        }
    }
    public static Label writerPolicyLabel(Principal owner, Principal writer) {
        try {
            enterTiming();
            return toLabel(new WriterPolicy(owner, writer));
        }
        finally {
            exitTiming();
        }
    }
    public static Label writerPolicyLabel(Principal owner, Collection writers) {
        try {
            enterTiming();
            return toLabel(new WriterPolicy(owner, PrincipalUtil.disjunction(writers)));
        }
        finally {
            exitTiming();
        }
    }
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static Label writerPolicyLabel(Label lbl, Principal owner, Principal[] writers) {
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
    public static IntegPolicy writerPolicy(Label lbl, Principal owner, Principal[] writers) {
        try {
            enterTiming();
            if (writers == null) return writerPolicy(owner, Collections.EMPTY_SET);
            return writerPolicy(owner, Arrays.asList(writers));
        }
        finally {
            exitTiming();
        }
    }
    
    public static IntegPolicy writerPolicy(Principal owner, PrincipalSet writers) {
        try {
            enterTiming();
            return writerPolicy(owner, writers.getSet());
        }
        finally {
            exitTiming();
        }
    }
    
    public static Label toLabel(ConfPolicy cPolicy, IntegPolicy iPolicy) {
        try {
            enterTiming();
            return new PairLabel(cPolicy, iPolicy);        
        }
        finally {
            exitTiming();
        }
    }
    public static Label toLabel(ConfPolicy policy) {
        try {
            enterTiming();
            return new PairLabel(policy, TOP_INTEG);
        }
        finally {
            exitTiming();
        }
    }
    public static Label toLabel(IntegPolicy policy) {
        try {
            enterTiming();
            return new PairLabel(BOTTOM_CONF, policy);
        }
        finally {
            exitTiming();
        }
    }
    
    
    public static Label join(Label l1, Label l2) {
        try {
            enterTiming();
            if (l1 == null) return l2;
            if (l2 == null) return l1;
            
            if (l1 instanceof PairLabel && l2 instanceof PairLabel) {
                Pair pair = new Pair(l1, l2);
                Label result = (Label)cacheLabelJoins.get(pair);
                if (result == null) {
                    PairLabel pl1 = (PairLabel)l1;
                    PairLabel pl2 = (PairLabel)l2;
                    Set dependencies = new HashSet();
                    result = new PairLabel(pl1.confPolicy().join(pl2.confPolicy(), dependencies),
                                           pl1.integPolicy().join(pl2.integPolicy(), dependencies));
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
                return result;            
                
            }
            // error! non pair labels!
            return null;
        }
        finally {
            exitTiming();
        }
    }
    public static Label meetLbl(Label l1, Label l2) {
        try {
            enterTiming();
            return meet(l1, l2);
        }
        finally {
            exitTiming();
        }
    }
    public static Label meet(Label l1, Label l2) {
        try {
            enterTiming();
            if (l1 == null) return l2;
            if (l2 == null) return l1;
            
            if (l1 instanceof PairLabel && l2 instanceof PairLabel) {
                Pair pair = new Pair(l1, l2);
                Label result = (Label)cacheLabelMeets.get(pair);
                if (result == null) {
                    PairLabel pl1 = (PairLabel)l1;
                    PairLabel pl2 = (PairLabel)l2;
                    Set dependencies = new HashSet();
                    result = new PairLabel(pl1.confPolicy().meet(pl2.confPolicy(), dependencies),
                                           pl1.integPolicy().meet(pl2.integPolicy(), dependencies));
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
                return result;                            
            }

            // error! non pair labels!
            return null;
        }
        finally {
            exitTiming();
        }
    }
    static public ConfPolicy join(ConfPolicy p1, ConfPolicy p2) {        
        return join(p1, p2, new HashSet());
    }
    static protected ConfPolicy join(ConfPolicy p1, ConfPolicy p2, Set s) {        
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
            return new JoinConfPolicy(comps);
        }
        finally {
            exitTiming();
        }
        
    }
    static public IntegPolicy join(IntegPolicy p1, IntegPolicy p2) {
        return join(p1, p2, new HashSet());
        
    }
    static IntegPolicy join(IntegPolicy p1, IntegPolicy p2, Set s) {        
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
            return new JoinIntegPolicy(comps);
        }
        finally {
            exitTiming();
        }
        
    }
    public static ConfPolicy meetPol(ConfPolicy p1, ConfPolicy p2) {
        return meet(p1, p2, new HashSet());
    }
    protected static ConfPolicy meet(ConfPolicy p1, ConfPolicy p2, Set s) {        
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
            return new MeetConfPolicy(comps);
        }
        finally {
            exitTiming();
        }
    }
    static public IntegPolicy meetPol(IntegPolicy p1, IntegPolicy p2) {
        return meet(p1, p2, new HashSet());
    }
     static IntegPolicy meet(IntegPolicy p1, IntegPolicy p2, Set s) {        
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
            return new MeetIntegPolicy(comps);
        }
        finally {
            exitTiming();
        }
        
    }
    
    
    
    public static boolean equivalentTo(Label l1, Label l2) {
        try {
            enterTiming();
            if (l1 == l2 || (l1 != null && l1.equals(l2))) return true;
            return relabelsTo(l1, l2) && relabelsTo(l2, l1);
        }
        finally {
            exitTiming();
        }
    }
    
    public static boolean isReadableBy(Label lbl, Principal p) {
        try {
            enterTiming();
            Label L = toLabel(PrincipalUtil.readableByPrinPolicy(p));
            return relabelsTo(lbl, L);
        }
        finally {
            exitTiming();
        }
    }
    
    public static boolean relabelsTo(Label from, Label to) {
        try {
            enterTiming();
            if (from == null || to == null) return false;
            if (from == to || from.equals(to)) return true;
            Pair pair = new Pair(from, to);
            if (cacheTrueLabelRelabels.contains(pair)) return true;
            if (cacheFalseLabelRelabels.contains(pair)) return false;
            Set dependencies = new HashSet();
            boolean result = from != null && from.relabelsTo(to, dependencies);
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
            return result;            
        }
        finally {
            exitTiming();
        }
    }

    public static boolean relabelsTo(Policy from, Policy to) {
        return relabelsTo(from, to, new HashSet());
    }

    protected static boolean relabelsTo(Policy from, Policy to, Set s) {
        try {
            enterTiming();
            if (from == null || to == null) return false;
            if (from == to || from.equals(to)) return true;
            Pair pair = new Pair(from, to);
            if (cacheTruePolicyRelabels.containsKey(pair)) {
                s.addAll((Set)cacheTruePolicyRelabels.get(pair));
                return true;
            }
            if (cacheFalsePolicyRelabels.contains(pair)) return false;
            Set dependencies = new HashSet();
            boolean result = from.relabelsTo(to, dependencies);
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
            return result;            
        }
        finally {
            exitTiming();
        }
    }
    
    public static String stringValue(Label lb) {
        try {
            enterTiming();
            if (lb == null) return "<null>";
            return lb.toString();
        }
        finally {
            exitTiming();
        }
    }
    
    public static String toString(Label lb) {
        try {
            enterTiming();
            return stringValue(lb);
        }
        finally {
            exitTiming();
        }
    }
    
    public static int hashCode(Label lb) {
        try {
            enterTiming();
            if (lb == null) return 0;
            return lb.hashCode();
        }
        finally {
            exitTiming();
        }
    }
        
    private static Set simplifyJoin(Set policies, Set dependencies) {
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
    private static Set simplifyMeet(Set policies, Set dependencies) {
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
    private static class Pair {
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

    static void notifyNewDelegation(Principal granter, Principal superior) {
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
    static void notifyRevokeDelegation(Principal granter, Principal superior) {
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
