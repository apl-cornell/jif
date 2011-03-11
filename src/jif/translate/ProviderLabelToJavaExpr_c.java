package jif.translate;

import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ProviderLabelToJavaExpr_c extends LabelToJavaExpr_c {

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw)
            throws SemanticException {
        // In Jif, all code is public and trusted.
        return label.typeSystem().bottomLabel().toJava(rw);
    }

}
