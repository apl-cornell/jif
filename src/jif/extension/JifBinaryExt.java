package jif.extension;

import java.util.ArrayList;

import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.util.CollectionUtil;

/** The Jif extension of the <code>Binary</code> node. 
 *  
 *  @see polyglot.ast.Binary_c
 */
public class JifBinaryExt extends JifExprExt 
{
    ////////////////////////////////////////////////////////////////////////////
    // new functionality                                                      //
    ////////////////////////////////////////////////////////////////////////////
    
    public JifBinaryExt(ToJavaExt toJava) {
        super(toJava);
    }

    /**
     * return true if this expression is a short-circuiting operator.
     */
    public boolean isShortCircuit() {
        return node().operator() == Binary.COND_AND
            || node().operator() == Binary.COND_OR;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // label checking                                                         //
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();

        JifMethodInstance method = JifBinaryDel.equivalentMethod(ts, node().operator());
        if (method != null)
            return labelCheckAsMethod(lc, method);
        
        List throwTypes = new ArrayList(node().del().throwTypes(ts));

        A = (JifContext) node().del().enterScope(A);

        Expr left = (Expr) lc.context(A).labelCheck(node().left());
        PathMap Xl = getPathMap(left);

        A = (JifContext) A.pushBlock();

        if (isShortCircuit()) {
            // if it's a short circuit evaluation, then
            // whether the right is executed or not depends on the _value_
            // of the left sub-expression.
            A.setPc(Xl.NV(), lc);            
        }
        else {
            // non-short circuit operator, the right sub-expression
            // will always be evaluated, provided the left sub-expression
            // terminated normally.
            A.setPc(Xl.N(), lc);            
        }

        Expr right = (Expr) lc.context(A).labelCheck(node().right());
        PathMap Xr = getPathMap(right);

        A = (JifContext) A.pop();

        PathMap X = Xl.N(ts.notTaken()).join(Xr);

        if (node().throwsArithmeticException()) {
            checkAndRemoveThrowType(throwTypes, ts.ArithmeticException());
            X = X.exc(Xr.NV(), ts.ArithmeticException());
        }

        checkThrowTypes(throwTypes);
        return updatePathMap(node().left(left).right(right), X);
    }
    
    /**
     * Check this node against constraints captured in a method signature.  More
     * specifically, check that f(this.left,this.right) is a valid call.
     * 
     * @param f
     *          a representation of a static method taking two arguments.
     */
    @SuppressWarnings("unchecked")
    protected Node labelCheckAsMethod(LabelChecker lc, JifProcedureInstance f) throws SemanticException
    {
        // TODO: because we are reusing the method call checking, the error
        // messages produced will be confusing.  Fixing this will require some
        // refactoring.
        
        CallHelper helper = new CallHelper(
            /* receiverLabel: ? */          null,
            /* receiver: f.container() */   lc.nodeFactory().CanonicalTypeNode(node().position(), f.container()),
            /* calleeContainer */           f.container(),
            /* procedure: f */              f,
            /* actualArgs: [left, right] */ CollectionUtil.list(node().left(), node().right()),
            /* position: pos             */ node().position());

        helper.checkCall(
            /* lc */              lc,
            /* throwTypes */      f.throwTypes(),
            /* targetMayBeNull */ false);

        helper.bindVarLabels(
            /* label checker  */     lc,
            /* receiver label */     null,
            /* actual arg labels */  childrenVars(lc),
            /* actual type params */ typeParamVars(lc,f));

        Node result = node().left  (helper.labelCheckedArgs().get(0))
                            .right (helper.labelCheckedArgs().get(1))
                            .type  (helper.returnType());

        return updatePathMap(result, helper.X());
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // helper methods                                                         //
    ////////////////////////////////////////////////////////////////////////////
    
    /** map (var -> new VarLabel) [left, right] */
    @SuppressWarnings("unchecked")
    private List<Label> childrenVars(LabelChecker lc) {
        JifTypeSystem ts = lc.typeSystem();
        return CollectionUtil.list(
                ts.freshLabelVariable(node().left().position(),  "lhs", "left hand side of " + node().operator()),
                ts.freshLabelVariable(node().right().position(), "rhs", "right hand side of " + node().operator())
        );
    }
    
    /** map (param -> new VarLabel) (f.params) */
    private static List<Label> typeParamVars(LabelChecker lc, JifProcedureInstance f) {
        // NOTE: this code taken from JifCallDel
        JifTypeSystem ts = lc.typeSystem();

        if (ts.unlabel(f.container()) instanceof JifSubstType) {            
            JifSubstType jst = (JifSubstType)ts.unlabel(f.container());
            List<Label> result = new ArrayList<Label>(jst.instantiatedFrom().formals().size());

            for (Param p : (List<Param>) jst.instantiatedFrom().formals()) {
                VarLabel paramLbl = ts.freshLabelVariable(p.position(), 
                        "param_"+p+"_label",
                        "label of param " + p + " of operator");
                result.add(paramLbl);
            }
            
            return result;
        }
        else {
            return CollectionUtil.list();
        }
    }

    @Override
    public Binary node()
    {
        return (Binary) super.node();
    }
}
