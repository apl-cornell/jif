package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import polyglot.ast.Expr_c;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

public class PrincipalExpr_c extends Expr_c implements PrincipalExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected PrincipalNode principal;

//    @Deprecated
    public PrincipalExpr_c(Position pos, PrincipalNode principal) {
        this(pos, principal, null);
    }

    public PrincipalExpr_c(Position pos, PrincipalNode principal, Ext ext) {
        super(pos, ext);
        this.principal = principal;
    }

    @Override
    public PrincipalNode principal() {
        return this.principal;
    }

    @Override
    public PrincipalExpr principal(PrincipalNode principal) {
        PrincipalExpr_c n = (PrincipalExpr_c) copy();
        n.principal = principal;
        return n;
    }

    protected PrincipalExpr_c reconstruct(PrincipalNode principal) {
        if (principal != this.principal) {
            PrincipalExpr_c n = (PrincipalExpr_c) copy();
            n.principal = principal;
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        PrincipalNode principal = visitChild(this.principal, v);
        return reconstruct(principal);
    }

    @Override
    public Node typeCheck(TypeChecker tc) {
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        return type(ts.Principal());
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return principal().principal().throwTypes(ts);
    }

    @Override
    public Term firstChild() {
        return null;
    }

    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("new principal (");
        print(principal, w, tr);
        w.write(")");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    @Override
    public String toString() {
        return principal.toString();
    }
}
