package jif.lang;

import java.util.*;

public class WriterPolicy implements IntegPolicy
{
    private final Principal owner;
    private final Set writers;
    
    public WriterPolicy(Principal owner, Collection writers) {
        this.owner = owner;
        if (writers == null) writers = Collections.EMPTY_SET; 
        this.writers = Collections.unmodifiableSet(new LinkedHashSet(writers));
    }
    
    public WriterPolicy(Principal owner, PrincipalSet writers) {
        this(owner, writers.getSet());
    }
    
    public Principal owner() {
        return owner;
    }
    
    public Set writers() {
        return writers;
    }
    
    
    public boolean relabelsTo(IntegPolicy p) {
        if (!(p instanceof WriterPolicy))
            return false;
        
        WriterPolicy pp = (WriterPolicy) p;
        
        // this = { o  : .. ri  .. }
        // p    = { o' : .. rj' .. }
        
        // o' >= o?
        
        if (! PrincipalUtil.actsFor(owner, pp.owner)) {
            return false;
        }
        
        // for all j . rj' >= o || exists i . rj' >= ri
        for (Iterator j = this.writers.iterator(); j.hasNext(); ) {
            Principal rj = (Principal) j.next();
            
            boolean sat = false;
            
            if (PrincipalUtil.actsFor(rj, pp.owner)) {
                sat = true;
            }
            else {
                for (Iterator i = pp.writers.iterator(); i.hasNext(); ) {
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
        return (owner==null?0:owner.hashCode()) + writers.size();
    }
    
    public boolean equals(Object o) {
        if (! (o instanceof WriterPolicy)) {
            return false;
        }
        
        WriterPolicy policy = (WriterPolicy) o;
        
        if (owner == policy || (owner != null && owner.equals(policy.owner)
                && policy.owner != null && policy.owner.equals(owner))) {
            return writers.containsAll(policy.writers) && 
            policy.writers.containsAll(writers);
        }
        
        return false;
    }
    
    public String componentString() {
        String str = (owner == null?"<null>":owner.name()) + ": ";
        for (Iterator iter = writers.iterator(); iter.hasNext(); ) {
            Principal writer = (Principal) iter.next();
            str += (writer == null?"<null>":writer.name());
            if (iter.hasNext()) str += ",";
        }
        return str;
    }
    
}
