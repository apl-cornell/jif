package jif.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** The Jif extension of the <code>ArrayInit</code> node. 
 */
public class JifArrayInitExt extends Jif_c
{
    public JifArrayInitExt(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
	ArrayInit init = (ArrayInit) node();

	JifTypeSystem ts = lc.jifTypeSystem();


	JifContext A = lc.jifContext();
        A = (JifContext) init.enterScope(A);

	A = (JifContext) A.pushBlock();

	PathMap X = ts.pathMap();
	X = X.N(A.pc());

	List l = new ArrayList(init.elements().size());

	for (Iterator i = init.elements().iterator(); i.hasNext(); ) {
	    Expr e = (Expr) i.next(); 
	    e = (Expr) lc.context(A).labelCheck(e);
	    l.add(e);

	    PathMap Xe = X(e);
	    X = X.N(ts.notTaken()).join(Xe);

	    A.setPc(X.N());
	}

        A = (JifContext) A.pop();

	return X(init.elements(l), X);
    }
    
    public void labelCheckElements(LabelChecker lc, Type lhsType) throws SemanticException {
        ArrayInit init = (ArrayInit) node();

        // Must check that the initializer is a subtype of the
        // declared type. 
        subtypeChecker.addSubtypeConstraints(lc, init.position(),
                                     lhsType, init.type());

        // Check if we can assign each individual element.
        Type t = lhsType.toArray().base();

        for (Iterator i = init.elements().iterator(); i.hasNext(); ) {
            Expr e = (Expr) i.next();
            Type s = e.type();
    
            if (e instanceof ArrayInit) {
                ((JifArrayInitExt) e.ext()).labelCheckElements(lc, t);
            }

            subtypeChecker.addSubtypeConstraints(lc, e.position(), t, s);
        }        
    }
}
