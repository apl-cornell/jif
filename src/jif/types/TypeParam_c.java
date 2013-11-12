package jif.types;

import jif.translate.JifToJavaRewriter;
import jif.translate.TypeParamToJavaExpr;
import jif.translate.TypeParamToJavaExpr_c;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class TypeParam_c extends Param_c implements TypeParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

    Type type;
    TypeParamToJavaExpr toJava;

    public TypeParam_c(Position pos, Type t) {
        super(t.typeSystem(), pos);
        this.type = t;
        toJava = new TypeParamToJavaExpr_c();
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return type.isCanonical();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava(this, rw);
    }
}
