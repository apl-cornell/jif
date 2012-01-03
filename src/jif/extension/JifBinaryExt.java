package jif.extension;

import java.util.ArrayList;

import java.util.List;

import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.qq.QQ;

/** The Jif extension of the <code>Binary</code> node. 
 *  
 *  @see polyglot.ast.Binary_c
 */
public class JifBinaryExt extends JifExprExt 
{
    public JifBinaryExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();

        List throwTypes = new ArrayList(node().del().throwTypes(ts));

        A = (JifContext) node().del().enterScope(A);

        Expr left = (Expr) lc.context(A).labelCheck(node().left());
        PathMap Xl = getPathMap(left);

        A = (JifContext) A.pushBlock();

        if (node().operator() == Binary.COND_AND || node().operator() == Binary.COND_OR) {
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
     */
    protected Node labelCheckAsMethod(LabelChecker lc, JifProcedureInstance f) throws SemanticException
    {
        // TODO: because we are reusing the method call checking, the error
        // messages produced will be confusing.  Fixing this will require some
        // refactoring.
        
        QQ qq;
        Expr e = qq.parseExpr("%M((%E),(%E))", f, node().left(), node().right());
        Call result = (Call) lc.labelCheck(e);

        Expr left  = (Expr) result.arguments().get(0);
        Expr right = (Expr) result.arguments().get(1);
        PathMap X  = JifUtil.jifExt(result).X();
        
        return updatePathMap(node().left(left).right(right), getPathMap(result));
    }
    
    @Override
    public Binary node()
    {
        return (Binary) super.node();
    }
}
