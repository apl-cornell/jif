package jif.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jif.Topics;
import jif.types.InformationFlowTrace.Direction;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.Label;
import jif.types.label.NotTaken;
import jif.types.label.VarLabel;
import jif.types.label.Variable;
import jif.types.principal.Principal;
import jif.types.principal.VarPrincipal;
import polyglot.frontend.Compiler;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.util.CollectionUtil;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Pair;

/**
 * A solver of Jif constraints. Finds solution to constraints essentially by
 * propagating upper bounds backwards.
 */
public abstract class AbstractSolver implements Solver {
    protected EquationQueue Q; // Queue of active equations to work on

    protected LinkedList<Set<Equation>> scc; // List of strongly connected components that
    // are left to work on

    protected Set<Equation> currentSCC; // Equations in the strongly connected component that we are currently working on

    // connected component that is being worked on
    protected Collection<Equation> equations; // Set of all equations

    // failed constrians in the type checking
    protected List<FailedConstraintSnapshot> failedEquations; // Set of all equations

    /**
     * Map from variables to (Set of) equations that may be invalidated by
     * the variable changing. That is, if the bound of
     * the variable v changes, then we may need to re-examine all equations in
     * (Set)varEqnDependencies.get(v)
     */
    private Map<Variable, Set<Equation>> varEqnDependencies;

    /**
     * Map from equations to (Set of) variables whose bound may be modified as a
     * result of solving that equation. If the bound of variables in the
     * equation eqn are modified, then the variables will be contained in
     * (Set)eqnVarDependencies.get(eqn)
     */
    private Map<Equation, Set<Variable>> eqnVarDependencies;

    /**
     * Map from variables to (Set of) equations that may change the value
     * of the variable. That is, when satisfying any equation in
     * (Set)varEqnReverseDependencies.get(v), the value of v may be changed.
     */
    private Map<Variable, Set<Equation>> varEqnReverseDependencies;

    /**
     * Map from equations to (Set of) variables in which a change in value
     * may invalidate this equation. That is, when the value of any variable
     * in (Set)eqnVarReverseDependencies.get(e), changes, the
     * equation e may be invalidated.
     */
    private Map<Equation, Set<Variable>> eqnVarReverseDependencies;

    /**
     * Set of Variables that had their initial value fixed when the constraint
     * was added.
     */
    protected Set<Variable> fixedValueVars;

    protected static final int STATUS_NOT_SOLVED = 0;

    protected static final int STATUS_SOLVING = 1;

    protected static final int STATUS_SOLVED = 2;

    protected static final int STATUS_NO_SOLUTION = 3;

    protected int status; // status of the current system has been solved

    protected VarMap bounds; // Current bounds on label variables

    protected JifTypeSystem ts;

    /**
     * Constraints that were added to the solver, and failed statically.
     * If the flag THROW_STATIC_FAILED_CONSTRAINTS is true, then the
     * constraint will be thrown immediately, otherwise the constraint
     * will be added to this set, and thrown when solve() is called.
     */
    protected Set<Equation> staticFailedConstraints;
    protected static final boolean THROW_STATIC_FAILED_CONSTRAINTS = false;
    protected final Compiler compiler;

    protected Map<VarLabel, List<Pair<Equation, Label>>> traces; //Map from variables to their histories of refining
    public List<InformationFlowTrace> fullTrace;

    protected static Collection<String> topics =
            CollectionUtil.list(Topics.jif, Topics.solver);

    /**
     * This boolean is used to turn on or off whether the strongly connected
     * components optimization is used.
     * 
     * If true, the constraints are partitioned into strongly connected
     * components, which are then sorted in topological order. If false, then
     * constraints are solved in the order they are added to the solver
     */
    protected final boolean useSCC;

    /**
     * The name of the solver, for debugging purposes.
     */
    private final String solverName;

    /**
     * Number of solvers instantiated, for debugging purposes
     */
    protected static int solverCounter;

    /**
     * Constructor
     */
    protected AbstractSolver(JifTypeSystem ts, Compiler compiler,
            String solverName) {
        this(ts, compiler, solverName, false);
    }

    /**
     * Constructor
     */
    protected AbstractSolver(JifTypeSystem ts, Compiler compiler,
            String solverName, boolean useSCC) {
        this.ts = ts;
        this.compiler = compiler;
        this.useSCC = useSCC;

        Q = new EquationQueue();
        equations = new LinkedHashSet<Equation>();
        varEqnDependencies = new LinkedHashMap<Variable, Set<Equation>>();
        eqnVarDependencies = new LinkedHashMap<Equation, Set<Variable>>();
        varEqnReverseDependencies =
                new LinkedHashMap<Variable, Set<Equation>>();
        eqnVarReverseDependencies =
                new LinkedHashMap<Equation, Set<Variable>>();
        traces = new LinkedHashMap<VarLabel, List<Pair<Equation, Label>>>();
        setStatus(STATUS_NOT_SOLVED);
        bounds = new VarMap(ts, getDefaultLabelBound(),
                getDefaultPrincipalBound());
        scc = null;
        currentSCC = null;
        this.solverName = solverName + " (#" + (++solverCounter) + ")";
        this.fixedValueVars = new HashSet<Variable>();
        failedEquations = new ArrayList<FailedConstraintSnapshot>();
        fullTrace = new ArrayList<InformationFlowTrace>();
    }

