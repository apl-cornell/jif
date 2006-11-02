package jif.lang;

import java.util.Iterator;

public class ReaderPolicy extends AbstractPolicy implements ConfPolicy
{
    private final Principal owner;
    private final Principal reader;
    
    public ReaderPolicy(Principal owner, Principal reader) {
        this.owner = owner;
        this.reader = reader;
    }
    
    public Principal owner() {
        return owner;
    }
    
    public Principal reader() {
        return reader;
    }
    
    
    public boolean relabelsTo(Policy p) {
        if (this == p || this.equals(p)) return true;
        
        if (p instanceof JoinConfPolicy) {
            JoinPolicy jp = (JoinPolicy)p;
            // this <= p1 join ... join p2 if there exists a pi such that
            // this <= pi
            for (Iterator iter = jp.joinComponents().iterator(); iter.hasNext();) {
                Policy pi = (Policy)iter.next();
                if (relabelsTo(pi)) return true;                
            }
            return false;
        }
        else if (p instanceof MeetConfPolicy) {
            MeetPolicy mp = (MeetPolicy)p;
            // this <= p1 meet ... meet p2 if for all pi 
            // this <= pi
            for (Iterator iter = mp.meetComponents().iterator(); iter.hasNext();) {
                Policy pi = (Policy)iter.next();
                if (!relabelsTo(pi)) return false;                
            }
            return true;            
        }
        else if (!(p instanceof ReaderPolicy))
            return false;
        
        ReaderPolicy pp = (ReaderPolicy) p;
        
        // this = { o  : .. ri  .. }
        // p    = { o' : .. rj' .. }
        
        // o' >= o?
        
        if (! PrincipalUtil.actsFor(pp.owner, owner)) {
            return false;
        }
        return PrincipalUtil.actsFor(pp.reader, reader) || 
               PrincipalUtil.actsFor(pp.reader, owner);        
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ (reader==null?0:reader.hashCode()) ^ 4238;
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof ReaderPolicy)) {
            return false;
        }
        
        ReaderPolicy policy = (ReaderPolicy) o;
        
        if (owner == policy.owner || (owner != null && owner.equals(policy.owner)
                && policy.owner != null && policy.owner.equals(owner))) {
            return (reader == policy.reader || (reader != null && reader.equals(policy.reader)
                    && policy.reader != null && policy.reader.equals(reader)));
        }
        
        return false;
    }
    
    public String toString() {
        String str = PrincipalUtil.toString(owner) + ": ";
        if (!PrincipalUtil.isTopPrincipal(reader))
            str += PrincipalUtil.toString(reader);
        return str;
    }

    public ConfPolicy join(ConfPolicy p) {
        return LabelUtil.join(this, p);
    }

    public ConfPolicy meet(ConfPolicy p) {
        return LabelUtil.meet(this, p);
    }
    
}
