package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class FieldToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Field n = (Field) node();
        n = (Field) super.exprToJava(rw);
        n = n.fieldInstance(null);
        return n;
    }
}
