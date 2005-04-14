package jif.types.label;

import jif.types.JifClassType;


/**
 * This label is used as a place-holder for the this label. It can be regarded
 * as being similar to an ArgLabel, as it is substituted away at every field access
 * and method call.
 */
public interface ThisLabel extends Label {
    JifClassType classType();
}
