package jif.types.label;

import polyglot.ast.Id;

public interface RifDynamicLabel extends Label {

    Id getName();

    Label getLabel();

}
