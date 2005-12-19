package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class PrincipalExprToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        PrincipalExpr n = (PrincipalExpr) node();
        return rw.bypass(n.principal());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        PrincipalExpr n = (PrincipalExpr) node();
        return n.visitChild(n.principal(), rw);
    }
}
