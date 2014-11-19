package jif.types;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jif.visit.LabelChecker;
import polyglot.ast.Id;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface RifFSM extends Serializable {

    RifFSMstate currentState();

    RifFSM takeTransition(Id action);

    boolean equalsFSM(RifFSM other, List<String> visited);

    boolean leqFSM(RifFSM other, List<String> visited);

    boolean isCanonical();

    boolean isRuntimeRepresentable();

    List<Type> throwTypes(TypeSystem ts);

    boolean isBottomConfidentiality();

    boolean isTopConfidentiality();

    @Override
    String toString();

    RifFSM subst(LabelSubstitution substitution) throws SemanticException;

    PathMap labelCheck(JifContext A, LabelChecker lc);

    Map<String, RifFSMstate> states();

    boolean isBottom(List<String> newvisited);

    boolean isTop(List<String> visited);

}
