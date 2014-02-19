package jif.types;

import java.util.List;

import jif.types.principal.Principal;
import polyglot.ast.Id;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface RifFSMstate {

    void setTransition(Id transName, RifFSMstate rstate);

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

}
