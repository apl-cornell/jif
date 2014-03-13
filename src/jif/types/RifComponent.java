package jif.types;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface RifComponent extends Serializable {

    boolean isCanonical();

    boolean isRuntimeRepresentable();

    String toString(Set<Label> printedLabels);

    List<Type> throwTypes(TypeSystem ts);

    RifComponent subst(LabelSubstitution substitution) throws SemanticException;

    PathMap labelCheck(JifContext A, LabelChecker lc);

}
