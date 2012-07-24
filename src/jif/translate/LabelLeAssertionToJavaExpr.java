package jif.translate;

import java.io.Serializable;

import jif.types.LabelLeAssertion;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface LabelLeAssertionToJavaExpr extends Serializable {
    Expr toJava(LabelLeAssertion lla, JifToJavaRewriter rw)
            throws SemanticException;
}
