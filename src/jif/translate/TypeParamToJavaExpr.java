package jif.translate;

import java.io.Serializable;

import jif.types.TypeParam;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface TypeParamToJavaExpr extends Serializable {
    public Expr toJava(TypeParam typeParam, JifToJavaRewriter rw)
            throws SemanticException;
}
