package jif.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jif.extension.LabelTypeCheckUtil;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.JoinLabel;
import jif.types.label.Label;
import jif.types.label.MeetLabel;
import jif.types.label.Variable;
import jif.types.label.WritersToReadersLabel;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * A <code>LabelConstraint</code> represents a constraint on labels, which
 * may either be an inequality or equality constraint.
 * <code>LabelConstraint</code>s are generated during type checking.
 * <code>LabelConstraint</code>s in turn produce {@link Equation Equations}
 * which are what the {@link Solver Solver} will use to find a satisfying
 * assignment for {@link Variable Variables}.
 */
public class LabelConstraint extends Constraint {
    /**
     * An equality kind of constraint. That is, the constraint requires that
     * lhs ⊑ rhs and rhs ⊑ lhs.
     */
    public static final Kind EQUAL = new Kind(" == ");

    /**
     * An inequality kind of constraint. That is, the constraint requires that
     * lhs ⊑ rhs.
     */
    public static final Kind LEQ = new Kind(" ⊑ ");

    /**
     * Names for the LHS
     */
    protected NamedLabel namedLHS;

    /**
     * Names for the RHS
     */
    protected NamedLabel namedRHS;

    public LabelConstraint(NamedLabel lhs, Kind kind, NamedLabel rhs,
            LabelEnv env, Position pos, ConstraintMessage msg, boolean report) {
        super(lhs.label(), kind, rhs.label(), env, pos, msg, report);
        this.namedLHS = lhs;
        this.namedRHS = rhs;
    }

    public Label lhsLabel() {
        return (Label) lhs;
    }

    public Label rhsLabel() {
        return (Label) rhs;
    }

    public NamedLabel namedLhs() {
        return namedLHS;
    }

    public NamedLabel namedRhs() {
        return namedRHS;
    }

    /**
     * Return a map from Strings to Labels, which are the named elements of
     * the left and right hand sides.
     */
    protected Map<String, Label> namedLabels() {
        Map<String, Label> ne = new LinkedHashMap<String, Label>();
        if (namedLHS != null) {
            ne.putAll(namedLHS.nameToLabels);
        }
        if (namedRHS != null) {
            ne.putAll(namedRHS.nameToLabels);
        }
        return ne;
    }

    /**
     * Return a map from Strings to Strings, which are the descriptions of
     * names in the left and right hand sides.
     */
    protected Map<String, String> namedDescrips() {
        Map<String, String> ne = new LinkedHashMap<String, String>();
        if (namedLHS != null) {
            ne.putAll(namedLHS.nameToDescrip);
        }
        if (namedRHS != null) {
            ne.putAll(namedRHS.nameToDescrip);
        }
        return ne;
    }

    /**
     * Returns a Map of Strings to List[String]s which is the definitions/bounds
     * of the NamedLabels, and the description of any components that
     * appear in the NamedLabels. This map is used for verbose output to the
     * user, to help explain the meaning of this constraint.
     */
    public Map<String, List<String>> definitions(VarMap bounds) {
        Map<String, List<String>> defns =
                new LinkedHashMap<String, List<String>>();

        Set<Label> labelComponents = new LinkedHashSet<Label>();
        Map<String, Label> namedLabels = this.namedLabels();
        Map<String, String> namedDescrips = this.namedDescrips();
        LabelTypeCheckUtil ltcu = lhs.typeSystem().labelTypeCheckUtil();

        for (String s : namedLabels.keySet()) {
            List<String> l = new ArrayList<String>(2);
            defns.put(s, l);

            if (namedDescrips.get(s) != null) {
                l.add(namedDescrips.get(s));
            }
            Label bound = bounds.applyTo(namedLabels.get(s));
            l.add(bound.toString());

            labelComponents.addAll(ltcu.labelComponents(bound));
        }

        // in case there are no named labels, add all components of the lhs and
        // rhs bounds.
        Label bound = bounds.applyTo(lhsLabel());
        labelComponents.addAll(ltcu.labelComponents(bound));

        bound = bounds.applyTo(rhsLabel());
        labelComponents.addAll(ltcu.labelComponents(bound));

        // get definitions for the label components.
        for (Label l : labelComponents) {
            if (l.description() != null) {
                String s = l.componentString();
                if (s.length() == 0) s = l.toString();
                List<String> list = new ArrayList<String>(2);
                list.add(l.description());
                defns.put(s, list);
                if (l instanceof WritersToReadersLabel) {
                    // add the transform of the writersToReaders label
                    list.add(env.triggerTransforms(l).toString());
                }
            }
        }

        defns.putAll(env.definitions(bounds, labelComponents));

        return defns;
    }

    /**
     * Produce a <code>Collection</code> of {@link Equation Equations} for this
     * constraint.
     */
    @Override
    public Collection<Equation> getEquations() {
        Collection<Equation> eqns = new LinkedList<Equation>();

        if (kind == LEQ) {
            addLEQEqns(eqns, lhsLabel(), rhsLabel());
        } else if (kind == EQUAL) {
            addLEQEqns(eqns, lhsLabel(), rhsLabel());
            addLEQEqns(eqns, rhsLabel(), lhsLabel());
        } else {
            throw new InternalCompilerError(
                    "Inappropriate kind of equation: " + kind);
        }

        return eqns;

    }

    /**
     * Produce equations that require <code>left</code> to be less than or
     * equal to <code>right</code>, and add them to <code>eqns</code>.
     */
    protected void addLEQEqns(Collection<Equation> eqns, Label left,
            Label right) {
        left = left.simplify();
        right = right.simplify();
        if (left instanceof JoinLabel) {
            for (Label jc : ((JoinLabel) left).joinComponents()) {
                addLEQEqns(eqns, jc, right);
            }
        } else if (right instanceof MeetLabel) {
            for (Label mc : ((MeetLabel) right).meetComponents()) {
                addLEQEqns(eqns, left, mc);
            }
        } else {
            Equation eqn = new LabelEquation(left, right, this);
            eqns.add(eqn);
        }
    }

    @Override
    public boolean hasVariables() {
        return lhsLabel().hasVariables() || rhsLabel().hasVariables();
    }
}
