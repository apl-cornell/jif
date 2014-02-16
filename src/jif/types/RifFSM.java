package jif.types;

import java.util.List;

import polyglot.ast.Id;

public interface RifFSM {

    RifFSMstate currentState();

    RifFSM takeTransition(Id action);

    boolean equalsFSM(RifFSM other, List<String> visited);

    boolean leqFSM(RifFSM other, List<String> visited);

}
