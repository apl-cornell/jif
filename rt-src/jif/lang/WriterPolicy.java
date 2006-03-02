package jif.lang;

import java.util.*;

public class WriterPolicy implements IntegPolicy
{
    private final Principal owner;
    private final Principal writer;
    private final Principal effectiveWriter; // disjunction of owner and writer
    
    public WriterPolicy(Principal owner, Principal writer) {
        this.owner = owner;
        this.writer = writer;
        this.effectiveWriter = PrincipalUtil.disjunction(owner, writer);
    }
    
    public Principal owner() {
        return owner;
    }
    
    public Principal writer() {
        return writer;
    }
    
    
    public boolean relabelsTo(Policy p) {
        if (p instanceof JoinIntegPolicy) {
            JoinPolicy jp = (JoinPolicy)p;
            // this <= p1 join ... join p2 if there exists a pi such that
            // this <= pi
            for (Iterator iter = jp.joinComponents().iterator(); iter.hasNext();) {
                Policy pi = (Policy)iter.next();
                if (relabelsTo(pi)) return true;                
            }
            return false;
        }
        else if (p instanceof MeetIntegPolicy) {
            MeetPolicy mp = (MeetPolicy)p;
            // this <= p1 meet ... meet p2 if for all pi 
            // this <= pi
            for (Iterator iter = mp.meetComponents().iterator(); iter.hasNext();) {
                Policy pi = (Policy)iter.next();
                if (!relabelsTo(pi)) return false;                
            }
            return true;            
        }
        else if (!(p instanceof WriterPolicy))
            return false;
        
        WriterPolicy pp = (WriterPolicy) p;
        
        // this = { o  : .. ri  .. }
        // p    = { o' : .. rj' .. }
        
        // o' >= o?
        
        if (! PrincipalUtil.actsFor(owner, pp.owner)) {
            return false;
        }
        
        // for all j . rj' >= o || exists i . rj' >= ri
        return PrincipalUtil.actsFor(this.effectiveWriter, pp.effectiveWriter);
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ (writer==null?0:writer.hashCode()) ^ -124978;
    }
    
    public boolean equals(Object o) {
        if (! (o instanceof WriterPolicy)) {
            return false;
        }
        
        WriterPolicy policy = (WriterPolicy) o;
        
        if (owner == policy.owner || (owner != null && owner.equals(policy.owner)
                && policy.owner != null && policy.owner.equals(owner))) {
            return (writer == policy.writer || (writer != null && writer.equals(policy.writer)
                    && policy.writer != null && policy.writer.equals(writer)));
        }
        
        return false;
    }
    
    public String toString() {
        String str = PrincipalUtil.toString(owner) + "!: ";
        if (!PrincipalUtil.isTopPrincipal(writer))
            str += PrincipalUtil.toString(writer);
        return str;
    }

    public IntegPolicy join(IntegPolicy p) {
        return LabelUtil.join(this, p);
    }

    public IntegPolicy meet(IntegPolicy p) {
        return LabelUtil.meet(this, p);
    }
    
}
