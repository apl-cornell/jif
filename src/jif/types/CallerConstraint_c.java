package jif.types;

import polyglot.ext.jl.types.*;
import polyglot.util.*;
import java.util.*;

import jif.types.principal.Principal;

/** An implementation of the <code>CallerConstraint</code> interface. 
 */
public class CallerConstraint_c extends TypeObject_c implements CallerConstraint {
    protected List principals;

    public CallerConstraint_c(JifTypeSystem ts, Position pos,
	                        List principals) {
	super(ts, pos);
	this.principals = TypedList.copyAndCheck(principals, Principal.class, true);
    }

    public CallerConstraint principals(List principals) {
	CallerConstraint_c n = (CallerConstraint_c) copy();
	n.principals = TypedList.copyAndCheck(principals, Principal.class, true);
	return n;
    }

    public List principals() {
	return principals;
    }

    public String toString() {
	String s = "caller(";
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
