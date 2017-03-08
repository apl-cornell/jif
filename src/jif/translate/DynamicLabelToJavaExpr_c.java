package jif.translate;

import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class DynamicLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier,
            boolean simplify) throws SemanticException {
        DynamicLabel L = (DynamicLabel) label;
        return rw.qq().parseExpr(L.path().exprString());
    }
}
