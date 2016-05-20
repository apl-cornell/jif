package jif.translate;

import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ProviderLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr qualifier)
            throws SemanticException {
        // In Jif, all code is public and trusted.
        return label.typeSystem().bottomLabel().toJava(rw, qualifier);
    }

}
