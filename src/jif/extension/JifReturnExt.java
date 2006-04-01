package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Return;
import polyglot.types.*;

/** The Jif extension of the <code>Return</code> node. 
 * 
 *  @see polyglot.ast.Return 
 */
public class JifReturnExt extends JifStmtExt_c
{
    public JifReturnExt(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	Return rs = (Return) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
	A = (JifContext) rs.del().enterScope(A);

	Expr e = null;

	PathMap X;

	if (rs.expr() == null) {
	    X = ts.pathMap();
	    X = X.R(A.pc());
	    X = X.NV(ts.notTaken());
	}
	else {
	    e = (Expr) lc.context(A).labelCheck(rs.expr());

	    PathMap Xe = X(e);

	    PathMap X1 = Xe.N(ts.notTaken());
	    PathMap X2 = ts.pathMap();
	    X2 = X2.R(Xe.N());
	    X2 = X2.RV(Xe.NV());
	    X = X1.join(X2);

	    CodeInstance ci = A.currentCode();

	    if (! (ci instanceof MethodInstance)) {
	        throw new SemanticException(
		    "Cannot return a value from " + ci + ".");
	    }
        
        

	    MethodInstance mi = (MethodInstance) ci;
            // Type retType = A.instantiate(mi.returnType()); 
            Type retType = mi.returnType();

	    // Must check that the expression type is a subtype of the declared
	    // return type.  Most of this is done in typeCheck, but if they are
	    // instantitation types, we must add constraints for the labels.
            subtypeChecker.addSubtypeConstraints(lc.context(A), e.position(),
						 retType, e.type());
	}

	return X(rs.expr(e), X);
    }
}
