package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** Jif extension of the <code>Throw</code> node.
 *  
 *  @see polyglot.ast.Throw
 */
public class JifThrowExt extends JifStmtExt_c
{
    public JifThrowExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	Throw ths = (Throw) node();

        JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
	A = (JifContext) ths.del().enterScope(A);

        List throwTypes = new ArrayList(ths.del().throwTypes(ts));

        Expr e = (Expr) lc.context(A).labelCheck(ths.expr());
	PathMap Xe = X(e);

        checkAndRemoveThrowType(throwTypes, e.type());
	PathMap X = Xe.exc(Xe.NV(), e.type());
	X = X.N(ts.notTaken());
	X = X.NV(ts.notTaken());

        checkThrowTypes(throwTypes);
	return X(ths, X);
    }
}
