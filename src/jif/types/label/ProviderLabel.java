package jif.types.label;

import jif.types.JifClassType;
import polyglot.util.Position;

/**
 * The label on a class, representing the trustworthiness of the class itself.
 */
public interface ProviderLabel extends Label {
    ProviderLabel position(Position pos);

    /**
     * @return the class type labelled by this label.
     */
    JifClassType classType();

    /**
     * @return whether this is a trusted provider.
     */
    boolean isTrusted();
}
