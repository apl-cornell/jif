package jif.ast;

import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class AmbProviderLabelNode_c extends AmbLabelNode_c
        implements AmbProviderLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    TypeNode typeNode;

//    @Deprecated
    public AmbProviderLabelNode_c(Position pos, TypeNode typeNode) {
        this(pos, typeNode, null);
    }

    public AmbProviderLabelNode_c(Position pos, TypeNode typeNode, Ext ext) {
        super(pos, ext);
        this.typeNode = typeNode;
    }

    protected <N extends AmbProviderLabelNode_c> N typeNode(N n,
            TypeNode typeNode) {
        if (n.typeNode == typeNode) return n;
        n = copyIfNeeded(n);
        n.typeNode = typeNode;
        return n;
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (!typeNode.isDisambiguated()) {
            return this;
        }

        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        Type type = typeNode.type();
        if (!type.isClass()) {
            throw new SemanticException(
                    "Provider label expressions can only "
                            + "be qualified with class types.",
                    typeNode.position());
        }

        Label providerLabel =
                ts.providerLabel(typeNode.position(), (JifClassType) type);
        return nf.CanonicalLabelNode(position, providerLabel);
    }

    @Override
    public String toString() {
        return "*" + typeNode + ".provider{amb}";
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("*");
        typeNode.del().prettyPrint(w, tr);
        w.write(".provider");
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        TypeNode typeNode = visitChild(this.typeNode, v);
        return reconstruct(this, typeNode);
    }

    protected <N extends AmbProviderLabelNode_c> N reconstruct(N n,
            TypeNode typeNode) {
        n = typeNode(n, typeNode);
        return n;
    }
}
