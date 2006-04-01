package jif.extension;

import java.util.*;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.ProcedureDecl;
import polyglot.main.Report;
import polyglot.types.*;
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
    protected Label checkEnforceSignature(JifProcedureInstance mi, LabelChecker lc)
	throws SemanticException
    {
	if (Report.should_report(jif_verbose, 2))
	    Report.report(2, "Adding constraints for header of " + mi);

      	JifContext A = lc.jifContext();
        
        // Set the "auth" variable.
	Set newAuth = constrainAuth(mi, A);

	for (Iterator iter = newAuth.iterator(); iter.hasNext(); ) {
	    Principal p = (Principal) iter.next();
	    // Check that there is a p' in the old "auth" set such that
	    // p' actsFor p.
	    checkActsForAuthority(p, A);
	}

        addCallers(mi, A, newAuth);
        A.setAuthority(newAuth);       

        constrainLabelEnv(mi, A, null);

        // check that any autoendorse constraints are satisfied,
        // and set and constrain the inital PC
        Label Li = checkAutoEndorseConstrainPC(mi, lc);

	return Li;
    }

    protected Label checkAutoEndorseConstrainPC(JifProcedureInstance mi, LabelChecker lc) throws SemanticException {
        final JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();
        JifClassType ct = (JifClassType) A.currentClass();
        Label Li = mi.pcBound();
        Label endorseTo = ts.topLabel();

        for (Iterator iter = mi.constraints().iterator(); iter.hasNext(); ) {
            Assertion c = (Assertion) iter.next();

            if (c instanceof AutoEndorseConstraint) {
                AutoEndorseConstraint ac = (AutoEndorseConstraint) c;
                endorseTo = ts.meet(endorseTo, ac.endorseTo());
            }
        }

        Label callerPcLabel = ts.callSitePCLabel(mi);
        if (!mi.flags().isStatic())  {
            // for non-static methods, we know the this label
            // must be bounded above by the start label
            A.addAssertionLE(ct.thisLabel(), callerPcLabel);  
        }

        A.setPc(callerPcLabel); 
        Label initialPCBound = Li;
        
        if (!endorseTo.isTop()) {
            // check that there is sufficient authority to endorse to 
            // the label endorseTo.
            JifEndorseExprExt.checkOneDimen(lc, A, Li, endorseTo, mi.position(), false, true);
            JifEndorseExprExt.checkAuth(lc, A, Li, endorseTo, mi.position(), false, true);            
            
            // the initial pc bound is the endorseTo label
            initialPCBound = endorseTo;
                        
            // add a restriction on the "callerPC" label. It is less
            // than the endorseTo label
            A.addAssertionLE(callerPcLabel, endorseTo);
        }
        
        A.setCurrentCodePCBound(initialPCBound);        
        return initialPCBound;
        
    }

    /**
     * This method corresponds to the constraint-authority predicate in the
     * thesis (Figure 4.39).  It returns the set of principals for which the
     * method can act.
     */
    protected Set constrainAuth(JifProcedureInstance mi, JifContext A) {
        Set newAuth = new LinkedHashSet();

        for (Iterator iter = mi.constraints().iterator(); iter.hasNext(); ) {
            Assertion c = (Assertion) iter.next();

            if (c instanceof AuthConstraint) {
                AuthConstraint ac = (AuthConstraint) c;

		for (Iterator i = ac.principals().iterator(); i.hasNext(); ) {
		    Principal pi = (Principal) i.next();
		    newAuth.add(pi);
		}
            }
        }

	return newAuth;
    }

    /** Adds the caller's authorities into <code>auth</code> */
    protected static void addCallers(JifProcedureInstance mi, JifContext A, Set auth) {
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
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        Principal authority = ts.conjunctivePrincipal(null, A.authority()); 
        if (A.actsFor(authority, p)) {
            return;
        }

	String codeName = A.currentCode().toString();
        if (A.currentCode() instanceof JifProcedureInstance) {
            codeName = ((JifProcedureInstance)A.currentCode()).debugString();
        }
	throw new SemanticDetailedException(
	    "The authority of the class " + A.currentClass().name() + 
            " is insufficient to act for principal " + p + ".", 
            "The " + codeName + " states that it has the authority of the " +
            "principal " + p + ". However, the conjunction of the authority" +
            " set of the class is insufficient to act for " + p + ".",
            A.currentCode().position());
    }

    /**
     * This method corresponds to the constraint-ph predicate in the thesis
     * (Figure 4.39).  It returns the principal hierarchy used to check the
     * body of the method.
     */
    protected static void constrainLabelEnv(JifProcedureInstance mi, JifContext A, CallHelper ch)
	throws SemanticException
    {
        for (Iterator i = mi.constraints().iterator(); i.hasNext(); ) {
            Assertion c = (Assertion) i.next();

            if (c instanceof ActsForConstraint) {
                ActsForConstraint ac = (ActsForConstraint) c;
		//A.addActsFor(A.instantiate(ac.actor()),
		//	     A.instantiate(ac.granter()));
                Principal actor = ac.actor();
                Principal granter = ac.granter();
                if (ch != null) {
                    actor = ch.instantiate(A, actor);
                    granter = ch.instantiate(A, granter);
                }

                if (ac.isEquiv()) {
                    A.addEquiv(actor, granter);
                }
                else {
                    A.addActsFor(actor, granter);
                }
	    }
            if (c instanceof LabelLeAssertion) {
                LabelLeAssertion lla = (LabelLeAssertion)c;
                Label lhs = lla.lhs();
                Label rhs = lla.rhs();
                if (ch != null) {
                    lhs = ch.instantiate(A, lhs);
                    rhs = ch.instantiate(A, rhs);
                }
                A.addAssertionLE(lhs, rhs); 
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

	// Add the return termination constraints.
	
	Label Lr = mi.returnLabel(); 

        // fold the call site pc into the return label
        Lr = lc.upperBound(Lr, ts.callSitePCLabel(mi));
//	// fold "this" label into the return label because it is protected
//	// at the caller side.
//	if (!mi.flags().isStatic())  {
//	    Lr = lc.upperBound(Lr, ct.thisLabel());
//	}
	        
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
                                                   join(lc,
                                                        "X.r",
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
	    Lrv = lc.upperBound(ts.labelOfType(returnType), Lr);
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
                                                           join(lc, "X(body).nv",
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
		
                // fold the call site pc into the return label
                Lj = lc.upperBound(Lj, ts.callSitePCLabel(mi));
//                // fold "this" label into the label of the declared
//		// throw type, because it is protected
//		// at the caller side.
//		if (!mi.flags().isStatic())  {
//		    Lj = lc.upperBound(Lj, ct.thisLabel());
//		}

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
