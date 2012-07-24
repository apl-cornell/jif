package jif.types.principal;

import jif.translate.JifToJavaRewriter;
import jif.types.JifTypeSystem;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>UnknownPrincipal</code> interface. 
 */
public class UnknownPrincipal_c extends Principal_c
                               implements UnknownPrincipal
{
    public UnknownPrincipal_c(JifTypeSystem ts, Position pos) {
	super(ts, pos);
    }

    @Override
    public boolean isRuntimeRepresentable() { return false; }
    @Override
    public boolean isCanonical() { return false; }

    @Override
    public String toString() { return "<unknown principal>"; }

    @Override
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
	throw new InternalCompilerError("Cannot translate an unknown principal.");
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return -572;
    }
}
