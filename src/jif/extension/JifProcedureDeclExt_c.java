package jif.extension;

import java.util.*;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Formal;
import polyglot.ast.ProcedureDecl;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>ProcedureDecl</code> node. 
 * 
 *  @see polyglot.ast.ProcedureDecl
 *  @see jif.types.JifProcedureInstance
 */
public class JifProcedureDeclExt_c extends Jif_c implements JifProcedureDeclExt
{
    public JifProcedureDeclExt_c(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    static String jif_verbose = "jif";

    /**
     * This methods corresponds to the check-arguments predicate in the
     * thesis (Figure 4.37).  It returns the start label of the method.
     * It mutates the local context (to the A'' in the rule).
     */
    protected Label checkArguments(JifProcedureInstance mi, LabelChecker lc)
	throws SemanticException
    {
	ProcedureDecl mn = (ProcedureDecl) node();
	
	if (Report.should_report(jif_verbose, 2))
	    Report.report(2, "Adding constraints for header of " + mi);

	JifTypeSystem ts = lc.jifTypeSystem();
      	JifContext A = lc.jifContext();
        JifClassType ct = (JifClassType) A.currentClass();
	
	// We begin by getting the start label.
        // Label Li = A.instantiate(mi.externalPC());
        Label Li = mi.externalPC();
	A.addAssertionLE(ct.thisLabel(), Li);	

        // let internal_pc = bottom, external_pc = Li
	A.setPc(ts.bottomLabel(mi.position()));
	A.setEntryPC(Li);

	// Add constraints for the formal parameters.  Equate the declared
	// label of each formal with the corresponding label variable in
	// the method instance.
	// @@@@@Check that this is correct!!! I think it is not needed, it is just insisting that <arg _ ub> <= ub
	List formalNames = new LinkedList();
	for (Iterator iter = mn.formals().iterator(); iter.hasNext(); ) 
	    formalNames.add(((Formal)iter.next()).name());
	
        Iterator types = mi.formalTypes().iterator();
	Iterator formals = mn.formals().iterator();

	int index = 0;
        while (types.hasNext() && formals.hasNext()) {
            Type tj = (Type) types.next();
	    Formal fj = (Formal) formals.next();

	    JifLocalInstance lij = (JifLocalInstance) fj.localInstance();
            Label Lj = lij.label();        
        
	    // instantiate argument types
	    // lij.setType(A.instantiate(lij.type()));
	    
	    // This is the declared label of the parameter.
	    Label argBj = ts.labelOfType(tj);
	    // argBj = A.instantiate(argBj);

	    A.addAssertionLE(Lj, argBj);

	    /*if (ts.isLabeled(tj)) {
		depGraph.addNode((VarLabel) Lj);

		for (Iterator iter = argLj.components().iterator(); iter.hasNext(); ) {
		    Label comp = (Label) iter.next();
		    if (comp instanceof VarLabel) {
			VarLabel vl = (VarLabel) comp;
			if (formalNames.contains(vl.uid().name()))
			    depGraph.addEdge((VarLabel) Lj, vl);
		    }
		}
	    }*/
	    
            //lc.constrainEqual(Lj, argLj, fj.position(), 
	    //	    "Lj == argLj (argument check)");

	    index++;
        }

        if (types.hasNext() || formals.hasNext()) {
	    throw new InternalCompilerError("Argument list mismatch.");
	}
	
	// commented out by zlt: there is no need to check for circularity. 
	/*if (depGraph.hasCycle())
	    throw new InternalCompilerError(
		    "Declared labels of formals contains circular
		    references.");*/

        // Set the "auth" variable.
	Set newAuth = constraintAuth(mi, A);

	for (Iterator iter = newAuth.iterator(); iter.hasNext(); ) {
	    Principal p = (Principal) iter.next();
	    // Check that there is a p' in the old "auth" set such that
	    // p' actsFor p.
	    checkActsForAuthority(p, A);
	}

	addCallers(mi, A, newAuth);

	A.setAuthority(newAuth);
       
	constrainPH(mi, A);

	return Li;
    }

    /**
     * This method corresponds to the constraint-authority predicate in the
     * thesis (Figure 4.39).  It returns the set of principals for which the
     * method can act.
     */
    protected Set constraintAuth(JifProcedureInstance mi, JifContext A) {
        Set newAuth = new HashSet();

        for (Iterator iter = mi.constraints().iterator(); iter.hasNext(); ) {
            Assertion c = (Assertion) iter.next();

            if (c instanceof AuthConstraint) {
                AuthConstraint ac = (AuthConstraint) c;

		for (Iterator i = ac.principals().iterator(); i.hasNext(); ) {
		    Principal pi = (Principal) i.next();
		    //newAuth.add(A.instantiate(pi));
		    newAuth.add(pi);
		}
            }
        }

	return newAuth;
    }

    /** Adds the caller's authorities into <code>auth</code> */
    protected void addCallers(JifProcedureInstance mi, JifContext A, Set auth) {
        for (Iterator iter = mi.constraints().iterator(); iter.hasNext(); ) {
            Assertion c = (Assertion) iter.next();

            if (c instanceof CallerConstraint) {
                CallerConstraint cc = (CallerConstraint) c;

		for (Iterator i = cc.principals().iterator(); i.hasNext(); ) {
		    Principal pi = (Principal) i.next();
		    // auth.add(A.instantiate(pi));
		    auth.add(pi);
		}
            }
        }
    }

    /**
     * Check that there is a p' in the old "auth" set such that p' actsFor p.
     */
    protected void checkActsForAuthority(Principal p, JifContext A)
	throws SemanticException
    {
	for (Iterator iter = A.authority().iterator(); iter.hasNext(); ) {
	    Principal pp = (Principal) iter.next();

	    if (A.actsFor(pp, p)) {
		return;
	    }
	}
//    System.out.println("### auth: " + A.authority());
//    System.out.println("### auth: " + A.currentCode().position());
//    System.out.println("### ph: " + ph);

	throw new SemanticException(
	    "No principal found in authority set that acts for principal \"" +
	    p + "\".", A.currentCode().position());
    }

    /**
     * This method corresponds to the constraint-ph predicate in the thesis
     * (Figure 4.39).  It returns the principal hierarchy used to check the
     * body of the method.
     */
    protected void constrainPH(JifProcedureInstance mi, JifContext A)
	throws SemanticException
    {
        for (Iterator i = mi.constraints().iterator(); i.hasNext(); ) {
            Assertion c = (Assertion) i.next();

            if (c instanceof ActsForConstraint) {
                ActsForConstraint ac = (ActsForConstraint) c;
		//A.addActsFor(A.instantiate(ac.actor()),
		//	     A.instantiate(ac.granter()));
		A.addActsFor(ac.actor(), ac.granter());
	    }
	}
    }

    /**
     * This method corresponds to most of the check-body predicate in the
     * thesis (Figure 4.40).  It assumes the body has already been checked
     * and that the path map X is the join of the body's path map and the
     * initial path map of the method.
     *
     * It adds the constraints that associate return termination and
     * return value labels in the path map X with the declared return
     * label and associates the exception labels in the path map X with
     * the declared labels in the methods "throws" clause.
     */
    protected void addReturnConstraints(Label Li, PathMap X,
	JifProcedureInstance mi, LabelChecker lc, final Type returnType) throws SemanticException
    {
	if (Report.should_report(jif_verbose, 2))
	    Report.report(2, "Adding constraints for result of " + mi);

	ProcedureDecl mn = (ProcedureDecl) node();
	JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
        JifClassType ct = (JifClassType) A.currentClass();

	// Add the return termination constraints.
	
	Label Lr = mi.returnLabel(); 

	// fold "this" label into the return label because it is protected
	// at the caller side.
	if (!mi.flags().isStatic()) 
	    Lr = Lr.join(ct.thisLabel());
	
	//Lr = A.instantiate(Lr);
        
        //Hack: If no other paths, the procedure must return. Therefore,
        //X.n is not taken, and X.r doesn't contain any information. 
	//TODO: implement a more precise single-path rule.
        if (! (X.N() instanceof NotTaken)) {
            boolean singlePath = true;
            for (Iterator iter = X.paths().iterator(); iter.hasNext(); ) {
                Path p = (Path) iter.next();
                if (p.equals(Path.N) || p.equals(Path.R)) continue;
                singlePath = false;
                break;
            }
            if (singlePath) {
                X = X.N(ts.notTaken());
                X = X.R(ts.bottomLabel());
            }
        }            

	lc.constrain(new LabelConstraint(new NamedLabel("X.n",
                                                        "information that may be gained by the body terminating normally",
                                                        X.N()).
                                                   join("X.r",
                                                        "information that may be gained by exiting the body with a return statement",
                                                        X.R()),
                                         LabelConstraint.LEQ,
                                         new NamedLabel("Lr", 
                                                        "return label of the method",
                                                        Lr),
                                         A.labelEnv(),
                                         mn.position())
                     {
                         public String msg() { 
                             return "The non-exception termination of the " +
                                    "method body may reveal more information " +
                                    "than is declared by the method return label.";
                         }
                         public String detailMsg() { 
                             return "The method return label, " + namedRhs() + 
                                    ", is an upper bound on how much " +
                                    "information can be gained by observing " +
                                    "that this method terminates normally " +
                                    "(i.e., terminates without throwing " +
                                    "an exception). The method body may " +
                                    "reveal more information than this. The " +
                                    "return label of a method is declared " +
                                    "after the variables, e.g. " +
                                    "\"void m(int i):{" + namedRhs() + "}\".";
                         }
                         public String technicalMsg() {
                             return "the return(end) label is less restrict than " +
                                    namedLhs() + " of the body.";
                         }
                     }
        );
	// Add the return value constraints.
	Label Lrv = null;

	if (ts.isLabeled(returnType)) {
	    Lrv = ts.labelOfType(returnType).join(Lr);
	}
	else if (returnType.isVoid()) {
	    Lrv = ts.notTaken();
	}
	else {
	    throw new InternalCompilerError("Unexpected return type: " + returnType);
	}
	// Lrv = A.instantiate(Lrv);

	if (! (Lrv instanceof NotTaken)) {
            lc.constrain(new LabelConstraint(new NamedLabel("X(body).rv",
                                                                "the label of values returned by the body of the method via a return statement",
                                                                X.RV()).
                                                           join("X(body).nv",
                                                                "the label of values returned by the body of the method",
                                                                X.NV()),
                                                 LabelConstraint.LEQ,
                                                 new NamedLabel("Lrv", 
                                                                "return value label of the method",
                                                                Lrv),
                                                 A.labelEnv(),
                                                 mn.position())
                             {
                             public String msg() { 
                                 return "This method may return a value with " +
                                        "a more restrictive label than the " +
                                        "declared return value label.";
                             }
                             public String detailMsg() { 
                                 return msg() + " The declared return type " +
                                        "of this method is " + returnType + 
                                        ". As such, values returned by this " +  
                                        "method can have a label of at most " +
                                        namedRhs() +".";
                             }
                             public String technicalMsg() {
                                 return "this method may return a value " +
                                        "with a more restrictive label " + 
                                        "than the declared return value label.";
                             }
                         }
                );
	}
	
	// Add the exception path constraints.
	for (Iterator iter = X.paths().iterator(); iter.hasNext(); ) {
	    Path path = (Path) iter.next();

	    if (! (path instanceof ExceptionPath)) {
		continue;
	    }

	    ExceptionPath ep = (ExceptionPath) path;

	    Label pathLabel = X.get(ep);

	    if (pathLabel instanceof NotTaken)
	        throw new InternalCompilerError(
		    "An exception path cannot be not taken");

	    Type pathType = ep.exception();
            NamedLabel pathNamedLabel = new NamedLabel("exc_"+pathType.toClass().name(), 
                         "upper bound on information that may be gained " +
                         "by observing the method throwing the exception " + pathType.toClass().name(),
                         pathLabel);
    
            for (Iterator j = mi.throwTypes().iterator(); j.hasNext(); ) {
		final Type tj = (Type) j.next();
		Label Lj = ts.labelOfType(tj, Lr);
		// Lj = A.instantiate(Lj);
		Lj = Lj.join(ct.thisLabel()); // FIXME: is this safe?

		if (ts.isImplicitCastValid(pathType, tj)) {
		    subtypeChecker.addSubtypeConstraints(lc, mn.position(),
			                                 tj, pathType);
		    if (Report.should_report(jif_verbose, 4))
			Report.report(4,
			">>> X[C'] <= Lj (for exception " + tj + ")");

                    lc.constrain(new LabelConstraint(pathNamedLabel,
                                                     LabelConstraint.LEQ,
                                                      new NamedLabel("decl_exc_"+tj.toClass().name(),
                       "declared upper bound on information that may be " +
                       "gained by observing the method throwing the exception " + tj.toClass().name(),
                                                                     Lj),
                                                      A.labelEnv(),
                                                      mi.position())
                                     {
                                     public String msg() { 
                                         return "More information may be gained " + 
                                           "by observing a " + tj.toClass().fullName() +
                                           " exception than is permitted by the " +
                                           "method/coonstructor signature";
                                     }
                                     public String technicalMsg() {
                                         return "the path of <" + tj + "> may leak information " +
                                                "more restrictive than the join of the declared " +
                                                "exception label and the return(end) label";
                                     }
                                 }
                        );
		}
	    }
	}
    }
    
    static class Graph {
	Hashtable nodes;
	List freeList;
	
	public Graph() {
	    nodes = new Hashtable();
	    freeList = new LinkedList();
	}
	
	public void addNode(VarLabel vl) {
	    nodes.put(vl, new LinkedList());
	}
	
	public void addEdge(VarLabel v1, VarLabel v2) {
	    List outs = (List) nodes.get(v1);
	    if (outs!=null) {
		if (!outs.contains(v2))
		    outs.add(v2);
	    }
	}
	
	public boolean hasCycle() {
	    while (true) {
		int old = nodes.size();
		for (Iterator iter=nodes.entrySet().iterator(); iter.hasNext(); ) {
		    Map.Entry entry = (Map.Entry) iter.next();
		    VarLabel v = (VarLabel) entry.getKey();
		    List deps = (List) entry.getValue();
		    boolean free = true;
		    for (Iterator i2 = deps.iterator(); i2.hasNext(); ) {
			Object n = i2.next();
			if (nodes.contains(n) && !freeList.contains(n)) {
			    free = false;
			    break;
			}
		    }
		    if (free) {
			freeList.add(v);
			iter.remove();
		    }
		}
		if (old == nodes.size()) break;
	    }
	    
	    return !nodes.isEmpty();
	}
    }
}
