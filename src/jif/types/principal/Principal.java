package jif.types.principal;

import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import jif.translate.JifToJavaRewriter;
import jif.types.LabelSubstitution;
import jif.types.Param;
import jif.types.label.*;

/** The root interface of all kinds of Jif principals. 
 */
public interface Principal extends Param {
    public Principal subst(AccessPathRoot r, AccessPath e);

    /**
     * @param substitution
     * @return
     * @throws SemanticException
     */
    Principal subst(LabelSubstitution substitution) throws SemanticException;
    Expr toJava(JifToJavaRewriter rw) throws SemanticException;
}
