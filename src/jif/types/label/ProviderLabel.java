package jif.types.label;

import polyglot.util.Position;
import jif.types.JifClassType;

/**
 * The label on a class, representing the trustworthiness of the class itself.
 */
public interface ProviderLabel extends Label {
    ProviderLabel position(Position pos);
    /**
     * @return the class type labelled by this label.
     */
    JifClassType classType();
}
