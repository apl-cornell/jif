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

public class ActsForToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        ActsFor n = (ActsFor) node();
        return rw.bypass(n.actor()).bypass(n.granter());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        TypeSystem ts = rw.java_ts();
        NodeFactory nf = rw.java_nf();

        ActsFor n = (ActsFor) node();

        // Now visit the principals.
        Expr actor = (Expr) n.visitChild(n.actor(), rw);
        Expr granter = (Expr) n.visitChild(n.granter(), rw);

        Stmt consequent = n.consequent();
        Stmt alternative = n.alternative();

        if (alternative != null) {
            return rw.qq().parseStmt(
                "if (jif.runtime.Runtime.acts_for((%E), (%E))) {" +
                "   %S                                          " +
                "} else {                                       " +
                "   %S                                          " +
                "}                                              ",
                actor, granter, consequent, alternative);
        }
        else {
            return rw.qq().parseStmt(
                "if (jif.runtime.Runtime.acts_for((%E), (%E))) {" +
                "   %S                                          " +
                "}                                              ",
                actor, granter, consequent);
        }
    }
}
