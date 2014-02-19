package jif.types;

import java.util.List;

import polyglot.ast.Id;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface RifFSM {

    RifFSMstate currentState();

    RifFSM takeTransition(Id action);

    boolean equalsFSM(RifFSM other, List<String> visited);

    boolean leqFSM(RifFSM other, List<String> visited);

    boolean isCanonical(List<String> visited);

    boolean isRuntimeRepresentable(List<String> visited);

    List<Type> throwTypes(TypeSystem ts, List<String> visited);

    boolean isBottomConfidentiality(List<String> visited);

    boolean isTopConfidentiality(List<String> visited);

    String toString(List<String> visited);

}
