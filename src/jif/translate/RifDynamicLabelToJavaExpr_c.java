package jif.translate;

import jif.types.label.Label;
import jif.types.label.RifDynamicLabel;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifDynamicLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw)
            throws SemanticException {
        RifDynamicLabel L = (RifDynamicLabel) label;
        Expr e = L.getLabel().toJava(rw);
        return (Expr) rw
                .qq()
                .parseExpr(
                        rw.runtimeLabelUtil() + ".taketransition(" + "\""
                                + L.getName().id() + "\"" + ",%E)", e)
                .position(Position.compilerGenerated());
    }
}