    /**
     * Constructor
     */
    protected AbstractSolver(AbstractSolver js) {
        this.ts = js.ts;
        this.compiler = js.compiler;
        this.useSCC = js.useSCC;
        Q = new EquationQueue(js.Q);
        equations = new LinkedHashSet<Equation>(js.equations);

        varEqnDependencies = js.varEqnDependencies;
        eqnVarDependencies = js.eqnVarDependencies;
        traces = new LinkedHashMap<VarLabel, List<Pair<Equation, Label>>>(
                js.traces);
        status = js.status;
        bounds = js.bounds.copy();
        equations = new LinkedHashSet<Equation>(js.equations);
        scc = new LinkedList<Set<Equation>>(js.scc);
        solverName = js.solverName;
        fixedValueVars = js.fixedValueVars;
        failedEquations = new ArrayList<FailedConstraintSnapshot>();
        fullTrace = new ArrayList<InformationFlowTrace>();
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

    @Override
    public Label applyBoundsTo(Label L) {
        return bounds.applyTo(L);
    }

    protected Label triggerTransforms(Label label, final LabelEnv env) {
        return env.triggerTransforms(label);
    }

    protected List<Equation> getQueue() {
        return Collections.unmodifiableList(Q.list);
    }

    protected void addEquationToQueue(Equation eqn) {
        Q.add(eqn);
    }

    protected void addEquationToQueueHead(Equation eqn) {
        Q.addFirst(eqn);
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
    public void setBound(VarLabel v, Label newBound,
            LabelConstraint responsible) throws SemanticException {
        bounds.setBound(v, newBound);
    }

    public void setBound(VarPrincipal v, Principal newBound,
            PrincipalConstraint responsible) {
        bounds.setBound(v, newBound);
    }

    /**
     * Solve the system of constraints. If the system has already been solved,
     * then returned the cached solution.
     * 
     * @throws SemanticException if the Solver cannot find a solution to the
     *             system of contraints.
     */
    @Override
    public VarMap solve() throws SemanticException {

        // Cache the solution.
        if (status == STATUS_SOLVED || status == STATUS_NO_SOLUTION) {
            return bounds;
        }
        if (status == STATUS_SOLVING) {
            throw new InternalCompilerError("solve called on solver while "
                    + "in the process of solving.");
        }

        // status at this point is STATUS_NOT_SOLVED
        setStatus(STATUS_SOLVING);

        //bounds = new VarMap(ts, getDefaultBound());

        if (shouldReport(1)) {
            report(1, "===== Starting solver " + solverName + " =====");
            report(1, "   " + equations.size() + " equations");
        }

        // check for static failures.
        if (staticFailedConstraints != null
                && !staticFailedConstraints.isEmpty()) {
            if (shouldReport(1)) {
                report(1, "   " + staticFailedConstraints.size()
                        + " statically failed constraint");
            }
            setStatus(STATUS_NO_SOLUTION);
            for (Iterator<Equation> iter =
                    staticFailedConstraints.iterator(); iter.hasNext();) {
                UnsatisfiableConstraintException ex = reportError(iter.next());
                // generate error report for all exceptions
                genFlowMessage(ex);
                if (!iter.hasNext()) {
                    throw ex;
                }
                // add all but the last to the queue.
                compiler.errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                        ex.getMessage(), ex.position());
            }
        }

        if (useSCC) {
            Pair<Equation[], int[]> pair = findSCCs();
            Equation[] by_scc = pair.part1();
            int[] scc_head = pair.part2();

            scc = new LinkedList<Set<Equation>>();
            Set<Equation> currentScc = null;
            for (int i = 0; i < scc_head.length; i++) {
                if (scc_head[i] == -1) {
                    // it's the start of a new scc
                    // add what we've already gathered to the set of strongly connected
                    // components
                    if (currentScc != null) {
                        scc.add(currentScc);
                        currentScc = null;
                    }
                }
                if (currentScc == null)
                    currentScc = new LinkedHashSet<Equation>();
                currentScc.add(by_scc[i]);
            }
            if (currentScc != null) {
                scc.add(currentScc);
                currentScc = null;
            }
        } else {
            // not using SCC, so pretend that
            // we have a single SCC consisting of all equations
            scc = new LinkedList<Set<Equation>>();
            scc.add(new LinkedHashSet<Equation>(equations));
        }

        // pre-initialize the queue with the first strongly connected
        // component
        if (scc.isEmpty()) {
            currentSCC = Collections.emptySet();
            Q = new EquationQueue();
        } else {
            currentSCC = scc.removeFirst();
            Q = new EquationQueue(currentSCC);
        }

        try {
            VarMap soln = solve_bounds();
            setStatus(STATUS_SOLVED);

            if (shouldReport(1)) {
                report(1, "   finished " + solverName);
            }
            return soln;
        } catch (SemanticException e) {
            setStatus(STATUS_NO_SOLUTION);
            genFlowMessage((UnsatisfiableConstraintException) e);
            throw e;
        }
    }

    public void genFlowMessage(UnsatisfiableConstraintException ex) {
        if (LabelFlowGraph.shouldReport(LabelFlowGraph.messageOnly)) {
            LabelFlowGraph g = new LabelFlowGraph(fullTrace, ex.getSnapshot());
            g.showErrorPath();
            if (LabelFlowGraph.shouldReport(LabelFlowGraph.showSlicedGraph))
                g.writeToDotFile();
        }
    }

    // this function outputs the dependency recorded in the map eqnVarDependency and varEqnDependency into a graph
//    public void dependencyToGraph ( ) {
//        String filename = solverName + ".dot";
//
//        try {
//            FileWriter fstream = new FileWriter(filename);
//            BufferedWriter out = new BufferedWriter(fstream);
//
//            out.write("digraph G1 {\n");
//            // set the fill color of nodes
//            out.write("\tnode [color = grey, style = filled];\n");
//
//            // first, generate all nodes in the graph
//            int refCounter = 0;
//            Map<Constraint, String> nameMap = new HashMap<Constraint, String>();
//
//            String nodes = ""; // keep track of all nodes appeared in the graph
//            Set vars = new HashSet();
//            Set links = new HashSet();
//
//            // now output all the links
//            for (Object o: eqnVarDependencies.keySet()) {
//                LabelEquation equ = (LabelEquation) o;
//                if (!nameMap.keySet().contains(equ.constraint)) {
//                    // create a new node and assign it a fresh name
//                    String name = "Equ"+refCounter;
//                    nameMap.put(equ.constraint, name);
//                    nodes += "\t" + name + " [label=\""
//                          + equ.constraint.lhs.toString()
//                          + equ.constraint.kind().toString()
//                          + equ.constraint.rhs.toString()
//                          + equ.constraint.pos.toString()
//                          + "\""
//                          + (failedEquations.contains(equ.constraint)? ", color = red " : "")
//                          + "];\n";
//                    refCounter ++;
//                }
//                for (Object o1 : (Set) eqnVarDependencies.get(equ)) {
//                    Variable var = (Variable) o1;
//                    if (!vars.contains(var))
//                        vars.add(var);
//                    links.add(nameMap.get(equ.constraint) + "->" + var.name());
//                }
//            }
//
//            for (Object o: varEqnDependencies.keySet()) {
//                Variable var = (Variable) o;
//                if (!vars.contains(var))
//                    vars.add(var);
//                for (Object o1: (Set) varEqnDependencies.get(var)) {
//                    LabelEquation equ = (LabelEquation) o1;
//                    if (!nameMap.keySet().contains(equ.constraint)) {
//                        // create a new node and assign it a fresh name
//                        String name = "Equ"+refCounter;
//                        nameMap.put(equ.constraint, name);
//                        nodes += "\t" + name + " [label=\""
//                              + equ.constraint.lhs.toString()
//                              + equ.constraint.kind().toString()
//                              + equ.constraint.rhs.toString()
//                              + equ.constraint.pos.toString() + "\"];\n";
//                        refCounter ++;
//                    }
//                    links.add(var.name() + "->" + nameMap.get(equ.constraint) );
//                }
//            }
//
//            // output all nodes to the graph
//            out.write (nodes);
//            for (Object o : vars) {
//                Variable v = (Variable) o;
//                out.write( "\t" + v.name() + "\n");
//            }
//            // output all links
//            for (Object o : links) {
//                String s = (String) o;
//                out.write( "\t" + s + ";\n");
//            }
//            out.write("}");
//            //Close the output stream
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            System.out.println("Unable to write to file: "+filename);
//        }
//    }

    // this function accepts a trace of refinements for variable's labels, and visualize it
//    public void traceToGraph ( ) {
    // add the failed constraint to the graph
//        DependencyGraph g = new DependencyGraph(fullTrace, failedConstraints, bounds);
//        g.writeToDotFile(solverName + ".dot");

//        String fileName = solverName + ".dot";
//        System.out.println( "Failed constraints #: "+failedConstraints.size());
//        for (Object o : failedConstraints)
//            System.out.println(((Constraint)o).toString());
//        Map varNameMap = new HashMap();
//        Map unchangedVarNameMap = new HashMap();
//
//        try {
//            FileWriter fstream = new FileWriter(fileName);
//            BufferedWriter out = new BufferedWriter(fstream);
//            out.write("digraph G1 {\n");
//            out.write("\tnode [color = grey, style = filled];\n");
//
//            // this is used to store the string for each variable
//            Map<VarLabel, String> strings = new HashMap<VarLabel, String>();
//            String gString = ""; // this is a global string
//
//            int varCounter = 0;
//            int refCounter = 0;
//
//            // first, get all var labels in the trace
//            for (Object o : fullTrace) {
//                Trace tr = (Trace) o;
//                if ( !strings.keySet().contains(tr.label)) {
//                    varNameMap.put(tr.label, "v"+varCounter+"_");
//                    strings.put(tr.label, "");
//                    System.out.println(tr.label.toString());
//                    strings.put(tr.label, "\tsubgraph cluster_" + varCounter
//                            + " {\n" + "\t\tlabel = \"" + tr.label.name()
//                            + "\";\n" + "\t\t" + varNameMap.get(tr.label) + refCounter
//                            + " [label=\"bot\"];\n");
//                    varCounter++;
//                }
//            }
//
//            for (Object o : fullTrace) {
//                Trace tr = (Trace) o;
//                refCounter ++;
//
//                for (Object o1 : strings.keySet()) {
//                    VarLabel var1 = (VarLabel) o1;
//
//                    // this is the var whose label is raised
//                    if (tr.label == var1) {
//                        // generated the refined label
//                        strings.put(var1, strings.get(var1) + "\t\t"
//                                    + varNameMap.get(var1) + refCounter + " [label=\""
//                                    + tr.to.toString() + "\"];\n");
//
//                        String label = tr.equ.lhs().toString()
//                                        + tr.equ.constraint.kind().toString()
//                                        + tr.equ.rhs().toString()
//                                        + tr.equ.position().toString();
//
//                        // first, label the refinement using the constraint
//                        strings.put(var1, strings.get(var1) + "\t\t"
//                                + varNameMap.get(var1) + (refCounter - 1)
//                                + "->" + varNameMap.get(var1) + refCounter
//                                + " [ " + "label=\"" + label + "\" ];\n");
//
//                        // add links from variables appeared in the lhs of the equation
//                        for (Object o3 : tr.equ.variables()) {
//                            if (o3 != tr.label) {
//                                VarLabel var2 = (VarLabel) o3;
//                                if (!varNameMap.containsKey(var2)) {
//                                    if (!unchangedVarNameMap.containsKey(var2)) {
//                                        unchangedVarNameMap.put(var2, "v"+ varCounter);
//                                        varCounter++;
//                                        gString = gString + "\t\t"
//                                                        + unchangedVarNameMap
//                                                                .get(var2)
//                                                        + " [label=\""
//                                                        + var2.toString()
//                                                        + "\"];\n";
//                                    }
//                                    gString = gString + "\t\t"
//                                                    + unchangedVarNameMap.get(var2)
//                                                    + "->"
//                                                    + varNameMap.get(var1) + refCounter + " [ "
//                                                    + "label=\"" + label
//                                                    + "\" ];\n";
//                                } else gString = gString + "\t\t" + varNameMap.get(var2)
//                                                + (refCounter - 1) + "->"
//                                                + varNameMap.get(var1)
//                                                + refCounter + " [ "
//                                                + "label=\"" + label
//                                                + "\" ];\n";
//                            }
//                        }
//                    } else {
//                        strings.put(var1, strings.get(var1) + "\t\t"
//                                        + varNameMap.get(var1) + refCounter
//                                        + " [label=\"\"];\n");
//                        strings.put(var1, strings.get(var1) + "\t\t"
//                                + varNameMap.get(var1) + (refCounter - 1) + "->"
//                                + varNameMap.get(var1) + refCounter + ";\n");
//                    }
//                }
//            }
//
//            // add failed constrain to the graph
//            gString = gString + "\t\t" + "FAIL [color = red];\n";
//            for (Object o : failedConstraints) {
//                LabelConstraint c = (LabelConstraint) o;
//                String label = c.lhs.toString()
//                                + c.kind().toString()
//                                + c.rhs.toString()
//                                + c.position().toString();
//
//                for (Object o1 : (c.lhsLabel().variables())) {
//                    Variable v = (Variable) o1;
//                    gString = gString + "\t\t" + varNameMap.get(v)
//                                    + refCounter + "->"
//                                    + "FAIL" + " [ "
//                                    + "label=\"" + label + "\" ];\n";
//                }
//            }
//
//            // write the subgraphs
//            for (Object o : strings.keySet()) {
//                VarLabel var1 = (VarLabel) o;
//                out.write(strings.get(var1));
//                out.write("\t}\n");
//            }
//            // write the global part
//            out.write(gString);
//
//            out.write("}");
//            // Close the output stream
//            out.close();
//        } catch (IOException e) {
//            System.out.println("Unable to write to file: " + fileName);
//        }
//    }

    protected void setStatus(int status) {
        this.status = status;
    }

    /**
     * This method must return a constant label, which is the default bound of
     * label variables.
     */
    protected abstract Label getDefaultLabelBound();

    /**
     * This method must return a constant principal, which is the default bound of
     * principal variables.
     */
    protected abstract Principal getDefaultPrincipalBound();

    /**
     * Solve the system of constraints, by finding upper bounds for the label
     * variables.
     * 
     * @return a solution to the system of constraints, in the form of a VarMap
     *         of the upper bounds of the label variables.
     * @throws SemanticException if the Solver cannot find a solution to the
     *             system of constraints.
     */
    protected VarMap solve_bounds() throws SemanticException {
        // Solve the system of constraints. bounds may already contain a
        // partial solution, in which case attempt to complete the solution.

        if (shouldReport(3)) {
            report(3, "======EQUATIONS======");
            for (Equation eqn : equations) {
                report(3, eqn.toString());
            }
        }

        int counter = 0;

        if (Q.isEmpty()) checkCandidateSolution();

        while (!Q.isEmpty()) {
            while (!Q.isEmpty()) {
                counter++;
                Equation eqn = Q.removeFirst();
                considerEquation(eqn);

                // if we finished the last strongly connected component
                // move to the next one
                //
                // done this way instead of an outer loop because of the
                // way the search method works
                if (Q.isEmpty() && !scc.isEmpty()) {
                    currentSCC = scc.removeFirst();
                    Q.addAll(currentSCC);
                }
            } // end while

            checkCandidateSolution();
        }
        if (shouldReport(2))
            report(2, "Number of relaxation steps: " + counter);
        if (shouldReport(2)) {
            report(2, bounds.toString());
        }
        return bounds;
    }

    protected void considerEquation(Equation eqn) throws SemanticException {
        if (eqn instanceof LabelEquation) {
            considerEquation((LabelEquation) eqn);
        } else if (eqn instanceof PrincipalEquation) {
            considerEquation((PrincipalEquation) eqn);
        } else {
            throw new InternalCompilerError("Unexpected eqn " + eqn);
        }
    }

    protected void considerEquation(LabelEquation eqn)
            throws SemanticException {
        Label lhsbound =
                triggerTransforms(bounds.applyTo(eqn.lhs()), eqn.env());
        Label rhsbound =
                triggerTransforms(bounds.applyTo(eqn.rhs()), eqn.env());

        if (eqn.env().leq(lhsbound, rhsbound)) {
            if (shouldReport(5)) report(5, "constraint: " + eqn
                    + " already satisfied: " + lhsbound + "<=" + rhsbound);
        } else {
            if (shouldReport(4)) {
                report(4,
                        "Considering constraint: " + eqn + " ("
                                + (eqn.position() == null ? "null"
                                        : ("line " + eqn.position().line()))
                                + ")");
            }
            // let the subclass deal with changing the bounds on
            // variables
            // to make this equation satisfied.
            solve_eqn(eqn);
        }
    }

    /**
     * This method changes the bounds of variables in the Equation eqn, to make
     * the equation satisfied. The method may postpone solving the equation by
     * putting the equation back on the queue, using addEquationToQueue().
     */
    protected abstract void solve_eqn(LabelEquation eqn)
            throws SemanticException;

    protected void considerEquation(PrincipalEquation eqn)
            throws SemanticException {
        Principal lhsbound = bounds.applyTo(eqn.lhs());
        Principal rhsbound = bounds.applyTo(eqn.rhs());

        if (eqn.env().actsFor(lhsbound, rhsbound)) {
            if (shouldReport(5))
                report(5, "constraint: " + eqn + " already satisfied: "
                        + lhsbound + " actsfor " + rhsbound);
        } else {
            if (shouldReport(4)) {
                report(4,
                        "Considering constraint: " + eqn + " ("
                                + (eqn.position() == null ? "null"
                                        : ("line " + eqn.position().line()))
                                + ")");
            }
            // let the subclass deal with changing the bounds on
            // variables
            // to make this equation satisfied.
            solve_eqn(eqn);
        }
    }

    /**
     * This method changes the bounds of variables in the Equation eqn, to make
     * the equation satisfied. The method may postpone solving the equation by
     * putting the equation back on the queue, using addEquationToQueue().
     */
    protected abstract void solve_eqn(PrincipalEquation eqn)
            throws SemanticException;

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
        for (Equation eqn : equations) {
            if (eqn instanceof LabelEquation) {
                checkEquationSatisfied((LabelEquation) eqn);
            }
        }
    }

