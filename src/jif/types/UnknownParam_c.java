package jif.types;

import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>UnknownParam</code> interface. 
 */
public class UnknownParam_c extends Param_c implements UnknownParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

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
