package jif.extension;

import java.util.*;

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
        A = (JifContext) mn.del().enterScope(A);
        JifConstructorInstance ci = (JifConstructorInstance) mn.constructorInstance();
    
        lc = lc.context(A);

        // First, check the arguments, adjusting the context.
        Label Li = checkEnforceSignature(ci, lc);
    
        Block body = null;
        PathMap X;
    
        // Now, check the body of the method in the new context.

        // Visit only the body, not the formal parameters.
        body = checkInitsAndBody(Li, ci, mn.body(), lc);
        X = X(body);

        if (Report.should_report(jif_verbose, 3))
        Report.report(3, "Body path labels = " + X);

        addReturnConstraints(Li, X, ci, lc, ts.Void());
    
        mn = (JifConstructorDecl) X(mn.body(body), X);
    
        return mn;
    }

    /** 
     * Utility method to get the set of field instances of final fields of
     * the given <code>ReferenceType</code> that do not have an initializer.
     */
    protected static Set uninitFinalFields(ReferenceType type) {
        Set s = new LinkedHashSet();
    
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

        // pc can be set to bottom during the init checking phase.
        A.setPc(ts.bottomLabel()); 

        A.setConstructorReturnLabel(Lr);

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
                checkFinalFieldAssignment(s, uninitFinalVars, A);

                if (s instanceof ConstructorCall) {
                    ConstructorCall ccs = (ConstructorCall) s;

                    if (ccs.kind() == ConstructorCall.SUPER) {    
                        // we are making a super constructor call. Is it
                        // a potentially dangerous one?
                                        
                        if (!ts.isJifClass(ci.container())) {
                            // the class is not a Jif class, but just a signature
                            // for a java class. Don't bother throwing any errors.
                        }
                        else if (ts.isJifClass(ci.container()) &&
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
                            A.setPc(lc.upperBound(A.pc(), ts.callSitePCLabel(ci)));

                            // We must make sure that all final variables of
                            // this class are initialized before the super call.                            
                            for (Iterator i = uninitFinalVars.iterator(); i.hasNext();) {
                                JifFieldInstance fi = (JifFieldInstance)i.next();
                                throw new SemanticDetailedException(
                                    "Final field \"" + fi.name()
                                    + "\" must be initialized before "
                                    + "calling the superclass constructor.",
                                    "All final fields of a class must " +
                                    "be initialized before the superclass " +
                                    "constructor is called, to prevent " +
                                    "ancestor classes from reading " +
                                    "uninitialized final fields. The " +
                                    "final field \"" + fi.name() + "\" needs to " +
                                    "be initialized before the superclass " +
                                    "constructor call.",
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
        A.setPc(lc.upperBound(A.pc(), ts.callSitePCLabel(ci)));

        A = (JifContext) A.pop();
        return (Block) X(body.statements(stmts), X);
    }

    /**
     * Check if the stmt is an assignment to a final field. Moreover, if
     * the final field is a label, and it is being initialized from a final
     * label, share the uids of the fields.
     */
    protected void checkFinalFieldAssignment(Stmt s, Set uninitFinalVars, JifContext A)
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
        
        // Note that the constraints specified in check-inits for the "v = E"
        // case (Figure 4.44) are added when we visit the statement "s"
        // normally, so we don't need to handle them specially here.
    }
}
