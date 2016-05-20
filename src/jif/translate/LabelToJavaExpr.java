package jif.translate;

import java.io.Serializable;

import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface LabelToJavaExpr extends Serializable {
    /**
     * @param qualifier
     *          an Expr with which to qualify all accesses to label params and
     *          principal params.
     */
    public Expr toJava(Label label, JifToJavaRewriter rw, Expr qualifier)
            throws SemanticException;
}
