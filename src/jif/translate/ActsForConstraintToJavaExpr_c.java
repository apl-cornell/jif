package jif.translate;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ActsForConstraintToJavaExpr_c
        implements ActsForConstraintToJavaExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public <Actor extends ActsForParam, Granter extends ActsForParam> Expr toJava(
            ActsForConstraint<Actor, Granter> actsFor, JifToJavaRewriter rw)
            throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Expr thisQualifier = rw.qq().parseExpr("this");
        Expr actor = actsFor.actor().toJava(rw, thisQualifier);
        Expr granter = actsFor.granter().toJava(rw, thisQualifier);

        String className;
        if (actsFor.actor() instanceof Label) {
            className = rw.runtimeLabelUtil();
        } else {
            className = ts.PrincipalUtilClassName();
        }

        String meth;
        if (actsFor.isEquiv()) {
            meth = "equivalentTo";
        } else {
            meth = "actsFor";
        }

        String comparison = className + "." + meth + "((%E), (%E))";
        return rw.qq().parseExpr(comparison, actor, granter);
    }

}
