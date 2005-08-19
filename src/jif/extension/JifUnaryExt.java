package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/** Jif extension of the <code>Unary</code> node.
 *  
 *  @see polyglot.ast.Unary
 */
public class JifUnaryExt extends Jif_c
{
    public JifUnaryExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
	Unary ue = (Unary) node();

        JifContext A = lc.jifContext();
	A = (JifContext) ue.enterScope(A);

	Expr e = ue.expr();

	if (ue.operator() == Unary.POST_INC ||
	    ue.operator() == Unary.POST_DEC ||
	    ue.operator() == Unary.PRE_INC ||
	    ue.operator() == Unary.PRE_DEC) {

            if (e instanceof Local) {
                e = (Expr)((JifLocalExt)e.ext()).labelCheckIncrement(lc.context(A));
            }
            else if (e instanceof Field) {
                e = (Expr)((JifFieldExt)e.ext()).labelCheckIncrement(lc.context(A));
            }
            else if (e instanceof ArrayAccess) {
                e = (Expr)((JifArrayAccessExt)e.ext()).labelCheckIncrement(lc.context(A));
            }
            else {
                throw new InternalCompilerError("Cannot perform unary operation on a " + e.type());
            }
	}
	else {
	    e = (Expr) lc.context(A).labelCheck(e);
	}

	return X(ue.expr(e), X(e));
    }
}
