package jif.types;

import java.util.*;

import jif.JifOptions;
import jif.types.label.*;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.util.*;

/**
 * A solver of Jif constraints. Finds solution to constraints essentially by
 * propogating upper bounds backwards.
 */
public abstract class Solver {
    private LinkedList Q; // Queue of active equations to work on

    private LinkedList scc; // List of strongly connected components that

    // are left to work on
    private Collection currentSCC; // Set of equations that is the current
                                   // strongly

    // connected component that is being worked on
    private Collection equations; // Set of all equations

    /**
     * Map from variables to Set of equations containing them. If the bound of
     * the variable v changes, then we may need to re-examine all equations in
     * (Set)varEqnDependencies.get(v)
     */
    private Map varEqnDependencies;

    /**
     * Map from equations to Set of variables whose bound may be modified as a
     * result of solving that equation. If the bound of variables in the
     * equation eqn are modified, then the variables will be contained in
     * (Set)eqnVarDependencies.get(eqn)
     */
    private Map eqnVarDependencies;

    /**
     * Map from variables to collection of equality constraints on those
     * variables. This map allows us to detect early if some constraint violates
     * an equality constraint. Note that early checking of equality constraints
     * does not affect the soundness of the solver, just when we determine that
     * the constraints cannot be satisfied, and which constraint we decide to
     * blame for this.
     */
    private Map varEqualConstraints;

    protected static final int STATUS_NOT_SOLVED = 0;

    protected static final int STATUS_SOLVING = 1;

    protected static final int STATUS_SOLVED = 2;

    protected static final int STATUS_NO_SOLUTION = 3;

    private int status; // true if the current system has been solved

    private VarMap bounds; // Current bounds on label variables

    protected VarMap dynBounds; // bounds of dynamic labels

    protected JifTypeSystem ts;

    private Map traces; //Map from variables to their histories of refining

    private static Collection topics = CollectionUtil.list("solver", "jif");

    /**
     * This boolean is used to turn on or off whether the strongly connected
     * components optimization is used.
     * 
     * If true, the constraints are partitioned into strongly connected
     * components, which are then sorted in topological order. If false, then
     * constraints are solved in the order they are added to the solver
     */
    private static final boolean useSCC = false;

    /**
     * The name of the solver, for debugging purposes.
     */
    private String solverName;
    /**
     * Constructor
     */
    public Solver(JifTypeSystem ts, String solverName) {
        this.ts = ts;
        Q = new LinkedList();
        equations = new LinkedHashSet();
        varEqnDependencies = new LinkedHashMap();
        eqnVarDependencies = new LinkedHashMap();
        varEqualConstraints = new LinkedHashMap();
        traces = new LinkedHashMap();
        status = STATUS_NOT_SOLVED;
        bounds = new VarMap(ts, getDefaultBound());
        dynBounds = new VarMap(ts, getDefaultBound());
        scc = null;
        currentSCC = null;
        this.solverName = solverName + " (#" + (++solverCounter) + ")";
    }
    
    private static int solverCounter;

    /**
     * Constructor
     */
    protected Solver(Solver js) {
        this.ts = js.ts;
        Q = new LinkedList(js.Q);
        equations = new LinkedHashSet(js.equations);

        varEqnDependencies = js.varEqnDependencies;
        eqnVarDependencies = js.eqnVarDependencies;
        varEqualConstraints = js.varEqualConstraints;
        traces = new LinkedHashMap(js.traces);
        status = js.status;
        bounds = js.bounds.copy();
        dynBounds = js.dynBounds.copy();
        equations = new LinkedHashSet(js.equations);
        scc = new LinkedList(js.scc);
        solverName = js.solverName;
    }

    /**
     * Convenience method to report messages for the appropriate topics
     */
    static final public void report(int level, String s) {
        Report.report(level, s);
    }

    /**
     * Convenience method to determine if messages of the given obscurity should
     * be reported.
     */
    static final public boolean shouldReport(int obscurity) {
        return Report.should_report(topics, obscurity);
    }

    public Label applyBoundsTo(Label L) {
        return bounds.applyTo(L);
    }

