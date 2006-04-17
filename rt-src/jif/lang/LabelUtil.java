package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.IntegPolicy Policy}.
 *  
 */
public class LabelUtil
{
    private static final ConfPolicy BOTTOM_CONF;
    private static final IntegPolicy TOP_INTEG;
    private static final Map intern = new HashMap();

    static {
        BOTTOM_CONF = new ReaderPolicy(null, null);
        TOP_INTEG = new WriterPolicy(null, null);
    }

    private LabelUtil() { }

    public static Label noComponents() {
        return intern(new PairLabel(BOTTOM_CONF, TOP_INTEG));
    }
    
    public static ConfPolicy readerPolicy(Principal owner, Principal reader) {
        return intern(new ReaderPolicy(owner, reader));
    }
    public static ConfPolicy readerPolicy(Principal owner, Collection readers) {
        return intern(new ReaderPolicy(owner, PrincipalUtil.disjunction(readers)));
    }
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static ConfPolicy readerPolicy(Label lbl, Principal owner, Principal[] readers) {
        if (readers == null) return readerPolicy(owner, Collections.EMPTY_SET);
        return readerPolicy(owner, Arrays.asList(readers));
    }

    public static ConfPolicy readerPolicy(Principal owner, PrincipalSet writers) {
        return readerPolicy(owner, writers.getSet());
    }
    
    public static Label readerPolicyLabel(Principal owner, Principal reader) {
        return toLabel(intern(new ReaderPolicy(owner, reader)));
    }
    public static Label readerPolicyLabel(Principal owner, Collection readers) {        
        Label l = toLabel(intern(new ReaderPolicy(owner, PrincipalUtil.disjunction(readers))));
        return l;
    }

    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static Label readerPolicyLabel(Label lbl, Principal owner, Principal[] readers) {
        if (readers == null) return readerPolicyLabel(owner, Collections.EMPTY_SET);
        return readerPolicyLabel(owner, Arrays.asList(readers));
    }

    public static Label readerPolicyLabel(Principal owner, PrincipalSet readers) {
        return readerPolicyLabel(owner, PrincipalUtil.disjunction(readers.getSet()));
    }

    public static IntegPolicy writerPolicy(Principal owner, Principal writer) {
        return intern(new WriterPolicy(owner, writer));
    }
    public static IntegPolicy writerPolicy(Principal owner, Collection writers) {
        return intern(new WriterPolicy(owner, PrincipalUtil.disjunction(writers)));
    }
    public static Label writerPolicyLabel(Principal owner, Principal writer) {
        return toLabel(intern(new WriterPolicy(owner, writer)));
    }
    public static Label writerPolicyLabel(Principal owner, Collection writers) {
        return toLabel(intern(new WriterPolicy(owner, PrincipalUtil.disjunction(writers))));
    }
    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static Label writerPolicyLabel(Label lbl, Principal owner, Principal[] writers) {
        if (writers == null) return writerPolicyLabel(owner, Collections.EMPTY_SET);
        return writerPolicyLabel(owner, Arrays.asList(writers));
    }

    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static IntegPolicy writerPolicy(Label lbl, Principal owner, Principal[] writers) {
        if (writers == null) return writerPolicy(owner, Collections.EMPTY_SET);
        return writerPolicy(owner, Arrays.asList(writers));
    }

    public static IntegPolicy writerPolicy(Principal owner, PrincipalSet writers) {
        return writerPolicy(owner, writers.getSet());
    }
    
    public static Label toLabel(ConfPolicy cPolicy, IntegPolicy iPolicy) {
        return intern(new PairLabel(cPolicy, iPolicy));        
    }
    public static Label toLabel(ConfPolicy policy) {
        return intern(new PairLabel(policy, TOP_INTEG));
    }
    public static Label toLabel(IntegPolicy policy) {
        return intern(new PairLabel(BOTTOM_CONF, policy));
    }
    

