package jif.translate;

import jif.ast.LabelIf;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.NodeVisitor;

public class LabelIfToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        LabelIf n = (LabelIf) node();
        return rw.bypass(n.lhs()).bypass(n.rhs());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        LabelIf n = (LabelIf) node();

        // Now visit the principals.
        Expr lhs = (Expr) n.visitChild(n.lhs(), rw);
        Expr rhs = (Expr) n.visitChild(n.rhs(), rw);

        Stmt consequent = n.consequent();
        Stmt alternative = n.alternative();

        if (alternative != null) {
            return rw.qq().parseStmt(
                "if (jif.lang.LabelUtil.relabelsTo(%E, %E)) {" +
                "   %S                                          " +
                "} else {                                       " +
                "   %S                                          " +
                "}                                              ",
                lhs, rhs, consequent, alternative);
        }
        else {
            return rw.qq().parseStmt(
                "if (jif.lang.LabelUtil.relabelsTo(%E, %E)) {" +
                "   %S                                          " +
                "}                                              ",
                lhs, rhs, consequent);
        }
    }
}
