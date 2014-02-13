package jif.ast;

import java.util.List;

import jif.types.RifState;
import polyglot.ast.Id;

public interface RifStateNode extends RifComponentNode {

    public Id name();

    public List<PrincipalNode> principals();

    public RifState state();

}
