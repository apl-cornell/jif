package jif.types;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.InformationFlowTrace.Direction;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.ConfPolicy;
import jif.types.label.IntegPolicy;
import jif.types.label.JoinLabel;
import jif.types.label.JoinPolicy_c;
import jif.types.label.Label;
import jif.types.label.MeetLabel;
import jif.types.label.MeetPolicy_c;
import jif.types.label.PairLabel;
import jif.types.label.VarLabel;
import jif.types.label.Variable;
import jif.types.principal.Principal;
import jif.types.principal.VarPrincipal;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * A solver of Jif constraints. Finds solution to constraints essentially by
 * propagating lower bounds forwards.
 * 
 */
public class SolverGLB extends AbstractSolver {
    /**
     * Constructor
     */
    public SolverGLB(JifTypeSystem ts, polyglot.frontend.Compiler compiler,
            String solverName) {
        super(ts, compiler, solverName);
    }

    /**
     * Constructor
     */
    protected SolverGLB(SolverGLB js) {
        super(js);
    }

    /**
     * This method adds the correct dependencies from Equation eqn to variables
     * occurring in eqn, and dependencies in the other direction (that is, from
     * variables occurring in eqn to eqn).
     * 
     * There is a dependency from Equation eqn to all variables that occur on
     * the RHS of eqn, as the bounds on these variables may be modified
     * (upwards) as a result of solving eqn.
     * 
     * There is a dependency from all variables on the LHS of eqn to eqn,
     * because modifying (upwards) the bounds on these variables may cause eqn
     * to no longer be satisfied.
     */
    @Override
    protected void addDependencies(Equation eqn) {
        // Build dependency maps for this equation.
        Set<Variable> changeable, awakeable;
        if (eqn instanceof LabelEquation) {
            changeable = ((LabelEquation) eqn).rhs().variableComponents();
            awakeable = ((LabelEquation) eqn).lhs().variableComponents();
        } else if (eqn instanceof PrincipalEquation) {
            changeable = ((PrincipalEquation) eqn).lhs().variables();
            awakeable = ((PrincipalEquation) eqn).rhs().variables();
        } else {
            throw new InternalCompilerError(
                    "Unexpected kind of equation " + eqn);
        }

        for (Variable v : changeable) {
            // If this equation is examined, then the bound for v may be changed
            addDependency(eqn, v);
        }
        for (Variable v : awakeable) {
            // If the bound for v is changed (upward), then we may need to
            // reexamine this equation.
            addDependency(v, eqn);
        }
    }

    /**
     * The default bound of label variables in this solver is bottom
     */
    @Override
    protected Label getDefaultLabelBound() {
        return ts.bottomLabel();
    }

    /**
     * The default bound of label variables in this solver is bottom
     */
    @Override
    protected Principal getDefaultPrincipalBound() {
        return ts.bottomPrincipal(Position.compilerGenerated());
    }