    public static Label join(Label l1, Label l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        if (l1 instanceof PairLabel && l2 instanceof PairLabel) {
            PairLabel pl1 = (PairLabel)l1;
            PairLabel pl2 = (PairLabel)l2;
                        
            return intern(new PairLabel(pl1.confPolicy().join(pl2.confPolicy()),
                                        pl1.integPolicy().join(pl2.integPolicy())));
        }
        // error! non pair labels!
        return null;
    }
    public static Label meetLbl(Label l1, Label l2) {
        return meet(l1, l2);
    }
    public static Label meet(Label l1, Label l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        if (l1 instanceof PairLabel && l2 instanceof PairLabel) {
            PairLabel pl1 = (PairLabel)l1;
            PairLabel pl2 = (PairLabel)l2;
                        
            return intern(new PairLabel(pl1.confPolicy().meet(pl2.confPolicy()),
                                        pl1.integPolicy().meet(pl2.integPolicy())));
        }
        // error! non pair labels!
        return null;
    }
    public static ConfPolicy join(ConfPolicy p1, ConfPolicy p2) {        
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
        comps = simplifyJoin(comps);
        
        if (comps.size() == 1) {
            return (ConfPolicy)comps.iterator().next();
        }
        return new JoinConfPolicy(comps);
    }
    public static IntegPolicy join(IntegPolicy p1, IntegPolicy p2) {        
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
        comps = simplifyJoin(comps);
        
        if (comps.size() == 1) {
            return (IntegPolicy)comps.iterator().next();
        }
        return new JoinIntegPolicy(comps);
    }
    public static ConfPolicy meetPol(ConfPolicy p1, ConfPolicy p2) {
        return meet(p1, p2);
    }
    public static ConfPolicy meet(ConfPolicy p1, ConfPolicy p2) {        
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
        comps = simplifyMeet(comps);
        
        if (comps.size() == 1) {
            return (ConfPolicy)comps.iterator().next();
        }
        return new MeetConfPolicy(comps);
    }
    public static IntegPolicy meetPol(IntegPolicy p1, IntegPolicy p2) {
        return meet(p1, p2);
    }
    public static IntegPolicy meet(IntegPolicy p1, IntegPolicy p2) {        
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
        comps = simplifyMeet(comps);
        
        if (comps.size() == 1) {
            return (IntegPolicy)comps.iterator().next();
        }
        return new MeetIntegPolicy(comps);
    }

    

    public static boolean equivalentTo(Label l1, Label l2) {
        l1 = intern(l1);
        l2 = intern(l2);
        if (l1 == l2) return true;
        return l1 != null && l2 != null && l1.relabelsTo(l2) && l2.relabelsTo(l1);
    }

    public static boolean isReadableBy(Label lbl, Principal p) {
        Label L = toLabel(PrincipalUtil.readableByPrinPolicy(p));
        return relabelsTo(lbl, L);
    }

    public static boolean relabelsTo(Label from, Label to) {
        from = intern(from);
        to = intern(to);
        if (from == to) return true;
        
        return from != null && from.relabelsTo(to);
    }

    private static boolean relabelsTo(Policy from, Policy to) {
        from = intern(from);
        to = intern(to);
        if (from == to) return true;
        
        return from != null && from.relabelsTo(to);
    }
    
    public static String stringValue(Label lb) {
        if (lb == null) return "<null>";
        return lb.toString();
    }

    public static String toString(Label lb) {
        return stringValue(lb);
    }
    
    public static int hashCode(Label lb) {
        if (lb == null) return 0;
    	return lb.hashCode();
    }
    
    private static Policy intern(Policy pol) {
        Policy in = (Policy)intern.get(pol);
        if (in == null) {
            in = pol;
            intern.put(in, in);
        }
        return in;        
    }
    private static ConfPolicy intern(ConfPolicy confPol) {
        return (ConfPolicy)intern((Policy)confPol);        
    }
    private static IntegPolicy intern(IntegPolicy integPol) {
        return (IntegPolicy)intern((Policy)integPol);        
    }
    private static Label intern(Label l) {
        Label in = (Label)intern.get(l);
        if (in == null) {
            in = l;
            intern.put(in, in);
        }
        return in;
    }

    private static Set simplifyJoin(Set policies) {
        Set needed = new LinkedHashSet();
        for (Iterator i = policies.iterator(); i.hasNext(); ) {
            Policy ci = (Policy)i.next();
            
            boolean subsumed = (ci == null); // null components are always subsumed.
            for (Iterator j = needed.iterator(); !subsumed && j.hasNext(); ) {
                Policy cj = (Policy) j.next();
                if (relabelsTo(ci, cj)) {
                    subsumed = true;
                    break;
                }
                
                if (relabelsTo(cj, ci)) { 
                    j.remove();
                }
            }
            
            if (!subsumed) needed.add(ci);
        }
        
        return needed;        
    }
    private static Set simplifyMeet(Set policies) {
        Set needed = new LinkedHashSet();
        for (Iterator i = policies.iterator(); i.hasNext(); ) {
            Policy ci = (Policy)i.next();
            
            boolean subsumed = (ci == null); // null components are always subsumed.
            for (Iterator j = needed.iterator(); !subsumed && j.hasNext(); ) {
                Policy cj = (Policy) j.next();
                if (relabelsTo(cj, ci)) {
                    subsumed = true;
                    break;
                }
                
                if (relabelsTo(ci, cj)) { 
                    j.remove();
                }
            }
            
            if (!subsumed) needed.add(ci);
        }
        
        return needed;        
    }
    
}
