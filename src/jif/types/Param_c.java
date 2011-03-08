package jif.types;

import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public abstract class Param_c extends TypeObject_c implements Param {

    public Param_c() {
        super();
    }

    public Param_c(TypeSystem ts, Position pos) {
        super(ts, pos);
    }

    public Param_c(TypeSystem ts) {
        super(ts);
    }

    @Override
    public JifTypeSystem typeSystem() {
        return (JifTypeSystem) super.typeSystem();
    }
}
