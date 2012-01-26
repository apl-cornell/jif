package jif.extension;

import java.util.ArrayList;

import java.util.List;

import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.qq.QQ;

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
     * return true if this expression is a label ⊑ label comparison.
     * @throws InternalCompilerError
     *         if the left and right sides have not been type-checked  
     */
    public boolean isRelabelsTo() throws InternalCompilerError {
        JifTypeSystem ts = checkTypeChecked();
        
        return node().operator().equals(Binary.LE)
            && ts.isLabel(node().left().type())
            && ts.isLabel(node().right().type());
    }
    
    /**
     * return true if this expression is a principal ≽ principal comparison.
     * @throws InternalCompilerError
     *         if the left and right sides have not been type-checked
     */
    public boolean isActsFor() throws InternalCompilerError
    {
        JifTypeSystem ts = checkTypeChecked();
        
        return node().operator().equals(JifBinaryDel.ACTSFOR)
            && ts.isPrincipal(node().left().type())
            && ts.isPrincipal(node().right().type());
    }
    
    /**
     * return true if this expression is a label ≽ principal comparison. 
     * @return
     * @throws InternalCompilerError
     */
    public boolean isAuthorizes() throws InternalCompilerError {
        JifTypeSystem ts = checkTypeChecked();
        return node().operator().equals(Binary.LE)
            && ts.isPrincipal(node().left().type())
            && ts.isLabel(node().right().type());
    }
    
    /**
     * return true if this expression is a principal ≽ label comparison
     * @throws InternalCompilerError
     *         if the left and right sides have not been type-checked
     */
    public boolean isEnforces() throws InternalCompilerError
    {
        JifTypeSystem ts = checkTypeChecked();
        
        return node().operator().equals(Binary.GE)
            && ts.isPrincipal(node().left().type())
            && ts.isPrincipal(node().right().type());
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

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();

        if (isRelabelsTo())
            return labelCheckAsMethod(lc, ts.relabelsToMethod());
        if (isActsFor())
            return labelCheckAsMethod(lc, ts.actsForMethod());
        if (isAuthorizes())
            return labelCheckAsMethod(lc, ts.authorizesMethod());
        if (isEnforces())
            return labelCheckAsMethod(lc, ts.enforcesMethod());
        
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
    
    /**
     * throws a compiler error if this expression is not type checked, otherwise
     * return the type system.
     */
    private JifTypeSystem checkTypeChecked() throws InternalCompilerError {
        Type lhs = node().left().type();
        Type rhs = node().right().type();
        if (lhs == null || rhs == null)
            throw new InternalCompilerError("Expression not typechecked");

        return (JifTypeSystem) lhs.typeSystem();
    }
    
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