    /**
     * This method changes the bounds of variables in the RHS of Equation eqn,
     * to make the equation satisfied. If the equation has no variables in the
     * RHS, it just checks that the equation holds, and returns.
     * 
     * Otherwise, if the RHS has exactly one variable, then it refines that
     * variable
     * {@linkplain SolverGLB#refineVariableEquation(VarLabel, Equation) (see here)}
     * and returns. If the RHS has more than one variable, then the method
     * performs a search, attempting to refine each variable in turn, and then
     * recursively attempting to solve the set of equations.
     */
    @Override
    protected void solve_eqn(LabelEquation eqn) throws SemanticException {
        // there are occurrences of variables on the RHS of the equation
        // there may be a join of components on the RHS of the equation.
        // we will need to try all possible ways of satisfying this equation,
        // trying the simple ones (i.e. var labels) first.

        if (!eqn.rhs().hasVariableComponents()) {
            // the RHS has no variable components in it, it has nothing for us to
            // modify the bound of. It had better hold...

            // L <= L', where L' cannot contain variables. Failure will throw
            // an exception.
            checkEquation(eqn);
            return;
        }

        // at this point we know that at least one component of the RHS of eqn
        // has a variable...

        // get a count of them, to figure out if we need to do a search...
        List<Variable> rhsVariables =
                new ArrayList<Variable>(eqn.rhs().variableComponents());
        boolean isSingleVar = (rhsVariables.size() == 1);
        VarLabel singleVar = null;
        if (isSingleVar) singleVar = (VarLabel) rhsVariables.get(0);
        if (isSingleVar && (!isFixedValueVar(singleVar)
                || eqn.constraint().kind() == LabelConstraint.EQUAL)) {
            // only a single component is a variable
            refineVariableEquation(singleVar, eqn, true);
        } else {
            if (!isSingleVar && !allActivesAreMultiVarRHS()) {
                // some of the active equations have single variables
                // on the RHS. Satisfy those first, to reduce the search
                // effort.
                if (shouldReport(3))
                    report(3, "Deferring multi var RHS constraint");
                addEquationToQueue(eqn);
                return;
            }

            // we will do a very simple search, not delaying the
            // solution of any constraints...
            // we only need one of the components to satisfy the constraints.
            // we will just try each one in turn.
            // copy the bounds before we start modifying anything, so we can
            // restore it again later...
            VarMap origBounds = bounds().copy();
            // record the last failed try for error report
            SemanticException lastexception = null;
            VarLabel comp = null;
            Label lastlabel = null;

            for (Variable var : rhsVariables) {
                comp = (VarLabel) var;

                if (isFixedValueVar(comp)
                        && eqn.constraint().kind() != LabelConstraint.EQUAL) {
                    // this var label had it's value fixed when it's constraint
                    // was added. Do not try to alter it's value.
                    continue;
                }

                refineVariableEquation(comp, eqn, false);

                // check that the equation is now satisfied.
                Label lhsbound = triggerTransforms(bounds().applyTo(eqn.lhs()),
                        eqn.env());
                Label rhsbound = triggerTransforms(bounds().applyTo(eqn.rhs()),
                        eqn.env());

                try {
                    if (eqn.env().leq(lhsbound, rhsbound) && search(eqn)) {
                        // we were successfully able to find a solution to the
                        // constraints!
                        addTrace(comp, eqn.lhs(), eqn, bounds.boundOf(comp),
                                Direction.IN);
                        return;
                    }
                } catch (SemanticException search) {
                    if (shouldReport(2))
                        report(2, "Solution failed, backtracking");
                    lastexception = search;
                    lastlabel = bounds.boundOf(comp);
                }

                // search failed!
                // set the bounds back to their original settings, and
                // try with another component
                setBounds(origBounds);
            }

            // if we fall through to here, then the search failed.
            if (shouldReport(1)) {
                report(1, "Search for refinement to constraint " + eqn
                        + " failed.");
            }
            // when the search failed, report the last bound snapshot and the try
            if (lastexception != null) {
                addTrace(comp, eqn.lhs(), eqn, lastlabel, Direction.IN);
                throw lastexception;
            } else {
                throw reportError(eqn);
            }
        }

    }

