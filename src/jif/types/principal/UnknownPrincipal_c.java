package jif.types.principal;

import jif.translate.JifToJavaRewriter;
import jif.types.JifTypeSystem;
import polyglot.ast.Expr;
import polyglot.ext.jl.types.TypeObject_c;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>UnknownPrincipal</code> interface. 
 */
public class UnknownPrincipal_c extends TypeObject_c
                               implements UnknownPrincipal
{
    public UnknownPrincipal_c(JifTypeSystem ts, Position pos) {
	super(ts, pos);
    }

    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCanonical() { return false; }

    public String translate(Resolver c) {
	throw new InternalCompilerError("Cannot translate an unknown label.");
    }

    public String toString() { return "<unknown principal>"; }

    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
	throw new InternalCompilerError("Cannot translate an unknown label.");
    }
}
