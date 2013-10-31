package jif.types;

import jif.types.label.ProviderLabel;
import polyglot.types.FieldInstance;

/** Jif field instance. A wrapper of all the type information related
 *  to a class field.
 */
public interface JifFieldInstance extends FieldInstance, JifVarInstance {
    boolean hasInitializer();

    void setHasInitializer(boolean hasInitializer);

    Param initializer();

    void setInitializer(Param init);

    /**
     * @return the provider label of the class declaring this field.
     */
    ProviderLabel provider();
}
