package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.IntegPolicy Policy}.
 *  
 */
public class LabelUtil
{
    private static final Label BOTTOM = new JoinLabel();
    private static final Map intern = new HashMap();

    static {
	intern.put(BOTTOM, BOTTOM);
    }

    private LabelUtil() { }


    public static Label bottom() {
	return BOTTOM;
    }

    public static ConfPolicy readerPolicy(Principal owner, Principal reader) {
        return intern(new ReaderPolicy(owner, reader));
    }
    public static ConfPolicy readerPolicy(Principal owner, Collection readers) {
        return intern(new ReaderPolicy(owner, PrincipalUtil.disjunction(readers)));
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
    public static IntegPolicy writerPolicy(Label lbl, Principal owner, Principal[] writers) {
        if (writers == null) return writerPolicy(owner, Collections.EMPTY_SET);
        return writerPolicy(owner, Arrays.asList(writers));
    }

    public static IntegPolicy writerPolicy(Principal owner, PrincipalSet writers) {
        return writerPolicy(owner, writers.getSet());
    }
    
    public static Label toLabel(Policy policy) {
        return intern(new JoinLabel(Collections.singleton(policy)));
    }
    

    public static Label join(Label l1, Label l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        Set comps = new LinkedHashSet(l1.joinComponents());
        comps.addAll(l2.joinComponents());
        comps = simplifyJoin(comps);
        if (comps.size() == 1) {
            return (Label)comps.iterator().next();
        }

        return intern(new JoinLabel(comps));
    }

    public static boolean equivalentTo(Label l1, Label l2) {
        l1 = intern(l1);
        l2 = intern(l2);
        if (l1 == l2) return true;
        return l1 != null && l2 != null && l1.relabelsTo(l2) && l2.relabelsTo(l1);
    }

    public static boolean isReadableBy(Label lbl, Principal p) {
        Label L = toLabel(new ReadableByPrinPolicy(p));
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

    protected static Set flattenJoin(Set labels) {
        Set comps = new LinkedHashSet();
        for (Iterator iter = labels.iterator(); iter.hasNext(); ) {
            Label l = (Label)iter.next();
            comps.addAll(l.joinComponents());
        }
        return comps;
    }
    
    private static Set simplifyJoin(Set labels) {
        Set needed = new LinkedHashSet();
        for (Iterator i = labels.iterator(); i.hasNext(); ) {
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
    
}