    protected List getQueue() {
        return Collections.unmodifiableList(Q);
    }

    protected void addEquationToQueue(Equation eqn) {
        Q.add(eqn);
    }

    protected void addEquationToQueueHead(Equation eqn) {
        Q.addFirst(eqn);
    }

    /**
     * Creating the equation graph with edges based on information gathered
     * during the addConstraint method.
     * 
     * TODO: XXX Maybe do most of this work while adding constraints?
     */
    protected final Graph createGraph() {
        Set nodes = new LinkedHashSet(equations);
        // edges going from equations to equations
        // there is an edge from equation e1 to e2 iff
        // solving e1 may modify the bound of a variable and thus invalidate e2
        Map edges = new LinkedHashMap();

        Set removedEqns = new LinkedHashSet();
        // removing stuff of form x <= x (always true, as for all x, x <= x)
        // and removing stuff that has no variables
        if (shouldReport(5))
                report(5, "=====Equations excluded from solving loop=====");
        for (Iterator e = nodes.iterator(); e.hasNext();) {
            Equation toCheck = (Equation)e.next();
            Label lhs = toCheck.lhs();
            Label rhs = toCheck.rhs();

            if (lhs.equals(rhs)) {
                removedEqns.add(toCheck);
                e.remove();
                if (shouldReport(5)) report(5, toCheck.toString());
            }
            else if (!toCheck.env().hasVariables() && !lhs.hasVariables()
                    && !rhs.hasVariables()) {
                removedEqns.add(toCheck);
                e.remove();
                if (shouldReport(5)) report(5, toCheck.toString());
            }
        }
        if (shouldReport(5))
                report(5, "Equations excluded: " + removedEqns.size());

        // consistency check. uncomment if debugging
        /*
         * if (removedEqns.size() + nodes.size() != equations.size()) {
         * System.out.println("removed: " + removedEqns.size());
         * System.out.println("nodes: " + nodes.size());
         * System.out.println("equations: " + equations.size());
         * System.out.println("Something doesn't add up."); }
         */

        for (Iterator e = nodes.iterator(); e.hasNext();) {
            Equation toCheck = (Equation)e.next();

            // get the equations that are dependent on the equation toCheck
            // by finding the variables that toCheck may alter, and then
            // finding the equations that depend on those variables.
            Set dependentEqns = eqnEqnDependencies(toCheck);
            if (!dependentEqns.isEmpty()) {
                Set s = (Set)edges.get(toCheck);
                if (s == null) {
                    s = new LinkedHashSet();
                    edges.put(toCheck, s);
                }
                s.addAll(dependentEqns);

                // not exactly efficient but it works
                s.removeAll(removedEqns);
            }
        }

        // removing all mappings to empty sets
        // other way to do this is to iterate accross the equation list
        // from the dependency graph but that gets... interesting
        for (Iterator e = edges.values().iterator(); e.hasNext();) {
            Set s = (Set)e.next();
            if (s.isEmpty()) {
                e.remove();
            }
        }

        return new Graph(nodes, edges);
    }

    /**
     * Get the bounds for this Solver.
     */
    public VarMap bounds() {
        return bounds;
    }

    /**
     * Set the bounds for this Solver.
     */
    public void setBounds(VarMap bnds) {
        this.bounds = bnds;
    }

