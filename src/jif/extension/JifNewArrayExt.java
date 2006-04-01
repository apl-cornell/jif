package jif.extension;

import java.util.*;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>NewArray</code> node. 
 * 
 *  @see polyglot.ast.NewArray
 */
public class JifNewArrayExt extends Jif_c 
{
    public JifNewArrayExt(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
	NewArray nae = (NewArray) node();

	JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
	A = (JifContext) nae.del().enterScope(A);

	A = (JifContext) A.pushBlock();

	PathMap Xs = ts.pathMap();
	Xs = Xs.N(A.pc());

	List dims = new LinkedList();

	for (Iterator iter = nae.dims().iterator(); iter.hasNext(); ) {
	    Expr e = (Expr) iter.next(); 
	    e = (Expr) lc.context(A).labelCheck(e);
	    dims.add(e);

	    PathMap Xe = X(e);
	    Xs = Xs.N(ts.notTaken()).join(Xe);

	    A.setPc(Xs.N());
	}

	ArrayInit init = null;

	if (nae.init() != null) {
	    init = (ArrayInit) lc.context(A).labelCheck(nae.init());
            ((JifArrayInitExt)(init.ext())).labelCheckElements(lc, nae.type()); 
	    PathMap Xinit = X(init);
	    Xs = Xs.N(ts.notTaken()).join(Xinit);
	}

        A = (JifContext) A.pop();

	return X(nae.dims(dims).init(init), Xs);
    }
}
