package jif.types;

import polyglot.ext.jl.types.*;
import polyglot.types.*;
import polyglot.util.*;

/** An implementation of the <code>UnknownParam</code> interface. 
 */
public class UnknownParam_c extends TypeObject_c
                               implements UnknownParam
{
    public UnknownParam_c(JifTypeSystem ts, Position pos) {
	super(ts, pos);
    }

    public boolean isRuntimeRepresentable() {
	return false;
    }

    public boolean isCanonical() {
	return false;
    }

    public String toString() {
	return "<unknown param>";
    }
}
