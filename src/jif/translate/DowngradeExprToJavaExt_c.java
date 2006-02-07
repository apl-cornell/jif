package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class DowngradeExprToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        DeclassifyExpr n = (DeclassifyExpr) node();
        return rw.bypass(n.bound()).bypass(n.label());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        DeclassifyExpr n = (DeclassifyExpr) node();
        return n.expr();
    }
}
