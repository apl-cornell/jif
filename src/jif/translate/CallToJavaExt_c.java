package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class CallToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Call n = (Call) node();
        n = (Call) super.exprToJava(rw);
        n = n.methodInstance(null);
        return n;
    }
}
