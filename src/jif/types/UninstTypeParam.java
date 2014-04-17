package jif.types;

import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface UninstTypeParam extends Type {

    ParamInstance paramInstance();

    ReferenceType upperBound();

    UninstTypeParam upperBound(ReferenceType upperBound);

}
