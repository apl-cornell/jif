package jif.ast;

import java.util.Collections;
import java.util.Set;

import jif.types.Assertion;
import polyglot.ast.Ext;
import polyglot.ast.Node_c;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>ConstraintNode</code> interface.
 */
public class ConstraintNode_c<Constraint extends Assertion> extends Node_c
        implements ConstraintNode<Constraint> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Set<Constraint> constraints; // of Assertion

    @Deprecated
    public ConstraintNode_c(Position pos) {
        this(pos, null);
    }

    public ConstraintNode_c(Position pos, Ext ext) {
        super(pos, ext);
    }

    @Override
    public boolean isDisambiguated() {
        if (constraints == null) return false;
        for (Assertion a : constraints) {
            if (!a.isCanonical()) return false;
        }
        return super.isDisambiguated();
    }

    @Override
    public Set<Constraint> constraints() {
        return constraints;
    }

    @Override
    public ConstraintNode<Constraint> constraints(Set<Constraint> constraints) {
        ConstraintNode_c<Constraint> n = copy();
        n.constraints = constraints;
        return n;
    }

    protected Constraint constraint() {
        if (constraints == null) return null;
        if (constraints.size() > 1)
            throw new InternalCompilerError("Multiple constraints");
        return constraints.iterator().next();
    }

    protected ConstraintNode<Constraint> constraint(Constraint constraint) {
        ConstraintNode_c<Constraint> n = copy();
        n.constraints = Collections.singleton(constraint);
        return n;
    }

    protected void setConstraint(Constraint constraint) {
        constraints = Collections.singleton(constraint);
    }

    /**
     * Bypass all children when performing an exception check. Constraints
     * aren't examined at runtime.
     */
    @Override
    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec)
            throws SemanticException {
        ec = (ExceptionChecker) super.exceptionCheckEnter(ec);
        return ec.bypassChildren(this);
    }

    @Override
    public ConstraintNode_c<Constraint> copy() {
        @SuppressWarnings("unchecked")
        ConstraintNode_c<Constraint> copy =
                (ConstraintNode_c<Constraint>) super.copy();
        return copy;
    }

    @Override
    public String toString() {
        if (constraints != null) {
            return constraints.toString();
        } else {
            return "<unknown-constraint>";
        }
    }
}
