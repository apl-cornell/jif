package jif.types;

import jif.types.label.Label;
import polyglot.types.*;

/** Jif field instance. A wrapper of all the type information related
 *  to a class field.
 */
public interface JifFieldInstance extends FieldInstance, JifVarInstance
{
    JifFieldInstance uid(UID uid);
    JifFieldInstance label(Label label);
    
    boolean hasInitializer();
    void setHasInitializer(boolean hasInitializer);
}
