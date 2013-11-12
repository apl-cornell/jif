package jif.types;

import jif.translate.JifToJavaRewriter;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.Type;

public interface TypeParam extends Param {
    Type type();

    Expr toJava(JifToJavaRewriter rw) throws SemanticException;
}
