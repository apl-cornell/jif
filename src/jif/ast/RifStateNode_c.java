/* new-begin */
package jif.ast;

import java.util.List;

import polyglot.ast.Id;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifStateNode_c extends RifComponentNode_c implements RifStateNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id name;
    private List<PrincipalNode> principals;

    public RifStateNode_c(Position pos, Id name, List<PrincipalNode> principals) {
        super(pos);
        this.name = name;
        this.principals = principals;
    }

    @Override
    public Id name() {
        return this.name;
    }

    @Override
    public List<PrincipalNode> principals() {
        return this.principals;
    }

}

/* new-end */
