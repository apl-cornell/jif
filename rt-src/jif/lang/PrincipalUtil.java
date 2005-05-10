package jif.lang;

/**
 * TODO Documentation 
 */
public class PrincipalUtil {
    /**
     * Does the principal p act for the principal q?
     */
    public static boolean actsFor(Principal p, Principal q) {
        // anyone can act for the "null" principal
        if (q == null) return true;
        
        // if the two principals are ==-equal, or if they
        // both agree that they are equal to each other, then
        // we return true (since the acts-for relation is 
        // reflexive).
        if (p == q) return true;
        if (q.equals(p) && p != null && p.equals(q)) return true;
        
        // try a simple test first
        if (q.delegatesTo(p)) return true;
        
        // try the cache
        
        // try searching
        
        
        return false;
    }
    
    /**
     * Are the principals p and q equivalent to each other? That is,
     * does p act for q, and q act for p?
     * 
     */
    public static boolean equivalentTo(Principal p, Principal q) {
        return actsFor(p, q) && actsFor(q, p);
    }
}
