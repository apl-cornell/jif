package jif.ast;

import jif.types.label.Policy;
import polyglot.ast.Node_c;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class PolicyNode_c extends Node_c implements PolicyNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected PrincipalNode owner;
    protected Policy policy = null;

    public PolicyNode_c(Position pos, Policy policy) {
        super(pos);
        this.policy = policy;
        this.owner = null;
    }

    public PolicyNode_c(Position pos, PrincipalNode owner) {
        super(pos);
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
    }

    @Override
    public Policy policy() {
        return this.policy;
    }

    @Override
    public PrincipalNode owner() {
        return this.owner;
    }

    @Override
    public PolicyNode owner(PrincipalNode owner) {
        PolicyNode_c n = (PolicyNode_c) copy();
        n.owner = owner;
        return n;
    }

    @Override
    public boolean isDisambiguated() {
        return policy != null;
    }

}
