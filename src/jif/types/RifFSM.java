package jif.types;

import polyglot.ast.Id;

public interface RifFSM {

    RifFSMstate currentState();

    RifFSM takeTransition(Id action);

}
