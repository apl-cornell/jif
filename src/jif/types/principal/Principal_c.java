package jif.types.principal;

import jif.translate.*;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import polyglot.ast.Expr;
import polyglot.ext.jl.types.TypeObject_c;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** An abstract implementation of the <code>Principal</code> interface. 
 */
public abstract class Principal_c extends TypeObject_c implements Principal, PrincipalImpl {
    PrincipalToJavaExpr toJava;

    public Principal_c(JifTypeSystem ts, Position pos) {
        this(ts, pos, new CannotPrincipalToJavaExpr_c());
    }

    public Principal_c(JifTypeSystem ts, Position pos, PrincipalToJavaExpr toJava) {
	super(ts, pos);
        this.toJava = toJava;
    }

    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava(this, rw);
    }

    public abstract boolean isCanonical();
    public abstract boolean isRuntimeRepresentable();

    public Principal subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substPrincipal(this);
    }
}