    @Override
    protected void solve_eqn(PrincipalEquation eqn) throws SemanticException {
        if (!eqn.lhs().hasVariables()) {
            // the LHS has no variable components in it, it has nothing for us to
            // modify the bound of. It had better hold...

            checkEquation(eqn);
            return;
        }

        // at this point we know that there is at least one variable of the LHS

        // get a count of them, to figure out if we need to do a search...
        List<Variable> lhsVariables =
                new ArrayList<Variable>(eqn.lhs().variables());
        boolean isSingleVar = (lhsVariables.size() == 1);
        VarPrincipal singleVar = null;
        if (isSingleVar) singleVar = (VarPrincipal) lhsVariables.get(0);
        if (isSingleVar && (!isFixedValueVar(singleVar)
                || eqn.constraint().kind() == PrincipalConstraint.EQUIV)) {
            // only a single component is a variable
            refineVariableEquation(singleVar, eqn);
        } else {
            // we will do a very simple search, not delaying the
            // solution of any constraints...
            // we only need one of the components to satisfy the constraints.
            // we will just try each one in turn.
            // copy the bounds before we start modifying anything, so we can
            // restore it again later...
            VarMap origBounds = bounds().copy();

            for (Variable var : lhsVariables) {
                VarPrincipal comp = (VarPrincipal) var;

                if (isFixedValueVar(comp)) {
                    // this var had it's value fixed when it's constraint
                    // was added. Do not try to alter it's value.
                    continue;
                }

                refineVariableEquation(comp, eqn);
                // check that the equation is now satisfied.
                Principal lhsbound = bounds().applyTo(eqn.lhs());
                Principal rhsbound = bounds().applyTo(eqn.rhs());

                if (eqn.env().actsFor(lhsbound, rhsbound) && search(eqn)) {
                    // we were successfully able to find a solution to the
                    // constraints!
                    return;
                }

                // search failed!
                // set the bounds back to their original settings, and
                // try with another component
                setBounds(origBounds);
            }

            // if we fall through to here, then the search failed.
            if (shouldReport(1)) {
                report(1, "Search for refinement to constraint " + eqn
                        + " failed.");
            }
            throw reportError(eqn);
        }
    }

