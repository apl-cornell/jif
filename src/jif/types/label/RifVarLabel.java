package jif.types.label;

import polyglot.ast.Id;

public interface RifVarLabel extends VarLabel {

    void takeTransition(Id transition);

    Id transition();

}
