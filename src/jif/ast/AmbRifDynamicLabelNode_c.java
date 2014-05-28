package jif.ast;

import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class AmbRifDynamicLabelNode_c extends AmbLabelNode_c implements
        AmbRifDynamicLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id name;
    private LabelNode label;

    public AmbRifDynamicLabelNode_c(Position pos, Id name, LabelNode label) {
        super(pos);
        this.label = label;
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name.toString() + this.label.toString();
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(this.name.toString());
        w.write("(");
        this.label.prettyPrint(w, tr);
        w.write(")");
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode label = (LabelNode_c) this.label.visitChildren(v);
        return reconstruct(label);
    }

    protected AmbRifDynamicLabelNode_c reconstruct(LabelNode label) {
        if (this.label != label) {
            AmbRifDynamicLabelNode_c n = (AmbRifDynamicLabelNode_c) copy();
            n.label = label;
            return n;
        }
        return this;
    }

    /** Disambiguate the type of this node. */
    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        LabelNode ln = this.label;
        if (!ln.isDisambiguated()) {
            ln = (LabelNode) this.label.del().disambiguate(sc);
            if (!ln.isDisambiguated()) {
                sc.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
        }
        Label L = ts.rifDynamicLabel(position(), this.name, ln.label());
        return nf.CanonicalLabelNode(position(), L);
    }
}
