package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class BinaryToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Binary n = (Binary) node();
        Precedence precedence = n.precedence();
        n = (Binary) super.exprToJava(rw);
        n = n.precedence(precedence);
        return n;
    }
}
