package jif.types;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.types.Package;
import polyglot.ext.jl.types.*;
import polyglot.ext.param.types.*;
import java.util.*;
import java.io.*;

/**
 * A place holder type used to serialize types that cannot be serialized.  
 */
public class JifPlaceHolder_c extends PlaceHolder_c implements PlaceHolder
{
    JifSubst subst;
    Position pos;

    public JifPlaceHolder_c() {
	subst = null;
    }

    public JifPlaceHolder_c(Type t) {
	super(t);
	pos = t.position();
	if (t instanceof JifSubstClassType_c) {
	    subst = (JifSubst) ((JifSubstClassType_c) t).subst();
	}
    }

    public TypeObject resolve(TypeSystem ts) {
	TypeObject t = super.resolve(ts);
	
	if (subst != null) {
	    return new JifSubstClassType_c((JifTypeSystem)ts, pos, 
			(ClassType) t, subst);
	}

	return t;
    }

    public String toString() {
	return super.toString() +    
	    (subst == null ? "" : subst.toString());
    }
}

	    
