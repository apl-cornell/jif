package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.visit.*;
import polyglot.visit.*;

public class ExprToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Expr e = (Expr) node();
        e = e.type(null);
        return exprToJava(rw);
    }

    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        return (Expr) super.toJava(rw);
    }
}
