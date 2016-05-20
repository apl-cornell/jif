package jif.types.principal;

import java.util.List;
import java.util.Set;

import jif.types.ActsForParam;
import jif.types.JifContext;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.types.label.Variable;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

/** The root interface of all kinds of Jif principals.
 */
public interface Principal extends ActsForParam {

    /**
     * @param labelSubst The <code>LabelSubstitution</code> to apply to this
     *            principal
     * @return the result of applying labelSubst to this principal.
     * @throws SemanticException
     */
    @Override
    Principal subst(LabelSubstitution labelSubst) throws SemanticException;

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
     * @see jif.ast.JifExt#labelCheck(LabelChecker)
     * @see Label#labelCheck(JifContext, LabelChecker)
     */
    PathMap labelCheck(JifContext A, LabelChecker lc);

    /**
     * If the principal is runtime representable, when it is evaluated at
     * runtime it may throw exceptions. This method returns a list of
     * the exceptions that the runtime evaluation of the principal may produce.
     * If the principal cannot be evaluated at runtime, an empty list should be returned.
     */
    List<Type> throwTypes(TypeSystem ts);

    /**
     * Does the label contain any variables at all? This includes variables
     * that are in bounds of arg labels.
     */
    boolean hasVariables();

    /**
     * The set of variables that this label contains including variables contained
     * in upper bounds of arg labels.
     */
    Set<Variable> variables();

    boolean isTopPrincipal();

    boolean isBottomPrincipal();

    /**
     * Simplify the label, using the actsfor relation if needed
     */
    @Override
    Principal simplify();
}
