package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>ArrayAccessAssign</code> node. 
 */
public class JifArrayAccessAssignExt extends JifAssignExt
{
    public JifArrayAccessAssignExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckLHS(LabelChecker lc)
        throws SemanticException
    {
        ArrayAccessAssign assign = (ArrayAccessAssign)node();
        final ArrayAccess aie = (ArrayAccess) assign.left();
        JifContext A = lc.jifContext();
        A = (JifContext) aie.enterScope(A);
        JifTypeSystem ts = lc.jifTypeSystem();

        List throwTypes = new ArrayList(assign.del().throwTypes(ts));

        if (assign.left() != aie) {
            throw new InternalCompilerError(aie +
                    " is not the left hand side of " + assign);
        }

        Type npe = ts.NullPointerException();
        Type oob = ts.OutOfBoundsException();
        Type ase = ts.ArrayStoreException();
        Type are = ts.ArithmeticException();

        final Expr array = (Expr) lc.context(A).labelCheck(aie.array());
        PathMap Xa = X(array);

        A = (JifContext) A.pushBlock();
        A.setPc(Xa.N());

        Expr index = (Expr) lc.context(A).labelCheck(aie.index());
        PathMap Xb = X(index);

        PathMap X2 = Xa.join(Xb);
        if (!((JifArrayAccessDel)assign.left().del()).arrayIsNeverNull()) {
            // a null pointer exception may be thrown
            checkAndRemoveThrowType(throwTypes, npe);
            X2 = X2.exc(Xa.NV(), npe);             
        }
        if (((JifArrayAccessDel)assign.left().del()).outOfBoundsExcThrown()) {
            // an out of bounds exception may be thrown
            checkAndRemoveThrowType(throwTypes, oob);
             X2 = X2.exc(lc.upperBound(Xa.NV(), Xb.NV()), oob);
        }

        A = (JifContext) A.pushBlock();

        if (assign.operator() != Assign.ASSIGN) {
            A.setPc(X2.N());
        }
        else {
            A.setPc(Xb.N());
        }

        Expr rhs = (Expr) lc.context(A).labelCheck(assign.right());
        PathMap Xv = X(rhs);

        A = (JifContext) A.pop();
        A = (JifContext) A.pop();

        Label La = arrayBaseLabel(array, ts);

        PathMap X;

        
        if (assign.operator() != Assign.ASSIGN) {
            PathMap X3 = X2.N(ts.notTaken()).NV(lc.upperBound(La, X2.NV())).join(Xv);

            if (assign.throwsArithmeticException()) {
                checkAndRemoveThrowType(throwTypes, are);
                X = X3.exc(Xv.NV(), are);
            }
            else {
                X = X3;
            }

            Xv = Xv.join(X);
        }
        else if (assign.throwsArrayStoreException()) {
            checkAndRemoveThrowType(throwTypes, ase);
            X = X2.exc(lc.upperBound(Xa.NV(), Xv.NV()), ase);
        }
        else {
            X = X2;
        }

        NamedLabel namedLa = new NamedLabel("La",
                                            "Label of the array base type",
                                            La);
        lc.constrain(new LabelConstraint(new NamedLabel("rhs.nv", 
                                                        "label of successful evaluation of right hand of assignment",
                                                        Xv.NV()).
                                                   join(lc, 
                                                        "lhs.n", 
                                                        "label of successful evaluation of array access " + aie,
                                                        X.N()), 
                                         LabelConstraint.LEQ, 
                                         namedLa,
                                         A.labelEnv(),
                                         aie.position()) {
                     public String msg() {
                         return "Label of succesful evaluation of array " +
                                "access and right hand side of the " +
                                "assignment is more restrictive than " +
                                "the label for the array base type.";
                     }
                     public String detailMsg() { 
                         return "More information may be revealed by the successul " +
                                "evaluation of the array access " + aie + 
                                " and the right hand side of the assignment " +
                                "than is allowed to flow to elements of the " + 
                                "array. Elements of the array can only " +
                                "contain information up to the label of the " +
                                "array base type, La.";
                     }
                     public String technicalMsg() {
                         return "Invalid assignment: " + namedLhs().toString() + 
                                " is more restrictive than the label of " +
                                "array element.";
                     }                     
         }
         );

        lc.constrain(new LabelConstraint(new NamedLabel("Li", 
                                                        "Lower bound for side-effects", 
                                                        A.currentCodePCBound()), 
                                         LabelConstraint.LEQ, 
                                         namedLa,
                                         A.labelEnv(),
                                         aie.position()) {
                     public String msg() {
                         return "Effect of assignment to array " + array + 
                                " is not bounded below by the PC bound.";
                     }
                     public String detailMsg() { 
                         return "Assignment to the array " + array + 
                                " is a side effect which reveals more" +
                                " information than this method is allowed" +
                                " to; the side effects of this method must" +
                                " be bounded below by the method's PC" +
                                " bound, Li.";
                     }
                     public String technicalMsg() {
                         return "Invalid assignment: Li is more " +
                                "restrictive than array base label.";
                     }
                     
         }
         );

        Expr lhs = (Expr) X(aie.index(index).array(array), X);

        checkThrowTypes(throwTypes);
        return (Assign) X(assign.right(rhs).left(lhs), X);
    }

    private Label arrayBaseLabel(Expr array, JifTypeSystem ts) {        
        ArrayType arrayType = (ArrayType)ts.unlabel(array.type());
        return ts.labelOfType(arrayType.base());	
    }

}
