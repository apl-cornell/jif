package jif.types.principal;

import jif.translate.JifToJavaRewriter;
import polyglot.ast.Expr;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;

/** The root interface of all kinds of Jif principals. 
 */
public interface PrincipalImpl extends Principal {
    String translate(Resolver c);
    Expr toJava(JifToJavaRewriter rw) throws SemanticException;
}
