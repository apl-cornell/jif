package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.Policy Policy}.
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

    public static Label privacyPolicyLabel(Principal owner, Collection readers) {
        return intern(new PrivacyPolicy(owner, readers));
    }

    /**
     * See the signature for the explanation of lbl.
     */
    public static Label privacyPolicyLabel(Label lbl, Principal owner, Principal[] readers) {
        if (readers == null) return privacyPolicyLabel(owner, Collections.EMPTY_SET);
	return privacyPolicyLabel(owner, Arrays.asList(readers));
    }

    public static Label privacyPolicyLabel(Principal owner, PrincipalSet readers) {
	return privacyPolicyLabel(owner, readers.getSet());
    }

    public static Label join(Label l1, Label l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        Set comps = new HashSet(flattenJoin(l1));
        comps.addAll(flattenJoin(l2));
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
        return relabelsTo(lbl, new ReadableByPrinLabel(p));
    }

    public static boolean relabelsTo(Label from, Label to) {
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
    
    private static Label intern(Label l) {
        Label in = (Label)intern.get(l);
        if (in == null) {
            in = l;
            intern.put(in, in);
        }
        return in;
    }

    protected static Set flattenJoin(Label l) {
        if (l instanceof JoinLabel) {
            JoinLabel jl = (JoinLabel)l;
            return jl.components();
            
        }
        else {
            return Collections.singleton(l);
        }
    }
    protected static Set flattenJoin(Set labels) {
        Set comps = new HashSet();
        for (Iterator iter = labels.iterator(); iter.hasNext(); ) {
            Label l = (Label)iter.next();
            comps.addAll(flattenJoin(l));
        }
        return comps;
    }
    
    private static Set simplifyJoin(Set labels) {
        Set needed = new LinkedHashSet();
        for (Iterator i = labels.iterator(); i.hasNext(); ) {
            Label ci = (Label)i.next();
            
            boolean subsumed = (ci == null); // null components are always subsumed.
            for (Iterator j = needed.iterator(); !subsumed && j.hasNext(); ) {
                Label cj = (Label) j.next();
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
