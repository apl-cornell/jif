package jif.extension;

import jif.ast.CheckedEndorseStmt_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** The Jif extension of the <code>CheckedEndorseStmt</code> node. 
 * 
 *  @see jif.ast.CheckedEndorseStmt
 */
public class JifCheckedEndorseStmtExt extends JifEndorseStmtExt
{
    public JifCheckedEndorseStmtExt(ToJavaExt toJava) {
        super(toJava);
    }
    
    protected void checkOneDimenOnly(LabelChecker lc, 
            final JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException {
        JifEndorseExprExt.checkOneDimen(lc, A, labelFrom, labelTo, pos, false, false);
    }
    
    protected void checkAuthority(LabelChecker lc, 
            final JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException {
        JifEndorseExprExt.checkAuth(lc, A, labelFrom, labelTo, pos, false, false);
    }
    
    protected void checkRobustness(LabelChecker lc, 
            JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException {
        JifEndorseExprExt.checkRobustEndorse(lc, A, labelFrom, labelTo, pos, false);
    }
    
    protected void checkAdditionalConstraints(LabelChecker lc, 
            JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException { 
        final CheckedEndorseStmt_c d = (CheckedEndorseStmt_c)this.node();
        if (d.expr() != null && !(d.expr() instanceof Local)) {
            throw new SemanticDetailedException("Checked endorse currently only supports locals", 
                                                "This version of Jif only permits local variables to " + 
                                                "be used in the expression for a checked endorse statement.",
                                                d.expr().position());
        }
        // check that the local can be downgraded appropriately.
        Expr e = (Expr) lc.context(A).labelCheck(d.expr());
        PathMap Xe = X(e);

        lc.constrain(new LabelConstraint(new NamedLabel("expr.nv", Xe.NV()), 
                                         LabelConstraint.LEQ, 
                                                 new NamedLabel("downgrade_bound", labelFrom),
                                                 A.labelEnv(),
                                                 d.position(),
                                                 true) {
            public String msg() {
                return "The label of the expression to " + 
                d.downgradeKind()+" is " + 
                "more restrictive than label of data that " +
                "the "+d.downgradeKind()+" expression is allowed to "+d.downgradeKind()+".";
            }
            public String detailMsg() {
                return "This "+d.downgradeKind()+" expression is allowed to " +
                ""+d.downgradeKind()+" information labeled up to " +
                namedRhs() + ". However, the label of the " +
                "expression to "+d.downgradeKind()+" is " +
                namedLhs() + ", which is more restrictive than " +
                "allowed.";
            }
            public String technicalMsg() {
                return "Invalid "+d.downgradeKind()+": NV of the " + 
                "expression is out of bound.";
            }                     
        }
        );               
    }
    protected JifContext bodyContext(JifContext A, Label downgradeFrom, Label downgradeTo) {
        A = super.bodyContext(A, downgradeFrom, downgradeTo);
        final CheckedEndorseStmt_c d = (CheckedEndorseStmt_c)this.node();
        Local l = (Local)d.expr();
        A.addCheckedEndorse(l.localInstance(), downgradeTo);
        return A;
    }
    
    
}
