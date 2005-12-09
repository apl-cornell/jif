package jif.translate;

import jif.types.label.Label;
import jif.types.label.LabelJ;
import jif.types.label.LabelM;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

public abstract class LabelToJavaExpr_c implements LabelToJavaExpr {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError("Should never be called");
    }
    public Expr toJava(LabelJ label, JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError("Should never be called");
    }
    public Expr toJava(LabelM label, JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError("Should never be called: " + label.getClass());
    }
}
