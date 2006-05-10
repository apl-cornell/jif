package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.JifOptions;
import jif.ast.DowngradeExpr;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** The Jif extension of the <code>DowngradeExpr</code> node. 
 * 
 *  @see jif.ast.DowngradeExpr
 */
public abstract class JifDowngradeExprExt extends Jif_c
{
    public JifDowngradeExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected JifContext declassifyConstraintContext(LabelChecker lc, JifContext A, Label downgradeFrom, Label downgradeTo) throws SemanticException {
        return A;
    }
    
    public Node labelCheck(LabelChecker lc) throws SemanticException {
	final DowngradeExpr d = (DowngradeExpr) node();

	JifContext A = lc.jifContext();
        A = (JifContext) d.del().enterScope(A);

	Expr e = (Expr) lc.context(A).labelCheck(d.expr());
	PathMap Xe = X(e);
        
        Xe = downgradeExprPathMap(lc.context(A), Xe);
        
	Label downgradeTo = d.label().label();
        Label downgradeFrom = null;
        boolean boundSpecified;
        if (d.bound() != null) {
            boundSpecified = true;
            downgradeFrom = d.bound().label();
        }
        else {
            boundSpecified = false;
            downgradeFrom = lc.typeSystem().freshLabelVariable(d.position(), 
                                              "downgrade_from", 
                                              "The label the downgrade expression is downgrading from");
        }
        
        
        lc.constrain(new LabelConstraint(new NamedLabel("expr.nv", Xe.NV()), 
                                         boundSpecified?LabelConstraint.LEQ:LabelConstraint.EQUAL, 
                                         new NamedLabel("downgrade_bound", downgradeFrom),
                                         A.labelEnv(),
                                         d.position(),
                                         boundSpecified) /* report this constraint if the bound was specified*/ {
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

        JifContext dA = declassifyConstraintContext(lc, A, downgradeFrom, downgradeTo);
        checkOneDimenOnly(lc, dA, downgradeFrom, downgradeTo, d.position());
        
        checkAuthority(lc, dA, downgradeFrom, downgradeTo, d.position());
        
        if (!((JifOptions)JifOptions.global).noRobustness) {
            checkRobustness(lc, dA, downgradeFrom, downgradeTo, d.position());
        }

        PathMap X = Xe.NV(lc.upperBound(dA.pc(), downgradeTo));           


	return X(d.expr(e), X);
    }

    protected PathMap downgradeExprPathMap(LabelChecker lc, PathMap Xe) throws SemanticException {
        return Xe;
    }

    /**
     * Check that only the integrity/confidentiality is downgraded, and not
     * the other dimension.
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected abstract void checkOneDimenOnly(LabelChecker lc, 
                                           JifContext A,
                                           Label labelFrom, 
                                           Label labelTo, Position pos) 
            throws SemanticException;

    /**
     * Check the authority condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected abstract void checkAuthority(LabelChecker lc, 
                                           JifContext A,
                                           Label labelFrom, 
                                           Label labelTo, Position pos) 
            throws SemanticException;

    /**
     * Check the robustness condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException 
     */
    protected abstract void checkRobustness(LabelChecker lc, 
                                            JifContext A,
                                            Label labelFrom, 
                                            Label labelTo, Position pos) 
        throws SemanticException;
}
