package jif.extension;

import java.util.*;

import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.ast.PrincipalNode;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.StringUtil;

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
    private final List args;
    
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
    private List argLabels;
    
    /**
     * LabelSusbtitution to replace signature <code>ArgLabel</code>s and
     * <code>DynamicArgLabel</code>s with labels of actual arguments 
     * (<code>argLabels</code>) and the dynamic arguments 
     * (<code>dynArgs</code>).
     */
    private ArgLabelSubstitution argSubstitution; // LabelSubstitution for argLabels and dynArgs 

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
                      List args, 
	              Position position)
    {
	this.calleeContainer = calleeContainer;
	this.targetObjLabel = targetObjLabel;
	this.args = new ArrayList(args);
	this.pi = pi;
	this.position = position;
        this.callChecked = false;
	
        if (pi.formalTypes().size() != args.size()) 
            throw new InternalCompilerError("Wrong number of args.");   

        this.argSubstitution = null;
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
	return args;
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
        	
        argLabels = new ArrayList(args.size());
        argPathMaps = new ArrayList(args.size());
	
        for (int i = 0; i < args.size(); i++ ) {
	    Expr Ej = (Expr) args.get(i);

	    // A[pc := X_{j-1}[N]] |- Ej : Xj
	    A = (JifContext) A.pushBlock();
	    A.setPc(Xj.N());
	    Ej = (Expr) lc.context(A).labelCheck(Ej);
            A = (JifContext) A.pop();
    
            args.set(i, Ej);

	    Xj = Jif_c.X(Ej);        
            argPathMaps.add(Xj);
            argLabels.add(Xj.NV().copy());
            
	    Xjoin = Xjoin.join(Xj);	    
	}
    }

    /**
     * Return the elements of the List args that are dynamic labels or 
     * principals. The reutnred list is the same size as args, and
     * any dynamic arguments in the returned list are at the same index
     * as they were in <code>args</code>.
     * 
     * @param args a list of <code>Expr</code>, being the actual arguments to the procedure
     * 
     */
    private List dynArgs(List args, JifTypeSystem ts) throws SemanticException {
        List dynArgs = new ArrayList(args.size());

        for (Iterator j = args.iterator(); j.hasNext();) {
            Expr Ej = (Expr)j.next();            
            if (ts.isLabel(Ej.type())) {
                Label l = JifUtil.exprToLabel(ts, Ej);
                if (l == null) {                
                    throw new InternalCompilerError("Unexpected label " + 
                            Ej + " (" + Ej.getClass().getName() + ")");
                }
                dynArgs.add(l);
            }
            else if (ts.isPrincipal(Ej.type())) {
                Principal p = JifUtil.exprToPrincipal(ts, Ej);
                if (p == null) {                
                    throw new InternalCompilerError("Unexpected principal " + 
                            Ej + " (" + Ej.getClass().getName() + ")");
                }
                dynArgs.add(p);
            }
            else {
                dynArgs.add(null);
            }
        }
        
        return dynArgs;
    }
    
    private VarLabel createVarLabelForArg(JifTypeSystem ts, int argIndex, Position argPos) {
        String name = "call";
        // For easier debugging.
        if (pi instanceof MethodInstance)
            name = ((MethodInstance)pi).name();
        name += "-"+(argIndex+1);
        return ts.freshLabelVariable(argPos, name,
                "label of the PC after evaluating the " + StringUtil.nth(argIndex+1) + 
                " arg of method call at " + 
                argPos.toString());
    }
    
    /**
     * ???@@@
     */
    private void constrainArguments(LabelChecker lc) throws SemanticException {
	JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.typeSystem();
        
	// Now constrain the labels of the arguments to be less
	// than the labels of the formals, substituting in the
	// fresh labels we just created.

	Iterator formalArgTypes = pi.formalTypes().iterator();

	for (int j = 0; j < args.size(); j++) {
            final int count = j + 1;
             
	    final Expr Ej = (Expr) args.get(j);

            // the var label for the jth argument
            Label Lj = createVarLabelForArg(ts, j, Ej.position());

	    Type tj = (Type) formalArgTypes.next();
            
            // the label of the formal argument type         
            Label argLj = instantiate(A, ts.labelOfType(tj, Lj));

	    // To get more precise results, we use the constraint
	    // "Lj <= argLj" instead of "Lj == argLj"
            lc.constrain(new LabelConstraint(new NamedLabel("poly_formal_arg_"+count, 
                                                            "the label of the " + StringUtil.nth(count) + " formal argument",
                                                            Lj), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("formal_arg_"+count+"_bound", 
                                                        "upper bound on label of the " + StringUtil.nth(count) + " formal argument",
                                                        argLj),
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
                     return "[Lj <= argLj] is not satisfied for argument <" + 
                            Ej + ">.";
                 }                     
            }
            );

	    // A |- Xj[nv] <= Lj

	    PathMap Xj = (PathMap)argPathMaps.get(j);
            lc.constrain(new LabelConstraint(new NamedLabel("actual_arg_"+count, 
                                                            "the label of the " + StringUtil.nth(count) + " actual argument",
                                                            Xj.NV()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("poly_formal_arg_"+count, 
                                                        "the label of the " + StringUtil.nth(count) + " formal argument",
                                                        Lj),
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
	                             instantiate(A, tj), Ej.type());
	}
    }

    private Label resolveStartLabel(JifContext A) {
	if (pi.startLabel()!= null) {
	    return instantiate(A, pi.startLabel());
	}
        return null;
    }

    /** 
     * Returns the instantiated return label or the bottom label if the return
     * label is not defined.
     */
    private Label resolveReturnLabel(JifContext A) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();

	Label Lr = ts.bottomLabel();
        if (pi.returnLabel() != null) {
            Lr = instantiate(A, pi.returnLabel());
        }

        return Lr;
    }

    private Label resolveReturnValueLabel(JifContext A, Label returnLabel) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
	Label L = null;

	if (pi instanceof MethodInstance) {
	    MethodInstance mi = (MethodInstance) pi;
	    if (ts.isLabeled(mi.returnType())) {
		L = instantiate(A, ts.labelOfType(mi.returnType()));
            }
	    else 
		L = defaultReturnValueLabel(ts);
	}
	else {
	    L = defaultReturnValueLabel(ts);
	}

	L = L.join(returnLabel);
    
        return L;	
    }
    
    private Label defaultReturnValueLabel(JifTypeSystem ts) {
        if (pi instanceof MethodInstance
            && !((MethodInstance)pi).returnType().isVoid()) {
            return Xjoin.NV();
        } else {
            return ts.bottomLabel(pi.position());
        }
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
	
	// A |- call-begin(ct, args, mti)
	if (shouldReport(4)) report(4, ">>>>> call-begin");

	// check arguments
	labelCheckArguments(lc);
	
        this.argSubstitution = new ArgLabelSubstitution(argLabels, dynArgs(args, ts), true);
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
	JifProcedureInstance jpi = (JifProcedureInstance) pi;
	
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
    
    private Label instantiate(JifContext A, Label L) {
        L = A.instantiate(L, true);
        if (L != null) {
            try {
                L = L.subst(argSubstitution);
            }
            catch (SemanticException e) {
                throw new InternalCompilerError("Unexpected SemanticException " +
                "during label substitution: " + e.getMessage(), L.position());
            }
        }
        return L;
    }
    

    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal. 
     */        
    private Principal instantiate(JifContext A, Principal p) {
        p = A.instantiate(p, true);
        if (p != null) {
            try {
                p = p.subst(argSubstitution);
            }
            catch (SemanticException e) {
                throw new InternalCompilerError("Unexpected SemanticException " +
                "during label substitution: " + e.getMessage(), p.position());
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