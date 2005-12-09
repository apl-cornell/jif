package jif.lang;

import java.util.*;

public class ReaderPolicy implements ConfPolicy
{
    private final Principal owner;
    private final Set readers;
    
    public ReaderPolicy(Principal owner, Collection readers) {
        this.owner = owner;
        if (readers == null) readers = Collections.EMPTY_SET; 
        this.readers = Collections.unmodifiableSet(new LinkedHashSet(readers));
    }
    
    public ReaderPolicy(Principal owner, PrincipalSet readers) {
        this(owner, readers.getSet());
    }
    
    public Principal owner() {
        return owner;
    }
    
    public Set readers() {
        return readers;
    }
    
    
    public boolean relabelsTo(ConfPolicy p) {
        if (p instanceof ReadableByPrinPolicy) {
            ReadableByPrinPolicy rbpp = (ReadableByPrinPolicy)p;
            if (PrincipalUtil.actsFor(rbpp.reader(),this.owner)) {
                return true;
            }
            for (Iterator i = readers.iterator(); i.hasNext(); ) {
                Principal ri = (Principal) i.next();
                
                if (PrincipalUtil.actsFor(rbpp.reader(), ri)) {
                    return true;
                }
            }
            return false;            
        }
        if (!(p instanceof ReaderPolicy))
            return false;
        
        ReaderPolicy pp = (ReaderPolicy) p;
        
        // this = { o  : .. ri  .. }
        // p    = { o' : .. rj' .. }
        
        // o' >= o?
        
        if (! PrincipalUtil.actsFor(pp.owner, owner)) {
            return false;
        }
        
        // for all j . rj' >= o || exists i . rj' >= ri
        for (Iterator j = pp.readers.iterator(); j.hasNext(); ) {
            Principal rj = (Principal) j.next();
            
            boolean sat = false;
            
            if (PrincipalUtil.actsFor(rj, owner)) {
                sat = true;
            }
            else {
                for (Iterator i = readers.iterator(); i.hasNext(); ) {
                    Principal ri = (Principal) i.next();
                    
                    if (PrincipalUtil.actsFor(rj, ri)) {
                        sat = true;
                        break;
                    }
                }
            }
            
            if (! sat) {
                return false;
            }
        }
        return true;
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) + readers.size();
    }
    
    public boolean equals(Object o) {
        if (! (o instanceof ReaderPolicy)) {
            return false;
        }
        
        ReaderPolicy policy = (ReaderPolicy) o;
        
        if (owner == policy || (owner != null && owner.equals(policy.owner)
                && policy.owner != null && policy.owner.equals(owner))) {
            return readers.containsAll(policy.readers) && 
            policy.readers.containsAll(readers);
        }
        
        return false;
    }
    
    public String componentString() {
        String str = (owner == null?"<null>":owner.name()) + ": ";
        for (Iterator iter = readers.iterator(); iter.hasNext(); ) {
            Principal reader = (Principal) iter.next();
            str += (reader == null?"<null>":reader.name());
            if (iter.hasNext()) str += ",";
        }
        return str;
    }
    
}
