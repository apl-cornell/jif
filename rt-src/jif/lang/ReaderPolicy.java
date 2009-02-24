package jif.lang;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ReaderPolicy extends AbstractPolicy implements ConfPolicy
{
    private final Principal owner;
    private final Principal reader;
    
    public ReaderPolicy(LabelUtil labelUtil, Principal owner, Principal reader) {
        super(labelUtil);
        this.owner = owner;
        this.reader = reader;
    }
    
    public Principal owner() {
        return owner;
    }
    
    public Principal reader() {
        return reader;
    }
    
    
    public boolean relabelsTo(Policy p, Set s) {
        if (this == p || this.equals(p)) return true;
        
        if (p instanceof JoinConfPolicy) {
            JoinPolicy jp = (JoinPolicy)p;
            // this <= p1 join ... join p2 if there exists a pi such that
            // this <= pi
            for (Iterator iter = jp.joinComponents().iterator(); iter.hasNext();) {
                Policy pi = (Policy)iter.next();
                if (labelUtil.relabelsTo(this, pi, s)) return true;                
            }
            return false;
        }
        else if (p instanceof MeetConfPolicy) {
            MeetPolicy mp = (MeetPolicy)p;
            // this <= p1 meet ... meet p2 if for all pi 
            // this <= pi
            Set temp = new HashSet();
            for (Iterator iter = mp.meetComponents().iterator(); iter.hasNext();) {
                Policy pi = (Policy)iter.next();
                if (!labelUtil.relabelsTo(this, pi, temp)) return false;                
            }
            s.addAll(temp);
            return true;            
        }
        else if (!(p instanceof ReaderPolicy))
            return false;
        
        ReaderPolicy pp = (ReaderPolicy) p;
        
        // this = { o  : .. ri  .. }
        // p    = { o' : .. rj' .. }
        
        // o' >= o?
    
        ActsForProof ownersProof = PrincipalUtil.actsForProof(pp.owner, owner);
        if (ownersProof == null) {
            return false;
        }
        ActsForProof readerReaderProof = PrincipalUtil.actsForProof(pp.reader, this.reader);
        if (readerReaderProof != null) {
            ownersProof.gatherDelegationDependencies(s);
            readerReaderProof.gatherDelegationDependencies(s);
            return true;
        }
        ActsForProof readerOwnerProof = PrincipalUtil.actsForProof(pp.reader, this.owner);
        if (readerOwnerProof != null) {
            ownersProof.gatherDelegationDependencies(s);
            readerOwnerProof.gatherDelegationDependencies(s);      
            return true;
        }
        return false;
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
        String str = PrincipalUtil.toString(owner) + "->";
        if (!PrincipalUtil.isTopPrincipal(reader))
            str += PrincipalUtil.toString(reader);
        return str;
    }

    public ConfPolicy join(ConfPolicy p, Set s) {
        return labelUtil.join(this, p, s);
    }

    public ConfPolicy join(ConfPolicy p) {
        return labelUtil.join(this, p);
    }

    public ConfPolicy meet(ConfPolicy p, Set s) {
        return labelUtil.meet(this, p, s);
    }
    public ConfPolicy meet(ConfPolicy p) {
        return labelUtil.meetPol(this, p);
    }
    
}
