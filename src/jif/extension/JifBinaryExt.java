package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>Binary</code> node. 
 *  
 *  @see polyglot.ext.jl.ast.Binary_c
 */
public class JifBinaryExt extends Jif_c 
{
    public JifBinaryExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
	Binary be = (Binary) node();

	JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
	
	if (ts.isLabel(be.left().type()) || ts.isLabel(be.right().type())) {
	    throw new SemanticException("Label comparison <= can only be used in an if statement, for example \"if (" + be + ") { ... }\"", be.position());
	}
	
        List throwTypes = new ArrayList(be.del().throwTypes(ts));

        A = (JifContext) be.enterScope(A);

	Expr left = (Expr) lc.context(A).labelCheck(be.left());
	PathMap Xl = X(left);

	A = (JifContext) A.pushBlock();

	A.setPc(Xl.N());

	Expr right = (Expr) lc.context(A).labelCheck(be.right());
	PathMap Xr = X(right);

        A = (JifContext) A.pop();

	PathMap X = Xl.set(Path.N, ts.notTaken()).join(Xr);

	if (be.throwsArithmeticException()) {
            checkAndRemoveThrowType(throwTypes, ts.ArithmeticException());
	    X = X.exc(Xr.NV(), ts.ArithmeticException());
	}

        checkThrowTypes(throwTypes);
	return X(be.left(left).right(right), X);
    }
}
