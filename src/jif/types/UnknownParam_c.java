package jif.types;

import polyglot.util.Position;

/** An implementation of the <code>UnknownParam</code> interface. 
 */
public class UnknownParam_c extends Param_c
                               implements UnknownParam
{
    public UnknownParam_c(JifTypeSystem ts, Position pos) {
	super(ts, pos);
    }

    @Override
    public boolean isRuntimeRepresentable() {
	return false;
    }

    @Override
    public boolean isCanonical() {
	return false;
    }

    @Override
    public String toString() {
	return "<unknown param>";
    }
}
