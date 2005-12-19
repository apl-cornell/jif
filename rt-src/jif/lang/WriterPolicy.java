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
    
    public String componentString() {
        String str = PrincipalUtil.toString(owner) + ": ";
        if (!PrincipalUtil.isTopPrincipal(writer))
            str += PrincipalUtil.toString(writer);
        return str;
    }
    
}
