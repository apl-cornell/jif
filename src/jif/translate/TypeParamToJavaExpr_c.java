package jif.translate;

import jif.types.TypeParam;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class TypeParamToJavaExpr_c implements TypeParamToJavaExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(TypeParam typeParam, JifToJavaRewriter rw)
            throws SemanticException {
        // Here's where the magic happens?
        return null;
    }

}
