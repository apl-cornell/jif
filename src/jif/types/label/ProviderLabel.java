package jif.types.label;

import jif.types.JifClassType;

/**
 * The label on a class, representing the trustworthiness of the class itself.
 */
public interface ProviderLabel extends Label {
    /**
     * @return the class type labelled by this label.
     */
    JifClassType classType();
}
