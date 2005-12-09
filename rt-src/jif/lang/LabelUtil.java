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

    public static ConfPolicy readerPolicyLabel(Principal owner, Collection readers) {
        return intern(new ReaderPolicy(owner, readers));
    }

    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static ConfPolicy readerPolicyLabel(Label lbl, Principal owner, Principal[] readers) {
        if (readers == null) return readerPolicyLabel(owner, Collections.EMPTY_SET);
        return readerPolicyLabel(owner, Arrays.asList(readers));
    }

    public static ConfPolicy readerPolicyLabel(Principal owner, PrincipalSet readers) {
        return readerPolicyLabel(owner, readers.getSet());
    }

    public static IntegPolicy writerPolicyLabel(Principal owner, Collection writers) {
        return intern(new WriterPolicy(owner, writers));
    }

    /**
     * See the Jif signature for the explanation of lbl.
     */
    public static IntegPolicy writerPolicyLabel(Label lbl, Principal owner, Principal[] writers) {
        if (writers == null) return writerPolicyLabel(owner, Collections.EMPTY_SET);
        return writerPolicyLabel(owner, Arrays.asList(writers));
    }

    public static IntegPolicy writerPolicyLabel(Principal owner, PrincipalSet writers) {
        return writerPolicyLabel(owner, writers.getSet());
    }

    public static ConfIntegPair pairLabel(ConfCollection confPols, IntegCollection integPols) {
        return intern(new ConfIntegPair(confPols, integPols));        
    }
    public static ConfIntegPair pairLabel(ConfPolicy confPol, IntegPolicy integPol) {
        return intern(new ConfIntegPair(confCollection(confPol), 
                                        integCollection(integPol)));        
    }
    
    public static Label label(ConfIntegPair pair) {
        return intern(new JoinLabel(pair));
    }
    public static ConfCollection bottomConf() {
        return new ConfCollection();
    }
    public static ConfCollection confCollection(ConfPolicy cp) {
        return new ConfCollection(cp);        
    }
    
    public static IntegCollection topInteg() {
        return new IntegCollection();
    }
    public static IntegCollection integCollection(IntegPolicy ip) {
        return new IntegCollection(ip);        
    }
    
    public static ConfCollection join(ConfCollection cc, ConfPolicy cp) {
        if (cp == null) return cc;
        if (cc == null) return new ConfCollection(cp);
        
        for (Iterator comps = cc.joinComponents().iterator(); comps.hasNext(); ) {
            ConfPolicy c = (ConfPolicy)comps.next();
            if (cp.relabelsTo(c)) return cc;
        }
        Set comps = new LinkedHashSet(cc.joinComponents());
        comps.add(cp);

        return new ConfCollection(comps);
    }
    public static IntegCollection meet(IntegCollection ic, IntegPolicy ip) {
        if (ip == null) return ic;
        if (ic == null) return new IntegCollection(ip);
        
        for (Iterator comps = ic.meetComponents().iterator(); comps.hasNext(); ) {
            IntegPolicy c = (IntegPolicy)comps.next();
            if (c.relabelsTo(ip)) return ic;
        }
        Set comps = new LinkedHashSet(ic.meetComponents());
        comps.add(ip);

        return new IntegCollection(comps);
    }

    public static Label join(Label l1, Label l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        Set comps = new LinkedHashSet(flattenJoin(l1));
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
        ConfPolicy readable = new ReadableByPrinPolicy(p);
        ConfCollection confCol = new ConfCollection(readable);
        return relabelsTo(lbl, new JoinLabel(new ConfIntegPair(confCol, new IntegCollection())));
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
    
    private static ConfIntegPair intern(ConfIntegPair cip) {
        ConfIntegPair in = (ConfIntegPair)intern.get(cip);
        if (in == null) {
            in = cip;
            intern.put(in, in);
        }
        return in;        
    }
    private static ConfPolicy intern(ConfPolicy confPol) {
        ConfPolicy in = (ConfPolicy)intern.get(confPol);
        if (in == null) {
            in = confPol;
            intern.put(in, in);
        }
        return in;        
    }
    private static IntegPolicy intern(IntegPolicy integPol) {
        IntegPolicy in = (IntegPolicy)intern.get(integPol);
        if (in == null) {
            in = integPol;
            intern.put(in, in);
        }
        return in;        
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
            return jl.joinComponents();
            
        }
        else {
            return Collections.singleton(l);
        }
    }
    protected static Set flattenJoin(Set labels) {
        Set comps = new LinkedHashSet();
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
