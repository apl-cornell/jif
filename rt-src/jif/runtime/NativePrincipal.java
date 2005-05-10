package jif.runtime;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jif.lang.*;
import jif.lang.Closure;
import jif.lang.Principal;
import jif.lang.PrincipalUtil;

/**
 * A NativePrincipal represents the file system users and groups.
 */
public class NativePrincipal implements Principal {
    private final String name;
    protected final Set superiors = new HashSet();
    
    protected NativePrincipal(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Set superiors() {
        return this.superiors;
    }

    public boolean delegatesTo(Principal p) {
        return superiors.contains(p);
    }

    public boolean isAuthorized(Object authorizationProof, Closure closure, Label lb) {
        // The default is that this principal authorizes no closures.
        return false;
    }
    
    public Principal[] findChainDownto(Principal q) {
        // don't even try! We don't have any information
        // about who we can act for.
        return null;
    }

    public Principal[] findChainUpto(Principal p) {
        // go through our set of superiors, and see if we can find a chain
        // from them to p.
        Principal[] chain;
        for (Iterator iter = superiors.iterator(); iter.hasNext(); ) {
            Principal s = (Principal)iter.next();
            chain = PrincipalUtil.findDelegatesChain(p, s);
            if (chain != null) {
                // success!
                // create a longer chain with this at the bottom 
                return addToChainBottom(chain, this);
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof Principal) {
            return equals((Principal)o);
        }
        return false;
    }
    
    public boolean equals(Principal p) {
        return (this.name == p.name() || (this.name != null && 
                this.name.equals(p.name()))) &&
                	this.getClass() == p.getClass();        
    }
    
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Create a new chain of length <code>chain.length+1</code>, such that
     * the last element of the new chain is <code>principal</code>, and 
     * all other elements are copied over from <code>chain</code>.
     */
    static protected Principal[] addToChainBottom(Principal[] chain, Principal principal) {
        if (chain == null) {
            Principal[] newChain = new Principal[1];
            newChain[0] = principal;
            return newChain;
        }
        
        Principal[] newChain = new Principal[chain.length + 1];
        for (int i = 0; i < chain.length; i++) {
            newChain[i] = chain[i];
        }
        newChain[chain.length] = principal;
        return newChain;
    }

    /**
     * Create a new chain of length <code>chain.length+1</code>, such that
     * the first element of the new chain is <code>principal</code>, and 
     * all other elements are copied over from <code>chain</code>, offset by one.
     */
    static protected Principal[] addToChainTop(Principal principal, Principal[] chain) {
        if (chain == null) {
            Principal[] newChain = new Principal[1];
            newChain[0] = principal;
            return newChain;
        }
        
        Principal[] newChain = new Principal[chain.length + 1];
        newChain[0] = principal;
        for (int i = 0; i < chain.length; i++) {
            newChain[i+1] = chain[i];
        }
        return newChain;
    }
}
