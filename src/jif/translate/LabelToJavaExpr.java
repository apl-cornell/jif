package jif.translate;

import java.io.Serializable;

import jif.types.label.Label;
import jif.types.label.LabelJ;
import jif.types.label.LabelM;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface LabelToJavaExpr extends Serializable {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException;
    public Expr toJava(LabelJ label, JifToJavaRewriter rw) throws SemanticException;
    public Expr toJava(LabelM label, JifToJavaRewriter rw) throws SemanticException;
}
