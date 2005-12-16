package jif.lang;

import java.util.*;

public class ReaderPolicy extends AbstractPolicy implements ConfPolicy
{
    private final Principal owner;
    private final Principal reader;
    private final Principal effectiveReader; // disjunction of owner and reader
    
    public ReaderPolicy(Principal owner, Principal reader) {
        this.owner = owner;
        this.reader = reader;
        this.effectiveReader = PrincipalUtil.disjunction(owner, reader);
    }
    
    public Principal owner() {
        return owner;
    }
    
    public Principal reader() {
        return reader;
    }
    
    
    public boolean relabelsTo(Policy p) {
        if (p instanceof ReadableByPrinPolicy) {
            ReadableByPrinPolicy rbpp = (ReadableByPrinPolicy)p;
            if (PrincipalUtil.actsFor(rbpp.reader(),this.effectiveReader)) {
                return true;
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
        return PrincipalUtil.actsFor(pp.reader, reader);        
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ (reader==null?0:reader.hashCode()) ^ 4238;
    }
    
    public boolean equals(Object o) {
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
    
    public String componentString() {
        String str = (owner == null?"<null>":owner.name()) + ": ";
        str += (reader == null?"<null>":reader.name());
        return str;
    }
    
}
