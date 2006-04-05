package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.JifInstantiator;
import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.*;

/** The Jif extension of the <code>LocalAssign</code> node. 
 */
public class JifFieldAssignExt extends JifAssignExt
{
    public JifFieldAssignExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckLHS(LabelChecker lc)
        throws SemanticException
    {
        Assign assign = (Assign) node();
        Field fe = (Field) assign.left();
        
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        // commented out by zlt
        // A = (JifContext) fe.enterScope(A);

        List throwTypes = new ArrayList(assign.del().throwTypes(ts));
        Type npe = ts.NullPointerException();
        Type are = ts.ArithmeticException();

        Receiver target = JifFieldExt.checkTarget(lc, fe);
        PathMap Xe = X(target);

        // check rhs
        A = (JifContext) A.pushBlock();


        Expr rhs = (Expr) lc.context(A).labelCheck(assign.right());
        PathMap Xr = X(rhs);

        A = (JifContext) A.pop();

        PathMap Xa; 
        PathMap X;

        if (assign.throwsArithmeticException()) {
            checkAndRemoveThrowType(throwTypes, are);
            Xa = Xe.join(Xr).exc(Xr.NV(), are);
        }
        else 
            Xa = Xe.join(Xr);	

        if (fe.target() instanceof Special) {
            // explicitly ignore the the evaluation of the target Xe, as it
            // will be tainted with the "this" label.          
            // "this" label <= A.pc
            X = Xr;
        }
        else if (((JifFieldDel)fe.del()).targetIsNeverNull()) {
            // Can't raise a NullPointerException.
            X = Xa;
        }
        else {
            checkAndRemoveThrowType(throwTypes, npe);
            X = Xa.exc(Xe.NV(), npe);
        }
        

        // Must be done after visiting target to get PC right.
        final FieldInstance fi = fe.fieldInstance();
        Label Lf = ts.labelOfField(fi, A.pc());

        if (target instanceof Expr) {
            if (!(target instanceof Special)) {
                Lf = JifInstantiator.instantiate(Lf, A, (Expr)target, JifFieldExt.targetType(ts, A, target, fe), X(target).NV());
            }
            else {
                JifClassType jct = (JifClassType) A.currentClass();
                Lf = JifInstantiator.instantiate(Lf, A, (Expr)target, JifFieldExt.targetType(ts, A, target, fe), jct.thisLabel());
            }
        }

        Label L = Lf;

        if (target instanceof Expr) {
            // instantiate the type of the field
            Type ft = JifInstantiator.instantiate(fe.type(), A, (Expr)target, JifFieldExt.targetType(ts, A, target, fe), X(target).NV());         
            fe = (Field)fe.type(ft);
        }
        
        if (target instanceof Special && A.checkingInits()) {
            // Relax the constraint: instead of X[nv] <= L, use
            // X[nv] <= {L; Lr}, where Lr is the return label of the
            // constructor. We can do this because Lr <= <var this>, 
            // and {L; Lr} <= X(this.f).nv
            Label Lr = A.constructorReturnLabel();

            if (Lr != null) 
                L = lc.upperBound(L, Lr);
            
            // if it is a final field being initialized,
            // add a definitional assertion that the field is equivalent
            // to the expression being assigned to it.
            if (fi.flags().isFinal() && 
                    (ts.isLabel(fi.type()) || ts.isImplicitCastValid(fi.type(), ts.Principal())) && 
                    JifUtil.isFinalAccessExprOrConst(ts, assign.right())) {
                
                if (ts.isLabel(fi.type())) {
                    Label dl = ts.dynamicLabel(fi.position(), JifUtil.varInstanceToAccessPath(fi, fi.position()));                
                    Label rhs_label = JifUtil.exprToLabel(ts, assign.right(), A);
                    A.addDefinitionalAssertionEquiv(dl, rhs_label);
                }
                if (ts.isImplicitCastValid(fi.type(), ts.Principal())) {
                    DynamicPrincipal dp = ts.dynamicPrincipal(fi.position(), JifUtil.varInstanceToAccessPath(fi, fi.position()));                
                    Principal rhs_principal = JifUtil.exprToPrincipal(ts, assign.right(), A);
                    A.addDefinitionalEquiv(dp, rhs_principal);                    
                }
            }                            
            
        }

        lc.constrain(new LabelConstraint(new NamedLabel("rhs.nv", 
                                                        "label of successful evaluation of right hand of assignment", 
                                                        X.NV()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("label of field " + fi.name(), L),
                                         A.labelEnv(),
                                         fe.position()) {
                     public String msg() {
                         return "Label of right hand side not less " + 
                                "restrictive than the label for field " + 
                                fi.name();
                     }
                     public String detailMsg() { 
                         return "More information is revealed by the successful " +
                                "evaluation of the right hand side of the " +
                                "assignment than is allowed to flow to " +
                                "the field " + fi.name() + ".";
                     }
                     public String technicalMsg() {
                         return "Invalid assignment: path NV of rhs is " +
                                "more restrictive than the declared label " +
                                "of the field <" + fi.name() + ">.";
                     }
                     
         }
         );

        if (target instanceof Special && A.checkingInits()) {
            // In constructors, assignments to fields are not
            // considered as side-effects.
        }
        else {
            lc.constrain(new LabelConstraint(new NamedLabel("Li", 
                                                            "Lower bound for side-effects", 
                                                            A.currentCodePCBound()), 
                                             LabelConstraint.LEQ, 
                                             new NamedLabel("label of field " + fi.name(), L),
                                             A.labelEnv(),
                                             fe.position()) {
                         public String msg() {
                             return "Effect of assignment to field " + fi.name() + 
                                    " is not bounded below by the PC bound.";
                         }
                         public String detailMsg() { 
                             return "Assignment to the field " + fi.name() + 
                                    " is a side effect which reveals more" +
                                    " information than this method is allowed" +
                                    " to; the side effects of this method must" +
                                    " be bounded below by the method's PC" +
                                    " bound, Li.";
                         }
                         public String technicalMsg() {
                             return "Invalid assignment: Li is more " +
                                    "restrictive than the declared label " +
                                    "of the field <" + fi.name() + ">.";
                         }
                     
             }
             );
        }

        if (assign.operator() != Assign.ASSIGN) {
            // e.g. f += 1
            X = X.set(Path.NV, lc.upperBound(X.NV(), Lf));
        }

        Expr lhs = (Expr) X(fe, X);

        checkThrowTypes(throwTypes);
        return (Assign) X(assign.right(rhs).left(lhs), X);
    }
    
}
