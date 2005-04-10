package jif.extension;

import java.util.*;

import jif.ast.*;
import jif.ast.JifConstructorDecl;
import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.main.Report;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>JifConstructorDecl</code> node. 
 * 
 *  @see polyglot.ast.ConstructorDecl
 *  @see jif.ast.JifConstructorDecl
 */
public class JifConstructorDeclExt extends JifProcedureDeclExt_c
{
    public JifConstructorDeclExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifConstructorDecl mn = (JifConstructorDecl) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) mn.enterScope(A);
        JifConstructorInstance ci = (JifConstructorInstance) mn.constructorInstance();
    
        /*List argLabels = new ArrayList(mn.formals().size());
        for (int i = 0; i < mn.formals().size(); i++) {
            Formal fi = (Formal) mn.formals().get(i);
            JifLocalInstance li = (JifLocalInstance) fi.localInstance();
            argLabels.add(li.label());
        }
        A = A.enterCall(argLabels, null); //###@@@ FIXME */

        lc = lc.context(A);

        // First, check the arguments, adjusting the context.
        Label Li = checkArguments(ci, lc);
    
        Block body = null;
        PathMap X;
    
        if (! mn.flags().isAbstract() && ! mn.flags().isNative()) {
            // Now, check the body of the method in the new context.
    
            // Visit only the body, not the formal parameters.
            body = checkInitsAndBody(Li, ci, mn.body(), lc);
            X = X(body);
    
            if (Report.should_report(jif_verbose, 3))
            Report.report(3, "Body path labels = " + X);
    
            addReturnConstraints(Li, X, ci, lc, ts.Void());
        }
        else {
            X = ts.pathMap();
            X = X.N(A.entryPC()); //###
        }
    
        mn = (JifConstructorDecl) X(mn.body(body), X);
    
        return mn;
    }

    /** 
     * Utility method to get the set of field instances of final fields of
     * the given <code>ReferenceType</code> that do not have an initializer.
     */
    protected static Set uninitFinalFields(ReferenceType type) {
        Set s = new HashSet();
    
        for (Iterator iter = type.fields().iterator(); iter.hasNext(); ) {
            JifFieldInstance fi = (JifFieldInstance) iter.next();
            if (fi.flags().isFinal() && !fi.hasInitializer()) {
                s.add(fi);
            }
        }
    
        return s;
    }

    /**
     * This method implements the check-inits predicate of the thesis
     * (Figures 4.41-45).
     */
    protected Block checkInitsAndBody(Label Li, 
                                      JifConstructorInstance ci,
                                      Block body, 
                                      LabelChecker lc) throws SemanticException
    {
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();
    
        A = (JifContext) A.pushBlock();
    
        PathMap X = ts.pathMap();
        X = X.N(A.pc());

        // This set is all the uninitialized final variables.
        // These fields need to be initialized before calling the super 
        // constructor, if the super constructor is an "untrusted class", i.e.
        // a Java class that isn't one of JifTypeSystem.trustedNonJifClassNames
        // or a Jif class.
        Set uninitFinalVars = uninitFinalFields(ci.container());

        // let the context know that we are label checking a constructor
        // body, and what the return label of the constructor is.
        A.setCheckingInits(true);
//        lc.checkingInits(true);
    
        Label Lr = ci.returnLabel();
        if (Lr==null) {
            Lr = ts.bottomLabel(ci.position());
        }

        A.setConstructorReturnLabel(JifInstantiator.subst(Lr));

        // stmts is the statements in the constructor body.
        List stmts = new LinkedList();
        
        // The flag preDangerousSuperCall indicates if we are before a call
        // to a "dangerous" super constructor call. A super call is dangerous
        // if the immediate superclass is a Jif class, or if this class has an
        // "untrusted" Java ancestor, 
        //          i.e. ts.hasUntrustedAncestor(ci.container()) != null.
        boolean preDangerousSuperCall = true;

        Iterator iter = body.statements().iterator();
        while (iter.hasNext()) {
            Stmt s = (Stmt) iter.next();
            s = (Stmt) lc.context(A).labelCheck(s);
            stmts.add(s);
            
            PathMap Xs = X(s);
            
            if (preDangerousSuperCall) {
                // we're before a potentially dangerous super call, so we
                // can do check to see if we are assigning to a final label
                // field.
                checkFinalFieldAssignment(s, uninitFinalVars, lc);

                if (s instanceof ConstructorCall) {
                    ConstructorCall ccs = (ConstructorCall) s;

                    if (ccs.kind() == ConstructorCall.SUPER) {    
                        // we are making a super constructor call. Is it
                        // a potentially dangerous one?
                                        
                        if (ts.isJifClass(ci.container()) &&
                            !ts.isJifClass(ci.container().superType()) &&
                            ts.hasUntrustedAncestor(ci.container()) == null) {
                            // Not a potentially dangerous super call.
                            // The immediate super class is a trusted Java
                            // class.
                            // Although there are uninitialized final vars before
                            // the call to super, it's OK, as this is a Jif class, 
                            // the immediate ancestor (and all ancestors) are
                            // "trusted" java classes, which do not access
                            // these fields before they are initialized.
                        } 
                        else {
                            // This is a potentially dangerous super call,
                            // as code in one of the ancestor classes may
                            // access a final field of this class. 
                            preDangerousSuperCall = false;

                            // Let the context know that we are no longer checking field
                            // initializations in the constructor.
                            A.setCheckingInits(false);
                            A.setConstructorReturnLabel(null);    

                            // We must make sure that all final variables of
                            // this class are initialized before the super call.                            
                            for (Iterator i = uninitFinalVars.iterator(); i.hasNext();) {
                                JifFieldInstance fi = (JifFieldInstance)i.next();
                                throw new SemanticException(
                                    "Final variable \""
                                    + fi.name()
                                    + "\" must be initialized before "
                                    + "calling the superclass constructor.",
                                ccs.position());
                            }
                        }
                    }
                }
            }
            
            // At this point, the environment A should have been extended
            // to include any declarations of s.  Reset the PC label.
            A.setPc(Xs.N());
            
            X = X.N(ts.notTaken()).join(Xs);
        }

        // Let the context know that we are no longer checking field
        // initializations in the constructor.
        A.setCheckingInits(false);
        A.setConstructorReturnLabel(null);    

        A = (JifContext) A.pop();
        return (Block) X(body.statements(stmts), X);
    }

    /**
     * Check if the stmt is an assignment to a final field. Moreover, if
     * the final field is a label, and it is being initialized from a final
     * label, share the uids of the fields.
     */
    protected void checkFinalFieldAssignment(Stmt s, Set uninitFinalVars, LabelChecker lc)
    throws SemanticException
    {
        if (!(s instanceof Eval) || !(((Eval)s).expr() instanceof FieldAssign)) {
            // we are not interested in this statement, it's not an assignment
            // to a field
            return;
        }
        
        FieldAssign ass = (FieldAssign)((Eval)s).expr();
        Field f = (Field) ass.left();
        JifFieldInstance assFi = (JifFieldInstance) f.fieldInstance();
            
        if (! (ass.operator() == Assign.ASSIGN &&
               f.target() instanceof Special && 
               ((Special)f.target()).kind() == Special.THIS && 
               assFi.flags().isFinal())) {
            // assignment to something other than a final field of this. 
            return;
        }    
    
        // Remove the field from the set of final vars, since it is
        // initialized here.
        uninitFinalVars.remove(assFi);

        JifTypeSystem ts = lc.jifTypeSystem();
        // deal with label and principal fields being initialzed 
        if (assFi.flags().isFinal() && 
                (ts.isLabel(assFi.type()) || ts.isPrincipal(assFi.type())) && 
                JifUtil.isFinalAccessExprOrConst(ts, ass.right())) {
            
            if (ts.isLabel(assFi.type())) {
                DynamicLabel dl = ts.dynamicLabel(assFi.position(), JifUtil.varInstanceToAccessPath(assFi));                
                Label rhs_label = JifUtil.exprToLabel(ts, ass.right(), lc.context().currentClass());
                lc.context().addDefinitionalAssertionLE(dl, rhs_label);
                lc.context().addDefinitionalAssertionLE(rhs_label, dl);
            }
            if (ts.isPrincipal(assFi.type())) {
                DynamicPrincipal dp = ts.dynamicPrincipal(assFi.position(), JifUtil.varInstanceToAccessPath(assFi));                
                Principal rhs_principal = JifUtil.exprToPrincipal(ts, ass.right(), lc.context().currentClass());
                lc.context().addActsFor(dp, rhs_principal);                    
            }
        }                            

        
        if (ts.isLabel(assFi.type())) {
            // the field is a label. If it is being initialized from a
            // final label, we want to equate the UIDs (Figure 4.45).      
            Expr rhs = ass.right();            
            JifVarInstance rhsVi = null;      
            if (rhs instanceof Field) {
                rhsVi = (JifVarInstance)((Field)rhs).fieldInstance();
            }
            else if (rhs instanceof Local) {
                rhsVi = (JifVarInstance)((Local)rhs).localInstance();
            }
            
            if (rhsVi != null && ts.isLabel(rhsVi.type()) && rhsVi.flags().isFinal()) {
                // the RHS of the assignment is a final label.
                //@@@@@ Here, I think we need to add an assertion to the appropriate hierarchy
                // This probably needs to be done a whole bunch earlier...
            }
        }                    

        // Note that the constraints specified in check-inits for the "v = E"
        // case (Figure 4.44) are added when we visit the statement "s"
        // normally, so we don't need to handle them specially here.
    }
}
