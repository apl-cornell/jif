package jif.types;

import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class TypeParam_c extends Param_c implements TypeParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public Type type;

    public TypeParam_c(Position pos, Type t) {
        super(t.typeSystem(), pos);
        this.type = t;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return type.isCanonical();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public Type type() {
        return type;
    }
}
