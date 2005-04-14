package jif.types.principal;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
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

    /**
     * If the principal is runtime representable, when it is evaluated at
     * runtime it may throw exceptions. This method returns a list of
     * the exceptions that the runtime evaluation of the principal may produce.
     * If the principal cannot be evaluated at runtime, an empty list should be returned.  
     */
    List throwTypes(TypeSystem ts);
}
