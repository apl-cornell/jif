package jif.ast;

import java.util.Set;

import jif.types.Assertion;
import polyglot.ast.Node;

/**
 * The root of various constraint nodes. This is the AST representation of an
 * <tt>Assertion</tt>.
 */
public interface ConstraintNode<Constraint extends Assertion> extends Node {
    Set<Constraint> constraints();

    ConstraintNode<Constraint> constraints(Set<Constraint> constraint);
}
