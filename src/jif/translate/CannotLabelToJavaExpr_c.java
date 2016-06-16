package jif.translate;

import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;

public class CannotLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label L, JifToJavaRewriter rw, Expr thisQualifier,
            boolean simplify) throws SemanticException {
        throw new InternalCompilerError(L.position(),
                "Cannot translate " + L + " to Java.");
    }
}
