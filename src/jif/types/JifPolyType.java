package jif.types;

import java.util.List;

import polyglot.ext.param.types.InstType;

/**
 * Jif polymorphic type.
 */
public interface JifPolyType
        extends JifClassType, InstType<ParamInstance, Param> {
    /**
     * Declared parameters of the class. Returns a list of
     * <code>ParamInstance</code>s.
     * 
     * @see ParamInstance
     */
    List<ParamInstance> params();
}
