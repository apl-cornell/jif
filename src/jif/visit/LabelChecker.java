package jif.visit;

import jif.ast.Jif;
import jif.ast.JifClassDecl;
import jif.ast.JifMethodDecl;
import jif.types.*;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.util.Copy;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;

/** 
 * The <code>LabelChecker</code> class is used in the label checking of
 * Jif. Primarily it provides the method {@link #labelCheck(Node) 
 * labelCheck(Node)} which invokes the {@link jif.ast.Jif#labelCheck labelCheck}
 * method on nodes, utility functions to help in the implementation of the
 * <code>labelCheck</code> methods, as well as references to the appropriate
 *  {@link jif.types.Solver Solver} and {@link jif.types.JifContext JifContext}.
 * 
 * <p>
 * <code>LabelChecker</code> is mostly imperative; however, the 
 * <code>JifContext</code> is treated functionally, and whenever a 
 * <code>LabelChecker</code> is given a new <code>JifContext</code>, a new
 *  <code>LabelChecker</code> is created.
 */
public class LabelChecker implements Copy
{
    /**
     * The <code>JifContext</code> appropriate for this label checker. The
     * Jif context records a substantial amount of information required for
     * label checking.
     */
    private JifContext context;
    
    final private JifTypeSystem ts;
    final private Job job;    
    final private NodeFactory nf;

    /**
     * If true, then a new system of constraints will be used for each 
     * class body, and upon leaving the class body, the system of constraints
     * will be solved.
     */
    final private boolean solvePerClassBody;
    
    /**
     * If true, then a new system of constraints will be used for each 
     * method body, and upon leaving the method body, the system of constraints
     * will be solved.
     */
    final private boolean solvePerMethod;
    
    /**
     * The <code>Solver</code> to add constraints to. Depending on 
     * <code>solveClassBodies</code>, a new <code>Solver</code> is used
     * for every class, or a single <code>Solver</code> used for the entire 
     * compilation. 
     */
    private Solver solver;

    /**
     * If true, then upon solving the system of constraints, a label 
     * substitution pass will be performed, replacing variables with
     * the inferred labels.
     */
    final private boolean doLabelSubst;

    public LabelChecker(Job job, TypeSystem ts, NodeFactory nf, boolean solvePerClassBody, boolean solvePerMethod) {
        this(job, ts, nf, solvePerClassBody, solvePerMethod, true);
    }

    public LabelChecker(Job job, TypeSystem ts, NodeFactory nf, boolean solvePerClassBody, boolean solvePerMethod, boolean doLabelSubst) {
        this.job = job;
        this.ts = (JifTypeSystem) ts;
        this.context = (JifContext) ts.createContext();
        this.nf = nf;

        this.solvePerClassBody = solvePerClassBody;
        this.solvePerMethod = solvePerMethod;
//        if (solvePerMethod && solvePerClassBody) {
//            throw new InternalCompilerError("cant solve for both class and method!");
//        }
        this.doLabelSubst = true;
        if (!solvePerClassBody && !solvePerMethod) {
            this.solver = this.ts.createSolver("Job solver: " + job.toString());
        }
    }

    public Object copy() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalCompilerError("Java clone() weirdness.", e);
        }
    }

    public JifContext context() {
        return context;
    }
    public JifContext jifContext() {
        return context;
    }

    public LabelChecker context(JifContext c) {
        if (c == this.context) return this;
        LabelChecker lc = (LabelChecker) copy();
        lc.context = c;
        return lc;
    }

    public JifTypeSystem typeSystem() {
        return ts;
    }
    public JifTypeSystem jifTypeSystem() {
        return ts;
    }

    public NodeFactory nodeFactory() {
	return nf;
    }

    public Solver solver() {
        return this.solver;
    }

    public Job job() {
        return this.job;
    }

    public ErrorQueue errorQueue() {
        return job.compiler().errorQueue();
    }
    
    
    /**
     * Returns an upper bound for L1 and L2
     */
    public Label upperBound(Label L1, Label L2) {
        return ts.join(L1, L2);
    }

    public Label upperBound(Label L1, Label L2, Label L3) {
        return ts.join(ts.join(L1, L2), L3);
    }

    /**
     * Returns a lower bound for L1 and L2
     */
    public Label lowerBound(Label L1, Label L2) {
        return ts.meet(L1, L2);
    }

    public Node labelCheck(Node n) throws SemanticException {
        if (n.ext() instanceof Jif) {
	    n = ((Jif) n.ext()).del().labelCheck(this);
	}
        return n;
    }

    public void constrain(LabelConstraint c) 
	throws SemanticException 
    {        
	this.solver.addConstraint(c);
    }

    /**
     * Called by JifClassDeclExt just before this label checker is used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public void enteringClassBody(ClassType ct) {
        if (solvePerClassBody) {
            // solving by class. Set a new solver for the class body
            this.solver = ts.createSolver(ct.name());
        }
    }

    /**
     * Called by JifMethodDeclExt just before this label checker is used to
     * check a method body. This allows us to use a different solver if
     * required.
     */
    public void enteringMethod(MethodInstance mi) {
        if (solvePerMethod) {
            // solving by method. Set a new solver for the class body
            this.solver = ts.createSolver(mi.container().toString() + "." + mi.name());
        }
    }

    /**
     * Called by JifClassDeclExt just after this label checker has been used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public JifClassDecl leavingClassBody(JifClassDecl n) {
        if (solvePerClassBody) {
            // solving by class. We need to solve the constraints
            return (JifClassDecl)solveConstraints(n);
        }
        return n;
    }

    /**
     * Called by JifClassDeclExt just after this label checker has been used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public JifMethodDecl leavingMethod(JifMethodDecl n) {
        if (solvePerMethod) {
            // solving by class. We need to solve the constraints
            return (JifMethodDecl)solveConstraints(n);
        }
        return n;
    }

    /**
     * This method should be called on the top level label checker once
     * the label checking has finished. This will perform label substitution
     * if required.
     */
    public Node finishedLabelCheckPass(Node n) {
        if (!solvePerClassBody) {
            // solving globally. We need to solve the constraints
            return solveConstraints(n);
        }
        return n;        
    }
    
    private Node solveConstraints(Node n) {
        if (!doLabelSubst) {
            return n;
        }
        
        Node newN = n;
        JifLabelSubst jls = new JifLabelSubst(this.job, this.ts, this.nf, this.solver);
        
        jls = (JifLabelSubst)jls.begin();
        
        if (jls != null) {
            newN = n.visit(jls);
            jls.finish(newN);
        }
        return newN;
    }

    public void reportSemanticException(SemanticException e) {
        Position pos = e.position() != null ? e.position() : job().ast().position();
        errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), pos);
    }
}
