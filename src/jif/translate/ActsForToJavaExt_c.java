package jif.translate;

import jif.ast.ActsFor;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.visit.NodeVisitor;

public class ActsForToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        ActsFor n = (ActsFor) node();
        return rw.bypass(n.actor()).bypass(n.granter());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ActsFor n = (ActsFor) node();

        // Now visit the principals.
        Expr actor = (Expr) n.visitChild(n.actor(), rw);
        Expr granter = (Expr) n.visitChild(n.granter(), rw);

        Stmt consequent = n.consequent();
        Stmt alternative = n.alternative();

        if (alternative != null) {
            return rw.qq().parseStmt(
                "if (jif.lang.PrincipalUtil.actsFor((%E), (%E))) {" +
                "   %S                                          " +
                "} else {                                       " +
                "   %S                                          " +
                "}                                              ",
                actor, granter, consequent, alternative);
        }
        else {
            return rw.qq().parseStmt(
                "if (jif.lang.PrincipalUtil.actsFor((%E), (%E))) {" +
                "   %S                                          " +
                "}                                              ",
                actor, granter, consequent);
        }
    }
}
