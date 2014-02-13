package jif.types;

import java.util.List;

import jif.types.principal.Principal;
import polyglot.ast.Id;

public interface RifState extends RifComponent {
    public Id name();

    public List<Principal> principals();

    boolean isBottomConfidentiality();

    boolean isTopConfidentiality();

}
