package jif.translate;

import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class LocalToJavaExt_c extends ExprToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        Local n = (Local) node();
        n = rw.java_nf().Local(n.position(), n.id());
        n = n.localInstance(null);
        return n;
    }
}
