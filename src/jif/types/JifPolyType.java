package jif.types;

import java.util.List;

import polyglot.ext.param.types.InstType;

/** Jif polymorphic type. 
 */
public interface JifPolyType extends JifClassType , InstType {
    /**
     * Declared parameters of the class.  Same as actuals() in
     * InstType, but kept around for historical reasons.
     */
    List params();
    
}
