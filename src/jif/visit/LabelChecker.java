package jif.visit;

import jif.ast.Jif;
import jif.ast.JifClassDecl;
import jif.types.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.Copy;
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
    final private boolean solveClassBodies;
    
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

    public LabelChecker(Job job, TypeSystem ts, NodeFactory nf, boolean solveClassBodies) {
        this.job = job;
        this.ts = (JifTypeSystem) ts;
        this.context = (JifContext) ts.createContext();
        this.nf = nf;

        this.solveClassBodies = solveClassBodies;
        this.doLabelSubst = true;
        if (!solveClassBodies) {
            this.solver = this.ts.solver();
        }
    }

    public LabelChecker(Job job, TypeSystem ts, NodeFactory nf) {
        this.job = job;
        this.ts = (JifTypeSystem) ts;
        this.context = (JifContext) ts.createContext();
        this.nf = nf;

        this.doLabelSubst = false;
        this.solveClassBodies = false;
        this.solver = this.ts.solver();
    }

    public Object copy() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalCompilerError("Java clone() weirdness.");
        }
    }

    public JifContext context() {
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

    public JifContext jifContext() {
	return context;
    }

    public Solver solver() {
        return this.solver;
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

//    public void bind(DynamicLabel dl, Label l) {
//        this.solver.bind(dl, l);
//    }

    /**
     * Called by JifClassDeclExt just before this label checker is used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public void enteringClassBody() {
        if (solveClassBodies) {
            // solving by class. Set a new solver for the class body
            this.solver = ts.solver();
        }
    }

    /**
     * Called by JifClassDeclExt just after this label checker has been used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public JifClassDecl leavingClassBody(JifClassDecl n) {
        if (solveClassBodies) {
            // solving by class. We need to solve the constraints
            return (JifClassDecl)solveConstraints(n);
        }
        return n;
    }

    /**
     * This method should be called on the top level label checker once
     * the label checking has finished. This will perform label substitution
     * if required.
     */
    public Node finishedLabelCheckPass(Node n) {
        if (!solveClassBodies) {
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
	jif.ExtensionInfo extInfo = (jif.ExtensionInfo) this.job.extensionInfo();
        JifLabelSubst jls = extInfo.labelSubst(this.job, this.solver);
        
        jls = (JifLabelSubst)jls.begin();
        
        if (jls != null) {
            newN = n.visit(jls);
            jls.finish(newN);
        }
        return newN;
    }
}
