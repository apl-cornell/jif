package jif.ast;

import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class AmbProviderLabelNode_c extends AmbLabelNode_c implements
        AmbProviderLabelNode {
    
    TypeNode typeNode;

    public AmbProviderLabelNode_c(Position pos, TypeNode typeNode) {
        super(pos);
        this.typeNode = typeNode;
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
            throw new SemanticException("Provider label expressions can only "
                    + "be qualified with class types.", typeNode.position());
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
        TypeNode typeNode = (TypeNode) visitChild(this.typeNode, v);
        return reconstruct(typeNode);
    }
    
    protected AmbProviderLabelNode_c reconstruct(TypeNode typeNode) {
        if (this.typeNode != typeNode) {
            AmbProviderLabelNode_c n = (AmbProviderLabelNode_c) copy();
            n.typeNode = typeNode;
            return n;
        }

        return this;
    }
}
