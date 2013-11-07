package jif.types;

import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

public class TypeParam_c extends Param_c implements TypeParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public Type type;

    public TypeParam_c(Type t) {
        this.type = t;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

}
