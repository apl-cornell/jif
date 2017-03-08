package jif.translate;

import java.io.Serializable;

import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface LabelToJavaExpr extends Serializable {
    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     */
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier)
            throws SemanticException;

    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     * @param simplify
     *          whether to attempt to simplify the label when it's constructed
     *          at run time.
     */
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr thisQualifier,
            boolean simplify) throws SemanticException;
}
