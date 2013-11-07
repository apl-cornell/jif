package jif.ast;

import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import jif.types.Param;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.JL;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ast.TypeNode_c;
import polyglot.frontend.ExtensionInfo;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.AscriptionVisitor;
import polyglot.visit.ConstantChecker;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

public class TypeParamNode_c extends TypeNode_c implements TypeParamNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    TypeNode typeNode;

    public TypeParamNode_c(Position pos, TypeNode tn) {
        super(pos);
        this.typeNode = tn;
    }

    @Override
    public Param parameter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node del(JL del) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JL del() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node ext(Ext ext) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ext ext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node ext(int n, Ext ext) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ext ext(int n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Position position() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node position(Position position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean error() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Node error(boolean flag) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDisambiguated() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTypeChecked() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Node visit(NodeVisitor v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Node visitEdge(Node parent, NodeVisitor v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node visitChild(Node child, NodeVisitor v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Node> List<T> visitList(List<T> l, NodeVisitor v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dump(CodeWriter w) {
        // TODO Auto-generated method stub

    }

    @Override
    public Node node() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(Node node) {
        // TODO Auto-generated method stub

    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Context enterScope(Context c) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Context enterChildScope(Node child, Context c) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDecls(Context c) {
        // TODO Auto-generated method stub

    }

    @Override
    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node disambiguateOverride(Node parent, AmbiguityRemover ar)
            throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeVisitor disambiguateEnter(AmbiguityRemover ar)
            throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeVisitor typeCheckEnter(TypeChecker tc) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node typeCheckOverride(Node parent, TypeChecker tc)
            throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node checkConstants(ConstantChecker cc) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec)
            throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dump(OutputStream os) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dump(Writer w) {
        // TODO Auto-generated method stub

    }

    @Override
    public void prettyPrint(OutputStream os) {
        // TODO Auto-generated method stub

    }

    @Override
    public void prettyPrint(Writer w) {
        // TODO Auto-generated method stub

    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter pp) {
        // TODO Auto-generated method stub

    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        // TODO Auto-generated method stub

    }

    @Override
    public Node copy(NodeFactory nf) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node copy(ExtensionInfo extInfo) throws SemanticException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object copy() {
        // TODO Auto-generated method stub
        return null;
    }

}
