package jif.types;

import java.util.HashMap;
import java.util.List;

import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Id;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface RifFSMstate {

    void setTransition(String transName, RifFSMstate rstate);

    RifFSMstate getNextState(Id action);

    boolean equalsFSM(RifFSMstate other);

    List<Principal> principals();

    Id name();

    boolean leqFSM(RifFSMstate other);

    boolean isCanonical();

    boolean isRuntimeRepresentable();

    List<Type> throwTypes(TypeSystem ts);

    boolean isBottomConfidentiality();

    boolean isTopConfidentiality();

    String toString(boolean current);

    HashMap<String, RifFSMstate> getTransitions();

    List<Principal> subst(LabelSubstitution substitution)
            throws SemanticException;

    PathMap labelCheck(JifContext A, LabelChecker lc);

}
