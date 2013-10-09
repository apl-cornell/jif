package jif.types;

import java.util.List;

import jif.types.principal.Principal;
import polyglot.ext.param.types.PClass;
import polyglot.types.ParsedClassType;

/** Jif parsed polymorphic class type.
 */
public interface JifParsedPolyType extends ParsedClassType, JifPolyType {
    void setParams(List<ParamInstance> params);

    void setAuthority(List<Principal> principals);

    void setConstraints(List<Assertion> constraints);

    void setInstantiatedFrom(PClass<ParamInstance, Param> pc);
}
