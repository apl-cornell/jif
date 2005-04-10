package jif.extension;

import java.util.*;

import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.types.*;
import jif.types.label.*;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;

/** 
 * This is a tool to label check method calls. 
 * This class should be used by creating an instance of it, and then calling
 * the method {@link #checkCall(LabelChecker) checkCall(LabelChecker)}. After
 * the call to that method, the remaining methods (which are getter methods)
 * may be called.
 */
public class CallHelper
{
    private static boolean shouldReport(int obscurity) {
	return Report.should_report(jif.Topics.labels, obscurity);
    }
    
    private static void report(int obscurity, String s) {
	Report.report(obscurity, "labels: " + s);
    }

    /**
     * The class containing the implementation of the procedure being
     * called. 
     */
    private final ReferenceType calleeContainer;
    
    /**
     * Label of the reference to the object on which the procedure is being 
     * called. 
     */
    private final Label targetObjLabel;
    
    /**
     * Copy of the list of the <code>Expr</code>s that are the arguments to 
     * the procedure call. As we label check the argument expressions, we
     * replace the elements of this list with the label checked versions of
     * the argument expressions.  
     */
    private final List actualArgs;
    
    /**
     * The procedure being called.
     */
    private final JifProcedureInstance pi;
    
    /**
     * The position of the procedure call
     */
    private final Position position;

    /**
     * The X pathmap for the each argument.
     */
    private List argPathMaps;
        
    /**
     * Labels of the actual arguments.
     */
    private List actualArgLabels;
        
    /**
     * Exprs of the actual arguments that are final expressions.
     */
    private List actualArgExprs;
        
    /**
     * LabelSusbtitution to replace signature <code>ArgLabel</code>s and
     * <code>DynamicArgLabel</code>s with labels of actual arguments 
     * (<code>argLabels</code>) and the dynamic arguments 
     * (<code>dynArgs</code>).
     */
    //private ArgLabelSubstitution argSubstitution; // LabelSubstitution for argLabels and dynArgs 

    /**
     * The PathMap for the procedure call.
     */        
    private PathMap X;
    
    /**
     * The PathMap for the evaluation of all actual arguments, that is, 
     * Xjoin = join_j xj, for all actual arguments xj.
     */
    private PathMap Xjoin; // join_j xj

    /**
     * The return type of the procedure, if there is one.
     */
    private Type returnType;
    
    /**
     * Flag to ensure that this class is used correctly.
     */
    private boolean callChecked;

    public CallHelper(ReferenceType calleeContainer, 
                      Label targetObjLabel,
                      JifProcedureInstance pi, 
                      List actualArgs, 
	              Position position)
    {
	this.calleeContainer = calleeContainer;
	this.targetObjLabel = targetObjLabel;
	this.actualArgs = new ArrayList(actualArgs);
	this.pi = pi;
	this.position = position;
        this.callChecked = false;

        if (pi.formalTypes().size() != actualArgs.size()) 
            throw new InternalCompilerError("Wrong number of args.");        
    }

    public Type returnType() {
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
	return returnType;
    }

    public List labelCheckedArgs() {
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
	return actualArgs;
    }

    public PathMap X() {
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
	return X;
    }

    /** 
     * Label check the arguments. Also initializes the array 
     * <code>argLabels</code>, that is the labels of the actual arguments.
     */
    private void labelCheckArguments(LabelChecker lc) 
	throws SemanticException
    {
	JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.typeSystem();
	
	// X_0 = X_null[n := A[pc]]
	PathMap Xj = ts.pathMap();
	Xj = Xj.N(A.pc());

        Xjoin = ts.pathMap();
        	
        actualArgLabels = new ArrayList(actualArgs.size());
        argPathMaps = new ArrayList(actualArgs.size());
	
        for (int i = 0; i < actualArgs.size(); i++ ) {
	    Expr Ej = (Expr) actualArgs.get(i);

	    // A[pc := X_{j-1}[N]] |- Ej : Xj
	    A = (JifContext) A.pushBlock();
	    A.setPc(Xj.N());
	    Ej = (Expr) lc.context(A).labelCheck(Ej);
            A = (JifContext) A.pop();
    
            actualArgs.set(i, Ej);

	    Xj = Jif_c.X(Ej);        
            argPathMaps.add(Xj);
            actualArgLabels.add(Xj.NV().copy());
            
	    Xjoin = Xjoin.join(Xj);	    
	}
    }

