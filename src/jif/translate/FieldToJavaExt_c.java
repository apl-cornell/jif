package jif.translate;

import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.types.SemanticException;

public class FieldToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Field n = (Field) node();
        n = (Field) super.exprToJava(rw);
        n = n.fieldInstance(null);
        return n;
    }
}