    /**
     * 
     * @param v the VarLabel to set the bound for
     * @param newBound the new bound for v
     * @param responsible the constraint that was responsible for modifying the
     *            bound.
     * @throws SemanticException if the new bound violates an equality
     *             constraint.
     */
    public void setBound(VarLabel v, Label newBound, LabelConstraint responsible)
            throws SemanticException {
        Label oldBound = bounds.applyTo(v);
        bounds.setBound(v, newBound);

        Collection eqConstraints = (Collection)varEqualConstraints.get(v);
        if (eqConstraints != null) {
            // check that the new bound does not violate the equality
            // constraints.
            for (Iterator iter = eqConstraints.iterator(); iter.hasNext();) {
                LabelConstraint eqCnstr = (LabelConstraint)iter.next();
                if (eqCnstr.rhs().hasVariables()) {
                    // the right hand side has variables.
                    // this means it may be tricky to perform early checking
                    // of equality constraints.
                    // we'll skip it.
                    continue;
                }

                Label boundRHS = bounds.applyTo(eqCnstr.rhs());
                if (!(eqCnstr.env().leq(newBound, boundRHS) && eqCnstr.env()
                        .leq(boundRHS, newBound))) {
                    // the equality constraint has been violated!
                    if (shouldReport(4)) {
                        report(4, "Equality constraint violated: " + eqCnstr);
                    }
                    // set the bound back to the original bound, to make the
                    // error message comprehensible.
                    bounds.setBound(v, oldBound);
                    reportError(responsible, Collections.singletonList(v));
                }
            }
        }
    }

    /**
     * Solve the system of constraints. If the system has already been solved,
     * then returned the cached solution.
     * 
     * @throws SemanticException if the Solver cannot find a solution to the
     *             system of contraints.
     */
    public final VarMap solve() throws SemanticException {
        // Cache the solution.
        if (status == STATUS_SOLVED || status == STATUS_NO_SOLUTION) {
            return bounds;
        }
        if (status == STATUS_SOLVING) {
            throw new InternalCompilerError("solve called on solver while "
                    + "in the process of solving.");
        }

        // status at this point is STATUS_NOT_SOLVED
        status = STATUS_SOLVING;

        //bounds = new VarMap(ts, getDefaultBound());

        if (shouldReport(1)) {
            report(1, "===== Starting solver " + solverName + " =====");
            report(1, "   " + equations.size() + " equations");
        }
        if (useSCC) {
//                System.err.println("  create graph start: " + new java.util.Date());
            Graph eqnGraph = createGraph();

            if (eqnGraph != null) {
                if (shouldReport(5)) {
                    report(5, "=====Equation Graph=====");
                    report(5, eqnGraph.toString());
                }
            }
//            System.err.println("  get super node start: " + new java.util.Date());
            Graph h = eqnGraph.getSuperNodeGraph();
            if (shouldReport(5)) {
                report(5, "=====Strongly Connected Equation Graph=====");
                report(5, h.toStringSetNodes());
            }
//            System.err.println("  topo sort start: " + new java.util.Date());
            scc = h.topoSort();
            if ((scc == null) || (eqnGraph == null)) {
                throw new InternalCompilerError("Unable to construct "
                        + "strongly connected components for equation graph");
            }
//            System.err.println("  done: " + new java.util.Date());
            if (shouldReport(1)) {
                report(1, "   " + scc.size() + " strongly connected components");
            }
        }
        else {
            // not using SCC
            scc = new LinkedList();
            scc.add(equations);
        }

        // pre-initialize the queue with the first strongly connected
        // componenet
        if (scc.isEmpty()) {
            Q = new LinkedList();
            currentSCC = new LinkedHashSet();
        }
        else {
            currentSCC = new LinkedHashSet((Collection)scc.removeFirst());
            Q = new LinkedList(currentSCC);
        }

        try {
            VarMap soln = solve_bounds();
            status = STATUS_SOLVED;
            if (shouldReport(1)) {
                report(1, "   finished " + solverName);
            }
            return soln;
        }
        catch (SemanticException e) {
            status = STATUS_NO_SOLUTION;
            throw e;
        }
    }

    /**
     * This method must return a constant label, which is the default bound of
     * variables.
     */
    protected abstract Label getDefaultBound();

