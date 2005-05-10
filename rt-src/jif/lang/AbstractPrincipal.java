package jif.lang;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO Documentation 
 */
public abstract class AbstractPrincipal implements Principal {
    private final String name;
    protected final Set superiors = new HashSet();
    
    protected AbstractPrincipal(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public boolean delegatesTo(Principal p) {
        return superiors.contains(p);
    }

    /**
     * The default is that no closures are authorized.
     */
    public boolean isAuthorized(Object authorizationProof, Closure closure) {
        return false;
    }
    
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof Principal) {
            Principal p = (Principal)o;
            return (this.name == p.name() || (this.name !=null && 
                                                this.name.equals(p.name()))) &&
                    this.getClass() == p.getClass();
        }
        return false;
    }
    
    public int hashCode() {
        return name.hashCode();
    }

}
