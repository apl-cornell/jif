package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class LocalToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Local n = (Local) node();
        n = (Local) super.exprToJava(rw);
        n = n.localInstance(null);
        return n;
    }
}