    /**
     * Solve the system of constraints, by finding upper bounds for the label
     * variables.
     * 
     * @return a solution to the system of constraints, in the form of a VarMap
     *         of the upper bounds of the label variables.
     * @throws SemanticException if the Solver cannot find a solution to the
     *             system of contraints.
     */
    protected VarMap solve_bounds() throws SemanticException {
        // Solve the system of constraints. bounds may already contain a
        // partial solution, in which case attempt to complete the solution.

        if (shouldReport(3)) {
            report(3, "======EQUATIONS======");
            for (Iterator i = equations.iterator(); i.hasNext();) {
                Equation eqn = (Equation)i.next();
                report(3, eqn.toString());
            }
        }

        int counter = 0;

        if (Q.isEmpty()) checkCandidateSolution();

        while (!Q.isEmpty()) {
            while (!Q.isEmpty()) {
                counter++;
                Equation eqn = (Equation)Q.removeFirst();
                Label lhsbound = bounds.applyTo(eqn.lhs());
                Label rhsbound = bounds.applyTo(eqn.rhs());

                if (eqn.env().leq(lhsbound, rhsbound)) {
                    if (shouldReport(5))
                            report(5, "constraint: " + eqn
                                    + " already satisfied: " + lhsbound + "<="
                                    + rhsbound);
                }
                else {
                    if (shouldReport(4)) {
                        report(4, "Considering constraint: " + eqn + " (line "
                                + eqn.position().line() + ")");
                    }
                    // let the subclass deal with changing the bounds on
                    // variables
                    // to make this equation satisfied.
                    solve_eqn(eqn);
                }

                // if we finished the last strongly connected component
                // move to the next one
                // 
                // done this way instead of an outer loop because of the
                // way the search method works
                if (Q.isEmpty() && !scc.isEmpty()) {
                    currentSCC = new LinkedHashSet((Set)scc.removeFirst());
                    Q.addAll(currentSCC);
                }
            } // end while

            checkCandidateSolution();
        }
        if (shouldReport(2))
                report(2, "Number of relaxation steps: " + counter);
        bounds.print();

        return bounds;
    }

    /**
     * This method changes the bounds of variables in the Equation eqn, to make
     * the equation satisfied. The method may postpone solving the equation by
     * putting the equation back on the queue, using addEquationToQueue().
     */
    protected abstract void solve_eqn(Equation eqn) throws SemanticException;

    /**
     * Check the candidate solution
     */
    protected final void checkCandidateSolution() throws SemanticException {
        if (shouldReport(4)) {
            report(4, "===== Checking candidate solution =====");
        }
        // We are done refining the upper bounds of the variables.
        // Make one final check that all equations are satisfied.
        // This will force a check on equations with unconstrained
        // variables.
        for (Iterator i = equations.iterator(); i.hasNext();) {
            Equation eqn = (Equation)i.next();

            Label lhsBound = bounds.applyTo(eqn.lhs());
            Label rhsBound = bounds.applyTo(eqn.rhs());

            if (shouldReport(4)) {
                report(4, "Checking equation: " + eqn);
            }

            if (shouldReport(6)) {
                report(6, "LHS = " + eqn.lhs());
                report(6, "LHS APP = " + lhsBound);
                report(6, "RHS APP = " + rhsBound);
            }

            // Check to see if it is currently satisfiable.
            if (!eqn.env().leq(lhsBound, rhsBound)) {
                //if (!dynCheck(lhsBound, rhsBound, eqn.env())) {
                reportError(eqn.constraint(), eqn.variables());
                //}
            }
        }
    }

    /**
     * Awakens all equations in the system that depend on the variable v,
     * ensuring that they are in the queue of active equations.
     */
    protected final void wakeUp(VarLabel v) {
        Set eqns = (Set)varEqnDependencies.get(v);

        if (eqns != null) {
            for (Iterator i = eqns.iterator(); i.hasNext();) {
                Equation eqn = (Equation)i.next();
                // if its in the current strongly connected set
                // and its not in the Queue, add it
                if (!Q.contains(eqn)
                        && (currentSCC == null || currentSCC.contains(eqn))) // offensive
                                                                             // but
                                                                             // simple
                        Q.add(eqn);
            }
        }
    }

    /**
     * Counter of the number of constraints added to the system. For debugging
     * purposes.
     */
    static private int constraint_counter = 0;

    /**
     * Constraint number at which to stop the compiler, for debugging purposes.
     * Set using a command line option, refer to {@link jif.ExtensionInfo}for
     * details
     */
    static public int stop_constraint = 0;

