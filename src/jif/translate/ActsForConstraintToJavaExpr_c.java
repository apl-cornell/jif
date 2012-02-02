package jif.translate;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

public class ActsForConstraintToJavaExpr_c implements
        ActsForConstraintToJavaExpr {
    @Override
    public <Actor extends ActsForParam, Granter extends ActsForParam> Expr toJava(
            ActsForConstraint<Actor, Granter> actsFor, JifToJavaRewriter rw)
            throws SemanticException {

        // TODO: this method mirrors the logic in
        // JifBinaryDel.disambiguateRelations and other methods to translate
        // Binary expressions. Try to integrate the two.
        
        JifTypeSystem ts = rw.jif_ts();
        Expr actor = actsFor.actor().toJava(rw);
        Expr granter = actsFor.granter().toJava(rw);
        
        // we have
        // <actor> <rel> <granter>.
        //
        // rel                       may be equiv (eq) or ≽ (ge)
        // actor (a) and granter (g) may be labels (l) or principals (p)
        
        boolean eq = actsFor.isEquiv();
        boolean ge = !eq;
        
        boolean al = actsFor.actor() instanceof Label;
        boolean ap = !al;
        
        boolean gl = actsFor.actor() instanceof Label;
        boolean gp = !gl;
            
        // find the right method based on which case we're in.
        
        MethodInstance method;
        if      (al && eq && gl) // ℓ equiv ℓ
            method = ts.labelEquivMethod();
        else if (ap && eq && gp) // p equiv p
            method = ts.principalEquivMethod();
        else if (ap && ge && gp) // p ≽ p
            method = ts.actsForMethod();
        else if (ap && ge && gl) // p ≽ ℓ
            method = ts.enforcesMethod();
        else if (al && ge && gp) // ℓ ≽ p
            method = ts.authorizesMethod();
        else
            throw new InternalCompilerError("Trying to translate invalid constraint");

        // operator translates to a method call.
        StringBuilder call = new StringBuilder();
        call.append(method.container().toString());
        call.append(".");
        call.append(method.name());
        call.append("(%E,%E)");

        return rw.qq().parseExpr(call.toString(), actor, granter);
    }

}
