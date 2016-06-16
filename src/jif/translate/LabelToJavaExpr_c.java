package jif.translate;

import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;

public abstract class LabelToJavaExpr_c implements LabelToJavaExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier)
            throws SemanticException {
        return toJava(label, rw, thisQualifier, true);
    }

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier,
            boolean simplify) throws SemanticException {
        throw new InternalCompilerError("Should never be called: " + label
                + " :: " + label.getClass().getName());
    }
}
