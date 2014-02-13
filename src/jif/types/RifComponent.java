package jif.types;

import java.util.List;
import java.util.Set;

import jif.types.label.Label;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface RifComponent {

    boolean isCanonical();

    boolean isRuntimeRepresentable();

    String toString(Set<Label> printedLabels);

    List<Type> throwTypes(TypeSystem ts);

    RifComponent subst(LabelSubstitution substitution) throws SemanticException;

}