    /**
     * Return the elements of the List args that are final expressions.
     * The returned list is the same size as args, and any args that are
     * not final expressions have an entry in the returned list of null.
     * 
     * 
     */
    private void buildActualArgExprs(JifTypeSystem ts) throws SemanticException {
        this.actualArgExprs = new ArrayList(actualArgs.size());

        for (Iterator j = actualArgs.iterator(); j.hasNext();) {
            Expr Ej = (Expr)j.next();            
            if (JifUtil.isFinalAccessExprOrConst(ts, Ej)){
                actualArgExprs.add(Ej);
            }
            else {
                actualArgExprs.add(null);
            }
        }
    }
    
    /**
     * ???@@@
     */
    private void constrainArguments(LabelChecker lc) throws SemanticException {
	JifContext A = lc.jifContext();
        JifTypeSystem jts = (JifTypeSystem)A.typeSystem();
        
	// Now constrain the labels of the arguments to be less
	// than the bounds of the formal args, substituting in the
	// fresh labels we just created.

	Iterator formalTypes = pi.formalTypes().iterator();

	for (int j = 0; j < actualArgs.size(); j++) {
            final int count = j + 1;
             
	    final Expr Ej = (Expr) actualArgs.get(j);
        
	    Type tj = (Type)formalTypes.next();
	    ArgLabel aj = (ArgLabel)jts.labelOfType(tj);
        
            // the upper bound label of the formal argument         
            Label argBoundj = instantiate(A, aj.upperBound());

	    // A |- Xj[nv] <= argLj
	    PathMap Xj = (PathMap)argPathMaps.get(j);
            lc.constrain(new LabelConstraint(new NamedLabel("actual_arg_"+count, 
                                                            "the label of the " + StringUtil.nth(count) + " actual argument",
                                                            Xj.NV()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("formal_arg_"+count, 
                                                        "the upper bound of the formal argument " + aj.formalInstance().name(),
                                                        argBoundj),
                                         A.labelEnv(),
                                         Ej.position()) {
                 public String msg() {
                     return "The actual argument is more restrictive than " +
                            "the formal argument.";
                 }
                 public String detailMsg() { 
                     return "The label of the actual argument, " + namedLhs() + 
                            ", is more restrictive than the label of the " +
                            "formal argument, " + namedRhs() + ".";
                 }
                 public String technicalMsg() {
                     return "Invalid argument: the actual argument <" + Ej + 
                            "> has a more restrictive label than the " +
                            "formal argument.";
                 }                     
            }
            );

	    // Must check that the actual is a subtype of the formal.
	    // Most of this is done in typeCheck, but if actual and formal
	    // are instantitation types, we must add constraints for the
	    // labels.
            SubtypeChecker sc = new SubtypeChecker();
	    sc.addSubtypeConstraints(lc, Ej.position(),
	                             instantiate(A, aj.formalInstance().type()), Ej.type());
	}
    }

    private Label resolveStartLabel(JifContext A) {
        return instantiate(A, pi.startLabel());
    }

    /** 
     * Returns the instantiated return label or the bottom label if the return
     * label is not defined.
     */
    private Label resolveReturnLabel(JifContext A) {
        return instantiate(A, pi.returnLabel());
    }

    private Label resolveReturnValueLabel(JifContext A, Label returnLabel) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
	Label L = null;

	if (pi instanceof MethodInstance) {
	    MethodInstance mi = (MethodInstance) pi;
	    L = instantiate(A, ts.labelOfType(mi.returnType()));
	}
	else {
	    L = ts.bottomLabel(pi.position());
	}

	L = L.join(returnLabel);
    
        return L;	
    }
    
    private PathMap excPathMap(JifContext A, Label returnLabel) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
	PathMap Xexn = ts.pathMap();

	for (Iterator e = pi.throwTypes().iterator(); e.hasNext(); ) {
	    Type te = (Type) e.next();

	    Label Le = ts.labelOfType(te, returnLabel);
	    Le = instantiate(A, Le);

	    Xexn = Xexn.exception(te, Le.join(A.pc()));
	}    
	