    /**
     * Increase the count of the number of constraints added (not just to this
     * system, but to all instances of the Solver).
     * 
     * For debugging purposes, if the constraint counter is equal to the
     * stop_constraint, then a RuntimeException is thrown.
     */
    protected final void inc_counter() {
        constraint_counter++;
        if (constraint_counter == stop_constraint) {
            System.err.println("Halting at constraint " + stop_constraint);
            throw new RuntimeException("Halting at constraint "
                    + stop_constraint);
        }
    }

    /**
     * Add the constraint c to the system
     */
    public final void addConstraint(LabelConstraint c) throws SemanticException {
        if (status != STATUS_NOT_SOLVED) {
            throw new InternalCompilerError("Computed solution already. "
                    + "Cannot add more constraints");
        }

        if (shouldReport(5)) report(5, (constraint_counter + 1) + ": " + c);
        if (shouldReport(6)) report(6, ">>> " + c.msg());
        inc_counter();

        if (!c.lhs().isCanonical() || !c.rhs().isCanonical()) {
            throw new SemanticException(errorMsg(c), c.position());
        }

        if (c.lhs() instanceof NotTaken && c.kind() == LabelConstraint.LEQ) {
            // if the LHS is NotTaken, then the constraint will always be
            // satisfied.
            return;
        }

        if (c.rhs() instanceof NotTaken && c.kind() == LabelConstraint.LEQ) {
            // if the RHS is NotTaken (and the LHS isn't), then the
            // constraint can never be satisfied.
            throw new SemanticException(errorMsg(c), c.position());
        }

        if (c.lhs() instanceof VarLabel && c.kind() == LabelConstraint.EQUAL) {
            // this is an equality constraint on a variable. Let's record it!
            VarLabel v = (VarLabel)c.lhs();
            Collection eqCnstrnts = (Collection)varEqualConstraints.get(v);
            if (eqCnstrnts == null) {
                eqCnstrnts = new LinkedList();
                varEqualConstraints.put(v, eqCnstrnts);
            }
            eqCnstrnts.add(c);
            bounds.setBound(v, bounds.applyTo(c.rhs()));
        }

        Collection eqns = c.getEquations();
        Equation eqn = null;
        for (Iterator i = eqns.iterator(); i.hasNext();) {
            eqn = (Equation)i.next();
            if (!eqn.env().hasVariables() && !eqn.lhs().hasVariables()
                    && !eqn.rhs().hasVariables()) {
                // The equation has no variables. We can check now if it is
                // satisfied or not
                if (!eqn.evaluate()) {
                    if (shouldReport(2)) {
                        report(2, "Statically failed " + eqn);
                    }
                    // The equation is not satisfied.
                    throw new SemanticException(errorMsg(eqn.constraint()), eqn
                            .position());
                }
                else {
                    // The equation is satisfied, no need to add it to
                    // the queue.
                }
            }
            else {
                if (shouldReport(5)) report(5, "Adding equation: " + eqn);
                eqn.env().setSolver(this);
                equations.add(eqn);
                addDependencies(eqn);
            }

        }
    }

    /**
     * This abstract method must add the correct dependencies from Equation eqn
     * to varaiables occuring in eqn, and dependencies in the other direction
     * (that is, from variables occuring in eqn to eqn).
     * 
     * There is a dependency from Equation eqn to variable var if the bound on
     * var may be modified as a result of solving eqn. This dependency should be
     * recorded by calling the method addDependency(eqn, var).
     * 
     * There is a dependency from variable var to Equation eqn if modifying the
     * bound on var may cause eqn to no longer be satisfied. This dependency
     * should be recorded by calling the method addDependency(var, eqn).
     */
    protected abstract void addDependencies(Equation eqn);

    /**
     * This method records a dependency from variable var to Equation eqn. This
     * method should only be called by subclasses during the execution of the
     * method addDependencies().
     * 
     * There is a dependency from variable var to Equation eqn if modifying the
     * bound on var may cause eqn to no longer be satisfied.
     */
    protected void addDependency(VarLabel var, Equation eqn) {
        Set eqns = (Set)varEqnDependencies.get(var);

        if (eqns == null) {
            eqns = new LinkedHashSet();
            varEqnDependencies.put(var, eqns);
        }

        eqns.add(eqn);
    }