    /**
     * return true if every active constraint has multi vars on the RHS.
     */
    protected boolean allActivesAreMultiVarRHS() {
        for (Equation eqn : getQueue()) {
            if (eqn instanceof LabelEquation) {
                if (((LabelEquation) eqn).rhs().variableComponents()
                        .size() <= 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Raise the bound on the label variable v, which is a component of the RHS
     * of the equation eqn.
     */
    protected void refineVariableEquation(VarLabel v, LabelEquation eqn,
            boolean trace) throws SemanticException {
        Label vBound = bounds().boundOf(v);
        Label lhsBound =
                triggerTransforms(bounds().applyTo(eqn.lhs()), eqn.env());
        Label rhsBound =
                triggerTransforms(bounds().applyTo(eqn.rhs()), eqn.env());

        if (shouldReport(5)) report(5, "BOUND of " + v + " = " + vBound);
        if (shouldReport(5)) report(5, "RHSBOUND = " + rhsBound);
        if (shouldReport(5)) report(5, "LHSBOUND = " + lhsBound);

        // Try and raise v's bound just enough to satisfy the equation
        Label needed = findNeeded(lhsBound, rhsBound, eqn.env());

        if (shouldReport(5)) report(4, "NEEDED = " + needed);

        Label newBound = ts.join(vBound, needed);

        if (shouldReport(4))
            report(4, "JOIN (" + v + ", NEEDED) := " + newBound);

        if (v.mustRuntimeRepresentable()
                && !newBound.isRuntimeRepresentable()) {
            Label rtRep = eqn.env().findNonArgLabelUpperBound(newBound);
            if (shouldReport(4))
                report(4, "RUNTIME_REPR (" + newBound + ") := " + rtRep);
            newBound = rtRep;

        }

        // since this method can be called while trying out the rhs components, only add
        // the refinement when it succeeds
        if (trace) addTrace(v, eqn.lhs(), eqn, newBound, Direction.IN);
        setBound(v, newBound, eqn.labelConstraint());

        wakeUp(v);
    }

    /**
     * Raise the bound on the label variable v, which is a component of the equation eqn.
     */
    protected void refineVariableEquation(VarPrincipal v,
            PrincipalEquation eqn) {
        Principal vBound = bounds().boundOf(v);
        Principal lhsBound = bounds().applyTo(eqn.lhs());
        Principal rhsBound = bounds().applyTo(eqn.rhs());

        if (shouldReport(5)) report(5, "BOUND of " + v + " = " + vBound);
        if (shouldReport(5)) report(5, "RHSBOUND = " + rhsBound);
        if (shouldReport(5)) report(5, "LHSBOUND = " + lhsBound);

        // Raise v's bound
        Principal newBound =
                ts.conjunctivePrincipal(vBound.position(), vBound, rhsBound)
                        .simplify();

        if (shouldReport(4))
            report(4, "CONJUNCT (" + v + ", NEEDED) := " + newBound);

        // addTrace(v, eqn, newBound);
        setBound(v, newBound, eqn.principalConstraint());
        wakeUp(v);
    }

    /**
     * Return the most permissive label L such that lhs <= rhs join L
     */
    protected Label findNeeded(Label lhs, Label rhs, LabelEnv env) {
        if (lhs instanceof JoinLabel) {
            JoinLabel jl = (JoinLabel) lhs;
            Set<Label> needed = new LinkedHashSet<Label>();
            // jl = c1 join ... join cn
            // Want L to be the join of all ci such that ci is not <= rhs
            for (Label ci : jl.joinComponents()) {
                if (!env.leq(ci, rhs)) {
                    needed.add(findNeeded(ci, rhs, env));
                }
            }
            return ts.joinLabel(lhs.position(), needed);
        } else if (lhs instanceof MeetLabel) {
            MeetLabel ml = (MeetLabel) lhs;
            // ml = c1 meet ... meet cn
            // find the needed components of each of the ci.
            Set<Label> needed = new LinkedHashSet<Label>();
            for (Label ci : ml.meetComponents()) {
                needed.add(findNeeded(ci, rhs, env));
            }
            return ts.meetLabel(lhs.position(), needed);
        } else if (lhs instanceof PairLabel) {
            PairLabel pl = (PairLabel) lhs;
            ConfPolicy cp =
                    findNeeded(pl.confPolicy(), ts.confProjection(rhs), env);
            IntegPolicy ip =
                    findNeeded(pl.integPolicy(), ts.integProjection(rhs), env);
            return ts.pairLabel(lhs.position(), cp, ip);
        } else {
            return lhs;
        }
    }

    protected ConfPolicy findNeeded(ConfPolicy lhs, ConfPolicy rhs,
            LabelEnv env) {
        if (lhs instanceof JoinPolicy_c) {
            @SuppressWarnings("unchecked")
            JoinPolicy_c<ConfPolicy> jp = (JoinPolicy_c<ConfPolicy>) lhs;
            Set<ConfPolicy> needed = new LinkedHashSet<ConfPolicy>();
            // jl = c1 join ... join cn
            // Want L to be the join of all ci such that ci is not <= rhs
            for (ConfPolicy ci : jp.joinComponents()) {
                if (!env.leq(ci, rhs)) {
                    needed.add(findNeeded(ci, rhs, env));
                }
            }
            return ts.joinConfPolicy(lhs.position(), needed);
        } else if (lhs instanceof MeetPolicy_c) {
            @SuppressWarnings("unchecked")
            MeetPolicy_c<ConfPolicy> mp = (MeetPolicy_c<ConfPolicy>) lhs;
            // find the needed components of each of the ci.
            Set<ConfPolicy> needed = new LinkedHashSet<ConfPolicy>();
            for (ConfPolicy ci : mp.meetComponents()) {
                needed.add(findNeeded(ci, rhs, env));
            }
            return ts.meetConfPolicy(lhs.position(), needed);
        } else {
            return lhs;
        }
    }

    protected IntegPolicy findNeeded(IntegPolicy lhs, IntegPolicy rhs,
            LabelEnv env) {
        if (lhs instanceof JoinPolicy_c) {
            @SuppressWarnings("unchecked")
            JoinPolicy_c<IntegPolicy> jp = (JoinPolicy_c<IntegPolicy>) lhs;
            Set<IntegPolicy> needed = new LinkedHashSet<IntegPolicy>();
            // jl = c1 join ... join cn
            // Want L to be the join of all ci such that ci is not <= rhs
            for (IntegPolicy ci : jp.joinComponents()) {
                if (!env.leq(ci, rhs)) {
                    needed.add(findNeeded(ci, rhs, env));
                }
            }
            return ts.joinIntegPolicy(lhs.position(), needed);
        } else if (lhs instanceof MeetPolicy_c) {
            @SuppressWarnings("unchecked")
            MeetPolicy_c<IntegPolicy> mp = (MeetPolicy_c<IntegPolicy>) lhs;
            // find the needed components of each of the ci.
            Set<IntegPolicy> needed = new LinkedHashSet<IntegPolicy>();
            for (IntegPolicy ci : mp.meetComponents()) {
                needed.add(findNeeded(ci, rhs, env));
            }
            return ts.meetIntegPolicy(lhs.position(), needed);
        } else {
            return lhs;
        }
    }

    /**
     * Search recursively for solution to system of constraints.
     */
    protected boolean search(Equation eqn) throws SemanticException {
        if (shouldReport(2)) {
            report(2, "===== Starting recursive search =====");
        }
        SolverGLB js = new SolverGLB(this);

        // make sure this equation is satisfied before continuing.
        js.addEquationToQueueHead(eqn);

        setBounds(js.solve_bounds());
        if (shouldReport(2)) report(2, "Solution succeeded, finishing up");
        return true;
    }

    /**
     * Check that the equation eqn is satisfied. The RHS of eqn cannot have any
     * variables.
     * 
     * @throws SemanticException if eqn is not satisfied.
     * @throws InternalCompilerError if eqn contains variables
     */
    protected void checkEquation(LabelEquation eqn) throws SemanticException {
        if (eqn.rhs().hasVariableComponents()) {
            throw new InternalCompilerError("RHS of equation " + eqn
                    + " should not contain variables.");
        }

        // This equation must have been woken up. We need to
        // check whether it is solvable given the current variables.

        Label rhsLabel =
                triggerTransforms(bounds().applyTo(eqn.rhs()), eqn.env());
        if (shouldReport(4)) report(4, "RHS = " + rhsLabel);

        Label lhsBound =
                triggerTransforms(bounds().applyTo(eqn.lhs()), eqn.env());
        if (shouldReport(4)) report(4, "LHS APP = " + lhsBound);

        // Check to see if it is currently satisfiable.
        if (!eqn.env().leq(lhsBound, rhsLabel)) {
            //            //try bounding the dynamic labels
            //            if (dynCheck(lhsBound, rhsLabel, eqn.env())) return;
            // This equation isn't satisfiable.
            throw reportError(eqn);
        }
    }

    /**
     * Check that the equation eqn is satisfied. The LHS of eqn cannot have any
     * variables.
     * 
     * @throws SemanticException if eqn is not satisfied.
     * @throws InternalCompilerError if eqn contains variables
     */
    protected void checkEquation(PrincipalEquation eqn)
            throws SemanticException {
        if (eqn.lhs().hasVariables()) {
            throw new InternalCompilerError("LHS of equation " + eqn
                    + " should not contain variables.");
        }

        // This equation must have been woken up. We need to
        // check whether it is solvable given the current variables.

        Principal rhsBound = bounds().applyTo(eqn.rhs());
        if (shouldReport(4)) report(4, "RHS = " + rhsBound);

        Principal lhsBound = bounds().applyTo(eqn.lhs());
        if (shouldReport(4)) report(4, "LHS APP = " + lhsBound);

        // Check to see if it is currently satisfiable.
        if (!eqn.env().actsFor(lhsBound, rhsBound)) {

            // This equation isn't satisfiable.
            throw reportError(eqn);
        }

    }

    /**
     * Find a contradicting equation.
     */
    @Override
    protected Equation findContradictiveEqn(LabelConstraint c) {
        if (c.lhsLabel().variableComponents().size() == 1) {
            // The LHS is has a single VarLabel, so we may be able to find
            // an equation that contradicts this one.
            VarLabel v = (VarLabel) c.lhsLabel().variableComponents().iterator()
                    .next();
            return findTrace(v, bounds().applyTo(c.rhsLabel()), false);
        }
        // TODO: could try some other ways to find contradictive
        // equation, or could produce a different error message.
        return null;
    }
}