	return Xexn;
    }

    /** 
     * Check method calls. (Thesis, Figure 4.29)
     * 
     *  
     */
    public void checkCall(LabelChecker lc) 
	throws SemanticException
    {        
        if (callChecked) {
            throw new InternalCompilerError("checkCall already called!");
        }
        
	JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.typeSystem();
	
        buildActualArgExprs(ts);

        // A |- call-begin(ct, args, mti)
	if (shouldReport(4)) report(4, ">>>>> call-begin");

	// check arguments
	labelCheckArguments(lc);
	
        A = A.objectTypeAndLabel(calleeContainer, targetObjLabel);
        lc = lc.context(A);

	constrainArguments(lc);
	
        // A |- X_{maxj}[N] + entry_pc <= Li
        Label Li = resolveStartLabel(A);    
	if (Li != null) {
            final ProcedureInstance callee = pi;

            // the path map for the last argument. 
            PathMap Xlast = ts.pathMap().N(A.pc()); 
            if (!argPathMaps.isEmpty()) {
                Xlast = (PathMap)argPathMaps.get(argPathMaps.size()-1);
            }
            NamedLabel namedLi = new NamedLabel("callee_PC_bound",
                                                "lower bound on the side effects of the method " + callee.signature(),
                                                Li);
                                                
            lc.constrain(new LabelConstraint(new NamedLabel("pc_call", 
                                                            "label of the program counter at this call site",
                                                            Xlast.N()), 
                                         LabelConstraint.LEQ, 
                                         namedLi,
                                         A.labelEnv(),
                                         position) {
                 public String msg() {
                     return "PC at call site more restrictive than " +
                            "begin label of " + callee.signature() + ".";
                 }
                 public String detailMsg() { 
                     return "Calling the method at this program point may " +
                            "reveal too much information to the receiver of " +
                            "the method call. " + callee.signature() + " can only be invoked " +
                            "if the invocation will reveal no more " +
                            "information than the callee's begin label, " + 
                            namedRhs() + ". However, execution reaching " +
                            "this program point may depend on information " +
                            "up to the PC at this program point: " + 
                            namedLhs() + ".";
                 }
                 public String technicalMsg() {
                     return "Invalid method call: " + namedLhs() + 
                            " is more restrictive than " +
                            "the callee's begin label.";
                 }                     
            }
            );
            lc.constrain(new LabelConstraint(new NamedLabel("caller_PC_bound", 
                                                            "lower bound on the side effects of caller",
                                                            A.entryPC()), 
                                         LabelConstraint.LEQ, 
                                         namedLi,
                                         A.labelEnv(),
                                         position) {
                 public String msg() {
                     return "The side effects of " + callee.signature() + 
                            " are not bounded by the PC bound.";
                 }
                 public String detailMsg() { 
                     return "Calling the method here may have side effects " +
                            "that are not bounded below by the PC bound of the " +
                            "caller. The side effects of the method to be " +
                            "invoked are bounded below by the callee's " +
                            "begin label, " + namedRhs() + 
                            ". However, the side effects of the calling context must " +
                            "be bounded below by the caller's begin label " + 
                            namedLhs() + ".";
                 }
                 public String technicalMsg() {
                     return "Invalid method call: " + namedLhs() + 
                            " is more restrictive than " +
                            "the callee's begin label.";
                 }                     
            }
            );
	}

	satisfiesConstraints(lc.context());

	// A |- call-end(ct, pi, Aa, Li, LrvDef)
	if (shouldReport( 4))
	    report( 4, ">>>>> call-end");

	Label Lr = resolveReturnLabel(A);
	Label Lrv = resolveReturnValueLabel(A, Lr);
	        
	X = Xjoin.N(Lr.join(A.pc()));
	X = X.NV(Lrv.join(A.pc()));
	X = X.join(excPathMap(A, Lr));
	
	if (pi instanceof MethodInstance) {
	    returnType = instantiate(A, ((MethodInstance)pi).returnType());
	}
        else {
            returnType = null;
        }
        
        // record that this method has now been called.
        callChecked = true;
    }

    /** Check if the caller has sufficient authority.
     *  Thesis, Figure 4.29
     */
    private void satisfiesConstraints(JifContext A) 
	throws SemanticException
    {
	JifProcedureInstance jpi = pi;
	
	for (Iterator i = jpi.constraints().iterator(); i.hasNext() ; ) {
	    Assertion jc = (Assertion) i.next();

	    if (jc instanceof AuthConstraint) {
		continue;
	    }
	    else if (jc instanceof CallerConstraint) {
		CallerConstraint jcc = (CallerConstraint) jc;

		// Check the authority 
		for (Iterator i2 = jcc.principals().iterator(); i2.hasNext(); ) {
		    Principal pi = (Principal) i2.next();
		    pi = instantiate(A, pi);

		    boolean sat = false;

		    for (Iterator j = A.authority().iterator(); j.hasNext(); ) {
			Principal pj = (Principal) j.next();

			if (A.actsFor(pj, pi)) {
			    sat = true;
			    break;
			}
		    }

		    if (! sat) {
			throw new SemanticException("Caller does not have " +
			    "sufficent authority to call " + jpi.debugString() + ".");
		    }
		}
	    }
	    else if (jc instanceof ActsForConstraint) {
		ActsForConstraint jac = (ActsForConstraint) jc;

		Principal actor = jac.actor();
		actor = instantiate(A, actor);

		Principal granter = jac.granter();
		granter = instantiate(A, granter);

		if (! A.actsFor(actor, granter)) {
		    throw new SemanticException(jpi.debugString() +
			" requires invalid actsFor relation.");
		}
	    }
	}
    }
    private static List getArgLabelsFromFormalTypes(List formalTypes, JifTypeSystem jts) {
        List formalArgLabels = new ArrayList(formalTypes.size());
        for (Iterator iter = formalTypes.iterator(); iter.hasNext(); ) {
            Type t = (Type)iter.next();
            ArgLabel al = (ArgLabel)jts.labelOfType(t);
            formalArgLabels.add(al);            
        }
        return formalArgLabels;        
    }
    private Label instantiate(JifContext A, Label L) {        
        return instantiateArgLabels(A.instantiate(L, true), 
                                    getArgLabelsFromFormalTypes(pi.formalTypes(), (JifTypeSystem)pi.typeSystem()), 
                                    this.actualArgLabels, 
                                    this.actualArgExprs, 
                                    A.currentClass());
    }
    
    private static Label instantiateArgLabels(Label L, List formalArgLabels, List actualArgLabels, List actualArgExprs, ClassType callerClass) {
        if (formalArgLabels.size() != actualArgLabels.size() || 
                (actualArgExprs != null && formalArgLabels.size() != actualArgExprs.size())) {
            throw new InternalCompilerError("Inconsistent sized lists of args");
        }
        if (L != null) {
            // go through each formal arglabel, and replace it appropriately...
            Iterator iArgLabels = formalArgLabels.iterator();
            Iterator iActualArgLabels = actualArgLabels.iterator();
            Iterator iActualArgExprs = null;
            if (actualArgExprs != null) {
                iActualArgExprs = actualArgExprs.iterator();
            }
            while (iArgLabels.hasNext()) {
                ArgLabel formalArgLbl = (ArgLabel)iArgLabels.next();
                Label actualArgLbl = (Label)iActualArgLabels.next();
                L = L.subst(formalArgLbl.formalInstance(), actualArgLbl);

                if (iActualArgExprs != null) {
                    Expr actualExpr = (Expr)iActualArgExprs.next();
                
                    if (actualExpr != null) {
                        L = L.subst((AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance()), 
                                    JifUtil.exprToAccessPath(actualExpr, callerClass));
                    }
                }
            }
            if (iArgLabels.hasNext() || 
                    iActualArgLabels.hasNext() || 
                    (iActualArgExprs != null && iActualArgExprs.hasNext())) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
        }
        return L;
    }
    
    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal. 
     */        
    private Principal instantiate(JifContext A, Principal p) {
        return instantiateArgLabels(A.instantiate(p, true),
                                    getArgLabelsFromFormalTypes(this.pi.formalTypes(), (JifTypeSystem)this.pi.typeSystem()), 
                                    this.actualArgExprs,
                                    A.currentClass());
    }
    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal. 
     */        
    private static Principal instantiateArgLabels(Principal p, List formalArgLabels, List actualArgExprs, ClassType callerClass) {
        if (formalArgLabels.size() != actualArgExprs.size()) {
            throw new InternalCompilerError("Inconsistent sized lists of args");
        }
        if (p != null) {
            // go through each formal arglabel, and replace it appropriately...
            Iterator iArgLabels = formalArgLabels.iterator();
            Iterator iActualArgExprs = actualArgExprs.iterator();
            while (iArgLabels.hasNext()) {
                ArgLabel formalArgLbl = (ArgLabel)iArgLabels.next();
                Expr actualExpr = (Expr)iActualArgExprs.next();
                
                if (actualExpr != null) {
                    p = p.subst((AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance()), 
                            JifUtil.exprToAccessPath(actualExpr, callerClass));
                }
            }
            if (iArgLabels.hasNext() || iActualArgExprs.hasNext()) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
        }
        
        return p;   
    }

    private Type instantiate(JifContext A, Type t) throws SemanticException {
        t = A.instantiate(t, true);
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType)t;
            Type baseType = at.base();
            t = at.base(instantiate(A, baseType));
        }
        
        if (ts.isLabeled(t)) {
            Label newL = instantiate(A, ts.labelOfType(t));
            Type newT = instantiate(A, ts.unlabel(t));
            t = ts.labeledType(t.position(), newT, newL);
        }

        if (t instanceof JifSubstType) {
            JifSubstType jit = (JifSubstType)t;
            Map newMap = new HashMap();
            boolean diff = false;
            for (Iterator i = jit.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = instantiate(A, (Label)arg);
                }
                else if (arg instanceof Principal) {
                    p = instantiate(A, (Principal)arg);
                }
                else {
                    throw new InternalCompilerError(
                        "Unexpected type for entry: "
                            + arg.getClass().getName());
                }
                newMap.put(e.getKey(), p);

                if (p != arg)
                    diff = true;
            }
            if (diff) {
                t = ts.subst(jit.base(), newMap);
            }
        }

        return t;
    }    
}