package jif.translate;

import java.util.ListIterator;

import jif.ast.*;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.Label;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.util.UniqueID;
import polyglot.visit.NodeVisitor;

public class SwitchLabelToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        SwitchLabel n = (SwitchLabel) node();
        return rw.bypassChildren(n);
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
	JifTypeSystem jif_ts = (JifTypeSystem) rw.jif_ts();

        SwitchLabel n = (SwitchLabel) node();

        // Get the runtime label of the expression.
        PathMap Xe = Jif_c.X(n.expr());
        Label L = Xe.NV();
        //@@@L = L.meet(jif_ts.runtimeLabel(), n.ph());

        Expr el1 = rw.labelToJava(L);

        // Rewrite the expression type first, since rewriting the expression
        // may munge it.
        TypeNode expr_type = rw.jif_nf().CanonicalTypeNode(n.expr().position(),
                                                           n.expr().type());
        expr_type = (TypeNode) n.visitChild(expr_type, rw);

        // Now rewrite the expression.
        Expr expr = (Expr) n.visitChild(n.expr(), rw);

        String tmp_var = UniqueID.newID("tmp");

        Stmt s = rw.java_nf().Empty(n.position());

	// Check each case in a big if-else statement.
        // Go backward through the cases to build up the if statement.
        for (ListIterator i = n.cases().listIterator(n.cases().size());
             i.hasPrevious(); ) {
	    LabelCase p = (LabelCase) i.previous();

            Stmt body = (Stmt) p.visitChild(p.body(), rw);

	    if (p.isDefault()) {
                // eval expr for side effects
                s = rw.qq().parseStmt("{ final %T %s = %E; %S }",
                                      expr_type, tmp_var, expr, body);
	    }
	    else {
		Label Li = p.label().label();
		//@@@@@Li = Li.meet(jif_ts.runtimeLabel(), n.ph());
                Expr el2 = rw.labelToJava(Li);

		if (p.decl() != null) {
		    // Generate an assignment as if initializing with the
		    // expression.
                    Stmt pdecl = (Stmt) rw.jif_nf().LocalDecl(p.decl().position(), p.decl().flags(), p.decl().type(), p.decl().name(), n.expr()).visit(rw);

                    s = rw.qq().parseStmt(
                                  " if ((%E).relabelsTo(%E)) {" +
                                  "     %S                    " +
                                  "     %S                    " +
                                  " } else {                  " +
                                  "     %S                    " +
                                  " }                         ",
                                  el1, el2, pdecl, body, s);
		}
		else {
                    s = rw.qq().parseStmt(
                                  " if ((%E).relabelsTo(%E)) { " +
                                       // eval expr for side-effects
                                  "    final %T %s = %E;       " +
                                  "    %S                      " +
                                  " } else {                   " +
                                  "    %S                      " +
                                  " }                          ",
                                  el1, el2, expr_type, tmp_var, expr, body, s);
		}
	    }
	}

        return s;
    }
}
