package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>If</code> node. 
 * 
 *  @see polyglot.ast.If
 */
public class JifIfExt extends JifStmtExt_c
{
    public JifIfExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
	If is = (If) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) is.enterScope(A);

	Expr e = (Expr) lc.context(A).labelCheck(is.cond());

	PathMap Xe = X(e);
	
	A = (JifContext) A.pushBlock();
	A.setPc(Xe.NV());

	Stmt S1 = (Stmt) lc.context(A).labelCheck(is.consequent());

        A = (JifContext) A.pop();
	PathMap X1 = X(S1);

	Stmt S2 = null;
	PathMap X2;

	if (is.alternative() != null) {
	    A = (JifContext) A.pushBlock();
	    A.setPc(Xe.NV());

	    S2 = (Stmt) lc.context(A).labelCheck(is.alternative());

            A = (JifContext) A.pop();
	    X2 = X(S2);
	}
	else {
	    // Simulate the effect of an empty statement.
	    // X0[node() := A[pc := Xe[nv][pc]]] == Xe[nv]
	    X2 = ts.pathMap();
	    X2 = X2.N(Xe.NV());
	}

	/*
	trace("Xe == " + Xe);
	trace("X1 == " + X1);
	trace("X2 == " + X2);
	*/

	PathMap X = Xe.N(ts.notTaken()).join(X1).join(X2);
	X = X.NV(ts.notTaken());
//System.out.println("### X(if) " + X + " " + node().position());        
	return X(is.cond(e).consequent(S1).alternative(S2), X);
    }
}
