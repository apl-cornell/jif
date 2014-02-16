package jif.types;

import java.util.List;

import jif.types.principal.Principal;
import polyglot.ast.Id;

public interface RifFSMstate {

    void setTransition(Id transName, RifFSMstate rstate);

    RifFSMstate getNextState(Id action);

    boolean equalsFSM(RifFSMstate other);

    List<Principal> principals();

    Id name();

    boolean leqFSM(RifFSMstate other);

}
