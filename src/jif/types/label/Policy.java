package jif.types.label;

import java.util.List;
import java.util.Set;

import jif.types.JifContext;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

/**
 * This class is the common super class for integrity polices and
 * confidentiality policies.
 */
public interface Policy extends TypeObject {
    @Override
    boolean isCanonical();

    boolean isSingleton();

    boolean isRuntimeRepresentable();

    boolean isTop();

    boolean isBottom();

    boolean hasVariables();

    boolean hasWritersToReaders();

    List<Type> throwTypes(TypeSystem ts);

    Policy subst(LabelSubstitution substitution) throws SemanticException;

    Policy simplify();

    /**
     * @param printedLabels
     *        the set of labels for which auxiliary information should be
     *        omitted.
     */
    String toString(Set<Label> printedLabels);

    PathMap labelCheck(JifContext A, LabelChecker lc);
}
