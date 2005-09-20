package jif.translate;

import java.util.ArrayList;
import java.util.List;

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

        String comparison = "jif.lang.PrincipalUtil.actsFor((%E), (%E))";
        List l = new ArrayList(5);
        l.add(actor);
        l.add(granter);
        if (n.kind() == ActsFor.EQUIV) {
            comparison += " && jif.lang.PrincipalUtil.actsFor((%E), (%E))";
            l.add(granter);             
            l.add(actor);
        }
        l.add(consequent);
        if (alternative != null) {
            l.add(alternative);
            return rw.qq().parseStmt(
                "if (" + comparison + ") {" +
                "   %S                                          " +
                "} else {                                       " +
                "   %S                                          " +
                "}                                              ",
                l);
        }
        else {
            return rw.qq().parseStmt(
                "if (" + comparison + ") {" +
                "   %S                                          " +
                "}                                              ",
                l);
        }
    }
}
