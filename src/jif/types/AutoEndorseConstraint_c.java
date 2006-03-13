package jif.types;

import polyglot.ext.jl.types.*;
import polyglot.util.*;
import java.util.*;

import jif.types.principal.Principal;

/** An implementation of the <code>CallerConstraint</code> interface. 
 */
public class AutoEndorseConstraint_c extends TypeObject_c implements AutoEndorseConstraint {
    protected List principals;

    public AutoEndorseConstraint_c(JifTypeSystem ts, Position pos,
	                        List principals) {
	super(ts, pos);
	this.principals = TypedList.copyAndCheck(principals, Principal.class, true);
    }

    public AutoEndorseConstraint principals(List principals) {
	AutoEndorseConstraint_c n = (AutoEndorseConstraint_c) copy();
	n.principals = TypedList.copyAndCheck(principals, Principal.class, true);
	return n;
    }

    public List principals() {
	return principals;
    }

    public String toString() {
	String s = "autoendorse(";
	for (Iterator i = principals.iterator(); i.hasNext(); ) {
	    Principal p = (Principal) i.next();
	    s += p;
	    if (i.hasNext()) {
		s += ", ";
	    }
	}
	s += ")";
	return s;
    }

    public boolean isCanonical() {
	return true;
    }
}