    protected void checkEquationSatisfied(LabelEquation eqn)
            throws SemanticException {
        // Check that any variables that must be runtime representable are in fact so.
        for (Variable v : eqn.variables()) {
            if (v.mustRuntimeRepresentable()) {
                boolean isRuntimeRepresentable = false;
                if (v instanceof VarLabel) {
                    isRuntimeRepresentable = bounds.boundOf((VarLabel) v)
                            .isRuntimeRepresentable();
                } else if (v instanceof VarPrincipal) {
                    isRuntimeRepresentable = bounds.boundOf((VarPrincipal) v)
                            .isRuntimeRepresentable();
                } else {
                    throw new InternalCompilerError("Unexpected variable " + v);
                }
                if (!isRuntimeRepresentable) {
                    // a variable that must be runtime representable is not.
                    reportTrace(v);
                    throw new SemanticException(
                            v + " must be runtime representable in equation "
                                    + eqn,
                            eqn.position());
                }
            }
        }

        Label lhsBound =
                triggerTransforms(bounds.applyTo(eqn.lhs()), eqn.env());
        Label rhsBound =
                triggerTransforms(bounds.applyTo(eqn.rhs()), eqn.env());

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
            throw reportError(eqn);
        }
    }

    /**
     * Awakens all equations in the system that depend on the variable v,
     * ensuring that they are in the queue of active equations.
     */
    protected final void wakeUp(Variable v) {
        Set<Equation> eqns = varEqnDependencies.get(v);

        if (eqns != null) {
            for (Equation eqn : eqns) {
                // if its in the current strongly connected set
                // and its not in the Queue, add it
                if (!Q.contains(eqn) && (!useSCC || currentSCC.contains(eqn))) // offensive but simple
                    Q.add(eqn);
            }
        }
    }

    public String solverName() {
        return this.solverName;
    }

    /**
     * Counter of the number of constraints added to the system. For debugging
     * purposes.
     */
    static protected int constraint_counter = 0;

    /**
     * Increase the count of the number of constraints added (not just to this
     * system, but to all instances of the Solver).
     * 
     * For debugging purposes, if the constraint counter is equal to the
     * stop_constraint, then a RuntimeException is thrown.
     */
    protected final void inc_counter() {
        constraint_counter++;
    }

    protected boolean isFixedValueVar(Variable v) {
        return fixedValueVars.contains(v);
    }

    /**
     * Add the constraint c to the system
     */
    @Override
    public void addConstraint(Constraint c) throws SemanticException {
        if (status != STATUS_NOT_SOLVED) {
            throw new InternalCompilerError("Computed solution already. "
                    + "Cannot add more constraints");
        }

        if (shouldReport(3)) {
            StackTraceElement[] stack = new Exception().getStackTrace();
            String source =
                    stack[4].getFileName() + ":" + stack[4].getLineNumber();
            report(3, (constraint_counter + 1) + ": " + c + " << " + source);
        }
        if (shouldReport(6)) report(6, ">>> " + c.msg());
        inc_counter();

        if (!c.isCanonical()) {
            throw new InternalCompilerError(c.position(),
                    "Constraint is not canonical.");
        }

        if (c instanceof LabelConstraint) {
            LabelConstraint lc = (LabelConstraint) c;
            if (lc.lhsLabel() instanceof NotTaken
                    && lc.kind() == LabelConstraint.LEQ) {
                // if the LHS is NotTaken, then the constraint will always be
                // satisfied.
                return;
            }

            if (lc.rhsLabel() instanceof NotTaken
                    && lc.kind() == LabelConstraint.LEQ) {
                // if the RHS is NotTaken (and the LHS isn't), then the
                // constraint can never be satisfied.
                LabelEquation eqn =
                        new LabelEquation(lc.lhsLabel(), lc.rhsLabel(), lc);
                reportError(eqn);
            }
        }
        processConstraint(c);
        addConstraintEquations(c);
    }

    /**
     * Perform any special processing for the label constraint
     */
    protected void processConstraint(Constraint c) throws SemanticException {
        if (c instanceof LabelConstraint) {
            LabelConstraint lc = (LabelConstraint) c;
            if (lc.lhsLabel() instanceof VarLabel
                    && lc.kind() == LabelConstraint.EQUAL) {
                // this is an equality constraint on a variable. Let's jump start the
                // solving by setting it immediately
                VarLabel v = (VarLabel) lc.lhsLabel();
                Label initialBound = bounds.applyTo(lc.rhsLabel());
                addTrace(v, lc.rhsLabel(), lc.getEquations().iterator().next(),
                        initialBound, InformationFlowTrace.Direction.BOTH);
                setBound(v, initialBound, lc);
                // only add the variable to the fixed value vars if the RHS does not contain
                // any variables. Otherwise, the bound of v may need to change
                // as the RHS changes.
                if (!lc.rhsLabel().hasVariableComponents()) {
                    fixedValueVars.add(v);
                }
            }
        } else if (c instanceof PrincipalConstraint) {
            PrincipalConstraint pc = (PrincipalConstraint) c;
            if ((pc.lhsPrincipal() instanceof VarPrincipal
                    || pc.rhsPrincipal() instanceof VarPrincipal)
                    && pc.kind() == PrincipalConstraint.EQUIV) {
                // this is an equality constraint on a variable. Let's jump start the
                // solving by setting it immediately

                VarPrincipal v = null;
                Principal other = null;
                if (pc.lhsPrincipal() instanceof VarPrincipal) {
                    v = (VarPrincipal) pc.lhsPrincipal();
                    other = pc.rhsPrincipal();
                } else {
                    v = (VarPrincipal) pc.rhsPrincipal();
                    other = pc.lhsPrincipal();
                }

                Principal initialBound = bounds.applyTo(other);
                setBound(v, initialBound, pc);
                // only add the variable to the fixed value vars if other does not contain
                // any variables. Otherwise, the bound of v may need to change
                // as the other changes.
                if (!other.hasVariables()) {
                    fixedValueVars.add(v);
                }
            }
        }
    }

    /**
     * Go through each equation in the constraint, add the equation
     * if needed, and register dependencies for the equation.
     * @param c
     * @throws SemanticException
     */
    protected void addConstraintEquations(Constraint c)
            throws SemanticException {
        Collection<Equation> eqns = c.getEquations();
        for (Equation eqn : eqns) {
            LabelEnv eqnEnv = eqn.env();
            if (!eqnEnv.hasVariables() && !eqn.constraint().hasVariables()) {
                // The equation has no variables. We can check now if it is
                // satisfied or not
                boolean eqnSatisfied = false;
                if (eqn instanceof LabelEquation) {
                    LabelEquation leqn = (LabelEquation) eqn;
                    eqnSatisfied =
                            eqnEnv.leq(triggerTransforms(leqn.lhs(), eqnEnv),
                                    triggerTransforms(leqn.rhs(), eqnEnv));
                } else if (eqn instanceof PrincipalEquation) {
                    PrincipalEquation peqn = (PrincipalEquation) eqn;
                    eqnSatisfied = eqnEnv.actsFor(peqn.lhs(), peqn.rhs());
                } else {
                    throw new InternalCompilerError(
                            "Unexpected kind of equation: " + eqn);
                }
                if (!eqnSatisfied) {
                    if (shouldReport(2)) {
                        report(2, "Statically failed " + eqn);
                    }
                    if (shouldReport(3) && eqn instanceof LabelEquation) {
                        report(3, "Statically failed "
                                + triggerTransforms(((LabelEquation) eqn).lhs(),
                                        eqnEnv)
                                + " <= " + triggerTransforms(
                                        ((LabelEquation) eqn).rhs(), eqnEnv));
                    }

                    // The equation is not satisfied.
                    if (THROW_STATIC_FAILED_CONSTRAINTS) {
                        throw reportError(eqn);
                    } else {
                        if (staticFailedConstraints == null) {
                            staticFailedConstraints =
                                    new LinkedHashSet<Equation>();
                        }
                        staticFailedConstraints.add(eqn);
                    }
                } else {
                    // The equation is satisfied, no need to add it to
                    // the queue.
                }
            } else {
                if (shouldReport(5)) report(5, "Adding equation: " + eqn);
                eqnEnv.setSolver(this);
                equations.add(eqn);
                addDependencies(eqn);
            }

        }
    }

    /**
     * This abstract method must add the correct dependencies from Equation eqn
     * to variables occurring in eqn, and dependencies in the other direction
     * (that is, from variables occurring in eqn to eqn).
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
    protected void addDependency(Variable var, Equation eqn) {
        Set<Equation> eqns = varEqnDependencies.get(var);
        if (eqns == null) {
            eqns = new LinkedHashSet<Equation>();
            varEqnDependencies.put(var, eqns);
        }
        eqns.add(eqn);

        Set<Variable> vars = eqnVarReverseDependencies.get(eqn);
        if (vars == null) {
            vars = new LinkedHashSet<Variable>();
            eqnVarReverseDependencies.put(eqn, vars);
        }
        vars.add(var);

    }

    /**
     * This method records a dependency from Equation eqn to variable var. This
     * method should only be called by subclasses during the execution of the
     * method addDependencies().
     * 
     * There is a dependency from Equation eqn to variable var if the bound on
     * var may be modified as a result of solving eqn.
     */
    protected void addDependency(Equation eqn, Variable var) {
        Set<Variable> vars = eqnVarDependencies.get(eqn);
        if (vars == null) {
            vars = new LinkedHashSet<Variable>();
            eqnVarDependencies.put(eqn, vars);
        }
        vars.add(var);

        Set<Equation> eqns = varEqnReverseDependencies.get(var);
        if (eqns == null) {
            eqns = new LinkedHashSet<Equation>();
            varEqnReverseDependencies.put(var, eqns);
        }
        eqns.add(eqn);
    }

    /**
     * Returns the equations that are dependent on the equation eqn by finding
     * the variables that eqn may alter if it is solved (useing the map
     * eqnVarDependencies), and then finds the equations that depend on those
     * variables (using the map varEqnDependencies)
     */
    protected Set<Equation> eqnEqnDependencies(Equation eqn) {
        Set<Variable> vars = eqnVarDependencies.get(eqn);

        if (vars == null || vars.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Equation> eqns = new LinkedHashSet<Equation>();
        for (Variable v : vars) {
            Set<Equation> s = varEqnDependencies.get(v);
            if (s != null) {
                eqns.addAll(s);
            }
        }
        return eqns;
    }

    /**
     * Returns the equations that are reverse dependent on the equation eqn by finding
     * the variables that may invalidate eqn (using the map
     * eqnVarReverseDependencies), and then finding the equations
     * that may alter those variables (using the map varEqnReverseDependencies)
     */
    protected Set<Equation> eqnEqnReverseDependencies(Equation eqn) {
        Set<Variable> vars = eqnVarReverseDependencies.get(eqn);

        if (vars == null || vars.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Equation> eqns = new LinkedHashSet<Equation>();
        for (Variable v : vars) {
            Set<Equation> s = varEqnReverseDependencies.get(v);
            if (s != null) {
                eqns.addAll(s);
            }
        }
        return eqns;
    }

    /**
     * Record the fact that label variable v, in the constraint eqn had its
     * bound set to the label lb.
     */
    protected final void addTrace(VarLabel v, Label sourcelabel, Equation eqn,
            Label lb, Direction dir) {
        fullTrace.add(new InformationFlowTrace(v, sourcelabel, dir,
                (LabelEquation) eqn));

        List<Pair<Equation, Label>> trace = traces.get(v);
        if (trace == null) {
            trace = new LinkedList<Pair<Equation, Label>>();
            traces.put(v, trace);
        }
        trace.add(new Pair<Equation, Label>(eqn, lb.copy()));
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
        List<Pair<Equation, Label>> history = traces.get(var);
        if (history != null) {
            for (Pair<Equation, Label> eqn_label : history) {
                Label label = eqn_label.part2();
                Equation eqn = eqn_label.part1();
                boolean test = lowerThreshold ? eqn.env().leq(threshold, label)
                        : eqn.env().leq(label, threshold);
                if (!test) {
                    return eqn;
                }
            }
        }
        return null;
    }

    protected Equation findContradictiveEqn(Constraint c) {
        if (c instanceof LabelConstraint) {
            return findContradictiveEqn((LabelConstraint) c);
        }
        throw new InternalCompilerError(
                "Unexpected constraint type: " + c.getClass());
    }

    protected abstract Equation findContradictiveEqn(LabelConstraint c);

    /** Returns the pair [by_scc, scc_head] where
     *  by_scc is an array in which SCCs occur in topologically
     *  order.
     *  scc_head[n] where n is the first peer in an SCC is set to -1.
     *  scc_head[n] where n is the last peer in a (non-singleton) SCC is set
     *  to the index of the first peer. Otherwise it is -2.
     * 
     *   by_scc contains the peers grouped by SCC.
     *   scc_head marks where the SCCs are. The SCC
     *    begins with a -1 and ends with the index of
     *     the beginning of the SCC.
     *  */
    protected Pair<Equation[], int[]> findSCCs() {

        Equation[] sorted = new Equation[equations.size()];

        // First, topologically sort the nodes (put in postorder)
        int n = 0;
        LinkedList<Frame> stack = new LinkedList<Frame>();
        Set<Equation> reachable = new HashSet<Equation>();
        for (Equation eq : equations) {
            if (!reachable.contains(eq)) {
                reachable.add(eq);
                stack.addFirst(new Frame(eq, true));
                while (!stack.isEmpty()) {
                    Frame top = stack.getFirst();
                    if (top.edges.hasNext()) {
                        Equation eqTo = top.edges.next();
                        if (!reachable.contains(eqTo)) {
                            reachable.add(eqTo);
                            stack.addFirst(new Frame(eqTo, true));
                        }
                    } else {
                        stack.removeFirst();
                        sorted[n++] = top.eqn;
                    }
                }
            }
        }

//      Now, walk the transposed graph picking nodes in reverse
//      postorder, thus picking out one SCC at a time and
//      appending it to "by_scc".
        Equation[] by_scc = new Equation[n];
        int[] scc_head = new int[n];
        Set<Equation> visited = new HashSet<Equation>();
        int head = 0;
        for (int i = n - 1; i >= 0; i--) {
            if (!visited.contains(sorted[i])) {
                // First, find all the nodes in the SCC
                Set<Equation> SCC = new HashSet<Equation>();
                visited.add(sorted[i]);
                stack.add(new Frame(sorted[i], false));
                while (!stack.isEmpty()) {
                    Frame top = stack.getFirst();
                    if (top.edges.hasNext()) {
                        Equation eqTo = top.edges.next();
                        if (reachable.contains(eqTo)
                                && !visited.contains(eqTo)) {
                            visited.add(eqTo);
                            Frame f = new Frame(eqTo, false);
                            stack.addFirst(f);
                        }
                    } else {
                        stack.removeFirst();
                        SCC.add(top.eqn);
                    }
                }
                // Now, topologically sort the SCC (as much as possible)
                // and place into by_scc[head..head+scc_size-1]
                stack.add(new Frame(sorted[i], true));
                Set<Equation> revisited = new HashSet<Equation>();
                revisited.add(sorted[i]);
                int scc_size = SCC.size();
                int nsorted = 0;
                while (stack.size() != 0) {
                    Frame top = stack.getFirst();
                    if (top.edges.hasNext()) {
                        Equation eqTo = top.edges.next();
                        if (SCC.contains(eqTo) && !revisited.contains(eqTo)) {
                            revisited.add(eqTo);
                            Frame f = new Frame(eqTo, true);
                            stack.addFirst(f);
                        }
                    } else {
                        stack.removeFirst();
                        int n3 = head + scc_size - nsorted - 1;
                        scc_head[n3] = -2;
                        by_scc[n3] = top.eqn;
                        nsorted++;
                    }
                }
                scc_head[head + scc_size - 1] = head;
                scc_head[head] = -1;
                head = head + scc_size;
            }
        }
//        for (int j = 0; j < n; j++) {
//            switch(scc_head[j]) {
//            case -1: Report.report(2, j + "[HEAD] : " + by_scc[j]); break;
//            case -2: Report.report(2, j + "       : " + by_scc[j]); break;
//            default: Report.report(2, j + " ->"+ scc_head[j] + " : " + by_scc[j]);
//            }
//            for (Iterator i = eqnEqnDependencies(by_scc[j]).iterator(); i.hasNext(); ) {
//                Report.report(3, "     successor: " + ((Equation)i.next()));
//            }
//        }
        return new Pair<Equation[], int[]>(by_scc, scc_head);
    }

    protected class Frame {
        Equation eqn;
        Iterator<Equation> edges;

        Frame(Equation e, boolean forward) {
            eqn = e;
            if (forward) {
                edges = eqnEqnDependencies(e).iterator();
            } else {
                edges = eqnEqnReverseDependencies(e).iterator();
            }
        }
    }

    /**
     * A queue for equations. This class ensures that an equation
     * is in the queue at most once.
     */
    protected static class EquationQueue {
        final LinkedList<Equation> list;
        final Set<Equation> elements;

        public EquationQueue() {
            list = new LinkedList<Equation>();
            elements = new HashSet<Equation>();
        }

        public EquationQueue(Collection<Equation> c) {
            list = new LinkedList<Equation>(c);
            elements = new HashSet<Equation>(c);
        }

        public EquationQueue(EquationQueue q) {
            list = new LinkedList<Equation>(q.list);
            elements = new HashSet<Equation>(q.elements);
        }

        public boolean contains(Equation eqn) {
            return elements.contains(eqn);
        }

        public void addAll(Collection<Equation> c) {
            if (c != null) {
                for (Equation e : c) {
                    add(e);
                }
            }
        }

        public Equation removeFirst() {
            Equation e = list.removeFirst();
            elements.remove(e);
            return e;
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }

        public void add(Equation eqn) {
            if (!elements.contains(eqn)) {
                list.add(eqn);
                elements.add(eqn);
            }
        }

        public void addFirst(Equation eqn) {
            if (elements.contains(eqn)) {
                // already in the queue. remove it.
                list.remove(eqn);
            }
            list.addFirst(eqn);
            elements.add(eqn);
        }
    }

    /**
     * Report an unsatisfiable constraint
     * 
     * @param  eqn
     *          The equation that is unsatisfiable
     * @return
     *          a suitable exception to indicate the failure
     */
    protected UnsatisfiableConstraintException reportError(Equation eqn) {
        for (Variable v : eqn.variables())
            reportTrace(v);
        Equation reporteqn = (Equation) eqn.copy();
        bounds.applyTo(reporteqn);
        return new UnsatisfiableConstraintException(this, reporteqn,
                new FailedConstraintSnapshot(eqn, bounds.copy()));
    }

    /** Report a trace for a given variable. */
    protected void reportTrace(Variable v) {
        /* This code was commented out in r1.5 before it was moved here.
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
         */
    }
}
