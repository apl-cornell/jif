package jif.translate;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ActsForConstraintToJavaExpr_c implements
        ActsForConstraintToJavaExpr {
    @Override
    public <Actor extends ActsForParam, Granter extends ActsForParam> Expr toJava(
            ActsForConstraint<Actor, Granter> actsFor, JifToJavaRewriter rw)
            throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Expr actor = actsFor.actor().toJava(rw);
        Expr granter = actsFor.granter().toJava(rw);
        
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
