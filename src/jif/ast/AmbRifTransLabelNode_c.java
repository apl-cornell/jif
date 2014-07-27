package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.ConfPolicy;
import jif.types.label.IntegPolicy;
import jif.types.label.RifConfPolicy;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class AmbRifTransLabelNode_c extends LabelNode_c implements RifLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Id name;
    protected LabelNode rifPolicyNode;

    public AmbRifTransLabelNode_c(Position pos, LabelNode rifPolicyNode, Id name) {
        super(pos);
        this.name = name;
        this.rifPolicyNode = rifPolicyNode;
    }

    @Override
    public List<RifPolicyNode> policies() {
        return null;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode n = (LabelNode) visitChild(this.rifPolicyNode, v);
        return reconstruct(n);
    }

    protected AmbRifTransLabelNode_c reconstruct(LabelNode n) {

        if (n != rifPolicyNode) {
            AmbRifTransLabelNode_c rnode = (AmbRifTransLabelNode_c) copy();
            rnode.rifPolicyNode = n;
            rnode.name = name;
            return rnode;
        }

        return this;
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        if (!rifPolicyNode.isDisambiguated()) {
            sc.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }
        ConfPolicy cp = rifPolicyNode.label().confProjection();
        IntegPolicy ip = rifPolicyNode.label().integProjection();
        ConfPolicy ncp = ((RifConfPolicy) cp).takeTransition(name);
        return nf.CanonicalLabelNode(position, ts.pairLabel(position, ncp, ip));
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("tc(");
        rifPolicyNode.prettyPrint(w, tr);
        w.write(", " + name.id() + ")");
    }

}
