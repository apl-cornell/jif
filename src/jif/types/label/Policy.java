package jif.types.label;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jif.translate.JifToJavaRewriter;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;

/**
 * This class represents the Jif security label.
 */
public interface Policy extends Label {
    boolean isBottomConfidentiality();
    boolean isTopConfidentiality();
    boolean isBottomIntegrity();
    boolean isTopIntegrity();
}