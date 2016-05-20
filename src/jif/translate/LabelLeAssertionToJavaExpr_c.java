package jif.translate;

import jif.types.LabelLeAssertion;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class LabelLeAssertionToJavaExpr_c
        implements LabelLeAssertionToJavaExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(LabelLeAssertion lla, JifToJavaRewriter rw)
            throws SemanticException {
        Expr qualifier = rw.qq().parseExpr("this");
        Expr left = lla.lhs().toJava(rw, qualifier);
        Expr right = lla.rhs().toJava(rw, qualifier);
        String comparison = rw.runtimeLabelUtil() + ".relabelsTo((%E), (%E))";
        return rw.qq().parseExpr(comparison, left, right);
    }

}
