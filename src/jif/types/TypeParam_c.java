package jif.types;

import polyglot.ast.TypeNode;
import polyglot.util.SerialVersionUID;

public class TypeParam_c extends Param_c implements TypeParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public TypeNode tn;

    public TypeParam_c(TypeNode tn) {
        this.tn = tn;
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
