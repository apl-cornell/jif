package jif.types.principal;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import jif.translate.JifToJavaRewriter;
import jif.types.*;
import jif.types.LabelSubstitution;
import jif.types.Param;
import jif.types.label.*;
import jif.visit.LabelChecker;

/** The root interface of all kinds of Jif principals. 
 */
public interface Principal extends Param {

    /**
     * @param substitution
     * @return
     * @throws SemanticException
     */
    Principal subst(LabelSubstitution substitution) throws SemanticException;

    /**
     * Label check the principal, which will determine how much information may be
     * gained if the principal is evaluated at runtime. For example, given the
     * dynamic principal p, where p is a local variable, evaluation of this
     * label at runtime will reveal as much information as the label of p. For
     * example, the following code is illegal, as the runtime evaluation of the
     * principal reveals too much information
     * <pre>
     * boolean{Alice:} secret = ...;
     * final principal{Alice:} p = secret?Bob:Chuck;
     * boolean{} leak = false;
     * if (p actsfor Bob) { // evaluation of p reveals
     *                      // information at level {Alice:}
     *     leak = true;
     * 	} 
     * </pre>
     * 
     * @see jif.ast.Jif#labelCheck(LabelChecker)
     * @see Label#labelCheck(JifContext)
     */
    PathMap labelCheck(JifContext A);

    Expr toJava(JifToJavaRewriter rw) throws SemanticException;

    /**
     * If the principal is runtime representable, when it is evaluated at
     * runtime it may throw exceptions. This method returns a list of
     * the exceptions that the runtime evaluation of the principal may produce.
     * If the principal cannot be evaluated at runtime, an empty list should be returned.  
     */
    List throwTypes(TypeSystem ts);
}
