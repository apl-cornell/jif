package jif.lang;

import java.util.*;
import java.lang.reflect.*;

public class InternalPrincipal extends AbstractPrincipal
{
    static PrincipalManager pm;
    
    protected InternalPrincipal(Set superiors) {
	super(superiors);
    }

    protected boolean actsForImpl(Principal principal) {
        if (principal instanceof InternalPrincipal) {
            return ((InternalPrincipal)principal).actedForBy(this);
        }
        return false; 
    }
    
    protected boolean actedForBy(InternalPrincipal superior) throws RuntimeException {
	if (this == superior) {
	    return true;
	}

	for (Iterator iter = superiors.iterator(); iter.hasNext(); ) {
	    InternalPrincipal sup = (InternalPrincipal) iter.next();

	    if (superior.actsFor(sup)) {
		return true;
	    }
	}
	return false;
    }
    
    public String name() {
	String fullName = this.getClass().getName();
	int index = fullName.lastIndexOf(".");
	if (index == -1) return fullName;
	
	return fullName.substring(index + 1);
    }

//    public static boolean actsFor(InternalPrincipal p1, InternalPrincipal p2) {
//        return pm.actsFor(p1, p2);
//    }
//
//    public static InternalPrincipal forName(String name)
//	throws PrincipalNotFoundException
//    {
//	String fullName = "jif.principal." + name;
//		
//	try {
//	    Class c = Class.forName(fullName);
//	    Field f = c.getField("P");
//	    return (InternalPrincipal) f.get(null);
//	}
//	catch (Exception e) {
//	    throw new PrincipalNotFoundException(name);
//	}
//    }
//
//    public static void addActsFor(InternalPrincipal actor, InternalPrincipal granter) {
//	actor.superiors.add(granter);
//	actor.cache.add(granter);
//    }

    static {
	String str = System.getProperty("PrincipalManager");
	pm = PMFactory.create(str);
    }
}
