package jif.types.label;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jif.translate.JifToJavaRewriter;
import jif.types.JifContext;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

/**
 * This class represents a meetable part of a label.
 */
public interface LabelM extends TypeObject {
    /**
     * Is this label equivalent to bottom?
     * <p>
     * For example, a MeetLabelM with two components, one of which is Bottom, would
     * return true for this method.
     */
    boolean isBottom();

    /**
     * Is this label equivalent to top?
     * <p>
     * For example, a MeetLabelM with no components would return true for this
     * method.
     */
    boolean isTop();

    /**
     * @param labelSubst The <code>LabelSubstitution</code> to apply to this
     *            label
     * @return the result of applying labelSubst to this label.
     * @throws SemanticException
     */
    LabelM subst(LabelSubstitution labelSubst) throws SemanticException;

    /**
     * Label check the label, which will determine how much information may be
     * gained if the label is evaluated at runtime. For example, given the
     * dynamic label {*lb}, where lb is a local variable, evaluation of this
     * label at runtime will reveal as much information as the label of lb. For
     * example, the following code is illegal, as the runtime evaluation of the
     * label reveals too much information
     * 
     * <pre>
     * 
     *  boolean{Alice:} secret = ...;
     *  final label{Alice:} lb = secret?new label{}:new label{Bob:};
     *  boolean{} leak = false;
     *  if ((*lb} &lt;= new label{}) { // evaluation of lb reveals 
     *                           // information at level {Alice:} 
     *     leak = true;
     *  } 
     *  
     * </pre>
     * 
     * @see jif.ast.Jif#labelCheck(LabelChecker)
     * @see jif.types.principal.Principal#labelCheck(JifContext)
     */
    PathMap labelCheck(JifContext A, LabelChecker lc);

    /**
     * Are the components of this label all disambiguated?
     */
    boolean isDisambiguated();

    /**
     * Implementation of leq, should only be called by LabelEnv
     * 
     * @param L the label to determine if this label is leq to. This label
     *            always satisfies !this.equals(L)
     * @param H the label environment (including principal hierarchy). Will
     *            always be non-null.
     */
    boolean leq_(LabelM L, LabelEnv H);

    boolean isRuntimeRepresentable();

    LabelM simplify();

    /**
     * If the label is runtime representable, when it is evaluated at runtime it
     * may throw exceptions. This method returns a list of the exceptions that
     * the runtime evaluation of the label may produce. If the label cannot be
     * evaluated at runtime, an empty list should be returned.
     */
    List throwTypes(TypeSystem ts);

    Expr toJava(JifToJavaRewriter rw) throws SemanticException;

    String componentString();

    String componentString(Set printedLabels);

    String toString(Set printedLabels);
}