    /**
     * This method records a dependency from Equation eqn to variable var. This
     * method should only be called by subclasses during the execution of the
     * method addDependencies().
     * 
     * There is a dependency from Equation eqn to variable var if the bound on
     * var may be modified as a result of solving eqn.
     */
    protected void addDependency(Equation eqn, VarLabel var) {
        Set vars = (Set)eqnVarDependencies.get(eqn);

        if (vars == null) {
            vars = new LinkedHashSet();
            eqnVarDependencies.put(eqn, vars);
        }

        vars.add(var);
    }

    /**
     * Returns the equations that are dependent on the equation eqn by finding
     * the variables that eqn may alter if it is solved (useing the map
     * eqnVarDependencies), and then finds the equations that depend on those
     * variables (using the map varEqnDependencies)
     */
    private Set eqnEqnDependencies(Equation eqn) {
        Set vars = (Set)eqnVarDependencies.get(eqn);

        if (vars == null || vars.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        Set eqns = new LinkedHashSet();
        for (Iterator i = vars.iterator(); i.hasNext();) {
            VarLabel v = (VarLabel)i.next();
            Set s = (Set)varEqnDependencies.get(v);
            if (s != null) {
                eqns.addAll(s);
            }
        }
        return eqns;
    }

    /**
     * Binds a dynamic label to an actual label. This kind of binding is caused
     * by assignments like final label lb = new label{L};
     */
    //    public final void bind(DynamicLabel dl, Label l) {
    //        dynBounds.setBound(dl, l);
    //    }
    // *********************************************************
    //
    // Error reporting
    /**
     * Record the fact that label variable v, in the constraint eqn had its
     * bound set to the label lb.
     */
    protected final void addTrace(VarLabel v, Equation eqn, Label lb) {
        List trace = (List)traces.get(v);
        if (trace == null) {
            trace = new LinkedList();
            traces.put(v, trace);
        }
        trace.add(new Pair(eqn, lb.copy()));
    }

    /**
     * @return an equation that contained label variable var, and that changed
     *         the bound of var to less than/greater than threshold. Null if no
     *         such equation exists. If lowerThreshold is true, then the
     *         equation returned is one that changed var to less than threshold;
     *         otherwise the equation returned is one that changed var to
     *         greater than the threshold
     */
    protected final Equation findTrace(VarLabel var, Label threshold,
            boolean lowerThreshold) {
        List history = (List)traces.get(var);
        if (history != null) {
            for (Iterator iter = history.iterator(); iter.hasNext();) {
                Pair eqn_label = (Pair)iter.next();
                Label label = (Label)eqn_label.part2();
                Equation eqn = (Equation)eqn_label.part1();
                boolean test = lowerThreshold ? eqn.env().leq(threshold, label)
                        : eqn.env().leq(label, threshold);
                if (!test) {
                    return eqn;
                }
            }
        }
        return null;
    }

    /**
     * Report the traces for each variables in the collection
     * <code>Variables</code>
     */
    protected void reportTraces(Collection variables) {
        if (shouldReport(3)) {
            // We'll produce the traces...

            StringBuffer trcs = new StringBuffer();
            for (Iterator vs = variables.iterator(); vs.hasNext();) {
                VarLabel v = (VarLabel)vs.next();

                List trace = (List)traces.get(v);
                if (trace != null) {
                    StringBuffer trc = new StringBuffer("\nTrace for " + v
                            + ":\n");

                    trc.append("  initially : " + getDefaultBound() + "\n");
                    for (int i = 0; i < trace.size(); ++i) {
                        Pair p = (Pair)trace.get(i);
                        Equation e = (Equation)p.part1();
                        Label l = (Label)p.part2();
                        trc.append("  " + i + ": " + l + " from eqn " + e.lhs()
                                + " <= " + e.rhs() + "\n");
                        trc.append("       from " + e.constraint());
                        if (e.constraint().position() != null) {
                            trc.append(" (line "
                                    + e.constraint().position().line() + ")");
                        }
                        trc.append("\n");
                    }
                    trcs.append(trc.toString());
                }
            }

            report(3, trcs.toString());
        }
    }

    protected boolean errorShowConstraint() {
        return (errorShowTechnicalMsg() || errorShowDetailMsg());
    }

    protected boolean errorShowTechnicalMsg() {
        return false;
    }

    protected boolean errorShowDetailMsg() {
        return ((JifOptions)Options.global).explainErrors;
    }

    protected boolean errorShowDefns() {
        return (errorShowTechnicalMsg() || errorShowDetailMsg())
                && errorShowConstraint();
    }

    /**
     * Produce an error message for the constraint c, which cannot be satisfied.
     */
    protected final String errorMsg(LabelConstraint c) {

        StringBuffer sb = new StringBuffer();

        if (errorShowConstraint()) {
            sb.append("Unsatisfiable constraint: \n");
            //sb.append(" \n------------------------ \n");
            sb.append(errorStringConstraint(c));
            sb.append(" \n \n");
        }

        if (errorShowDefns()) {
            sb.append("Label Descriptions");
            sb.append(" \n------------------");
            sb.append(errorStringDefns(c));
            sb.append(" \n \n");
        }

        if (errorShowTechnicalMsg()) {
            sb.append(c.technicalMsg());
        }
        else if (errorShowDetailMsg()) {
            sb.append(c.detailMsg());
        }
        else {
            sb.append(c.msg());
        }
        return sb.toString();
    }

    /**
     * Produce a string appropriate for an error message that displays the
     * unsatisfiable constraint <code>c</code>.
     */
    protected String errorStringConstraint(LabelConstraint c) {
        StringBuffer sb = new StringBuffer();
        if (c.namedLhs() != null || c.namedRhs() != null) {
            sb.append("  ");
            sb.append(c.namedLhs());
            sb.append(c.kind());
            sb.append(c.namedRhs());
            sb.append(" \n");
        }

        sb.append("\t");
        sb.append(bounds.applyTo(c.lhs()));
        sb.append(c.kind());
        sb.append(bounds.applyTo(c.rhs()));
        if (!c.env().isEmpty()) {
            sb.append(" \nin environment \n   ");
            sb.append(c.env());
        }

        return sb.toString();
    }

    /**
     * Produce a string appropriate for an error message that displays the
     * definitions needed by the unsatisfiable constraint <code>c</code>.
     */
    protected String errorStringDefns(LabelConstraint c) {
        StringBuffer sb = new StringBuffer();

        Map defns = c.definitions(bounds);
        for (Iterator iter = defns.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Map.Entry)iter.next();
            sb.append(" \n - ");
            sb.append((String)e.getKey());
            List l = (List)e.getValue();
            for (Iterator j = l.iterator(); j.hasNext();) {
                sb.append(" = ");
                sb.append((String)j.next());
                if (j.hasNext()) {
                    sb.append(" \n - ");
                    sb.append((String)e.getKey());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Throws a SemanticException with the appropriate error message.
     * 
     * @param c The constraint that cannot be satisfied.
     * @throws SemanticException always.
     */
    protected void reportError(LabelConstraint c, Collection variables)
            throws SemanticException {
        while (!c.report()) {
            // we don't want to blame this constraint for the error, if
            // possible. Try to find the constraint that made this one
            // unsatisfiable.
            Equation eqn = findContradictiveEqn(c);
            if (eqn == null) {
                // we can't find a contradictive eqn. Just use this one.
                if (shouldReport(3))
                        report(3, "Could not find contradictive eqn for " + c);
                break;
            }
            if (shouldReport(3))
                    report(3, "Found contradictive eqn for " + c + "; it is "
                            + eqn);
            c = eqn.constraint();
        }

        Position pos = c.position();

        if (variables != null) reportTraces(variables);

        throw new SemanticException(errorMsg(c), pos);
    }

    protected abstract Equation findContradictiveEqn(LabelConstraint c);
}