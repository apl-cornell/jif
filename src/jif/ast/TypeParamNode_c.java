package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.TypeParam;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.ast.Term_c;
import polyglot.ast.TypeNode;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class TypeParamNode_c extends Term_c implements TypeParamNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    TypeNode typeNode;
    TypeParam typeParam;

    public TypeParamNode_c(Position pos, TypeNode tn) {
        super(pos);
        typeNode = tn;
        if (typeNode.isDisambiguated()) {
            JifTypeSystem jts = (JifTypeSystem) typeNode.type().typeSystem();
            typeParam = jts.typeParam(typeNode.type());
        }
    }

    @Override
    public Param parameter() {
        return typeParam;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(typeParam.toString());
    }

    @Override
    public TypeParamNode parameter(TypeParam param) {
        TypeParamNode_c newNode = (TypeParamNode_c) copy();
        newNode.typeParam = param;
        return newNode;
    }

    @Override
    public Term firstChild() {
        return typeNode;
    }

    @Override
    public TypeNode typeNode() {
        return typeNode;
    }

    @Override
    public TypeParamNode typeNode(TypeNode type) {
        if (type == typeNode) return this;

        TypeParamNode_c newNode = (TypeParamNode_c) copy();
        newNode.typeNode = type;
        if (type.isDisambiguated()) {
            JifTypeSystem jts = (JifTypeSystem) type.type().typeSystem();
            newNode.typeParam = jts.typeParam(type.type());
        }
        return newNode;
    }

    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        TypeNode tn = (TypeNode) visitChild(typeNode, v);
        return typeNode(tn);
    }

    @Override
    public String toString() {
        return typeNode.toString();
    }
}
