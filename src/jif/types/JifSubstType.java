package jif.types;

import polyglot.ext.param.types.InstType;
import polyglot.ext.param.types.SubstType;

public interface JifSubstType extends JifClassType,
        SubstType<ParamInstance, Param>, InstType<ParamInstance, Param> {
}
