package jif.types.label;

import polyglot.types.LocalInstance;


/**
 * This label is used as a place-holder for method argument labels.
 * The purpose is to avoid having to re-interpret labels at each call.
 */
public interface ArgLabel extends Label {
    LocalInstance formalInstance();
    Label upperBound();
    void setUpperBound(Label upperBound);
}
