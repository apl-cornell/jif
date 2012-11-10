package jif.translate;

import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class FieldToJavaExt_c extends ExprToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Field n = (Field) node();
        n = rw.java_nf().Field(n.position(), n.target(), n.id());
        return n;
    }
}
