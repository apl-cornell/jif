package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

import polyglot.util.InternalCompilerError;

import java.util.*;

public class LabelIfToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        LabelIf n = (LabelIf) node();
        return rw.bypass(n.lhs()).bypass(n.rhs());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        TypeSystem ts = rw.java_ts();
        NodeFactory nf = rw.java_nf();

        LabelIf n = (LabelIf) node();

        // Now visit the principals.
        Expr lhs = (Expr) n.visitChild(n.lhs(), rw);
        Expr rhs = (Expr) n.visitChild(n.rhs(), rw);

        Stmt consequent = n.consequent();
        Stmt alternative = n.alternative();

        if (alternative != null) {
            return rw.qq().parseStmt(
                "if ((%E).relabelsTo(%E)) {" +
                "   %S                                          " +
                "} else {                                       " +
                "   %S                                          " +
                "}                                              ",
                lhs, rhs, consequent, alternative);
        }
        else {
            return rw.qq().parseStmt(
                "if ((%E).relabelsTo(%E)) {" +
                "   %S                                          " +
                "}                                              ",
                lhs, rhs, consequent);
        }
    }
}
