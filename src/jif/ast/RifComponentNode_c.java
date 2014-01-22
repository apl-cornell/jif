package jif.ast;

import polyglot.ast.Node_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifComponentNode_c extends Node_c implements RifComponentNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public RifComponentNode_c(Position pos) {
        super(pos);
    }

}
