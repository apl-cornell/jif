package jif.types.label;

import polyglot.ast.Id;

public interface RifVarLabel extends VarLabel {

    RifVarLabel takeTransition(Id transition);

}
