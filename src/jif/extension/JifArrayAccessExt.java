package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** The Jif extension of the <code>ArrayAccess</code> node. 
 */
public class JifArrayAccessExt extends Jif_c
{
    public JifArrayAccessExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckIncrement(LabelChecker lc) throws SemanticException
    {
	JifContext A = lc.jifContext();
	final JifTypeSystem ts = lc.jifTypeSystem();

	final ArrayAccess aie = (ArrayAccess) node();

	Type npe = ts.NullPointerException();
	Type oob = ts.OutOfBoundsException();

	final Expr array = (Expr) lc.context(A).labelCheck(aie.array());

        // Xa is the path map for evaluating the array expression
	PathMap Xa = X(array);

	A = (JifContext) A.pushBlock();
	A.setPc(Xa.N());

	Expr index = (Expr) lc.context(A).labelCheck(aie.index());

        // Xb is the path map for evaluating the index expression
	PathMap Xb = X(index);

        A = (JifContext) A.pop();

	Label La = arrayBaseLabel(array, ts);

        // X2 is the path map for the array access expression, including paths
        // for the NullPointerException (thrown if the array is null) and 
        // the ArrayIndexOutOfBoundsException (thrown if the index is less than
        // 0 or not less than the length of the array).
        PathMap X2 = Xa.join(Xb);
        if (!((JifArrayAccessDel)node().del()).arrayIsNeverNull()) {
            // a null pointer exception may be thrown
            X2 = X2.exc(Xa.NV(), npe);             
        }
        if (((JifArrayAccessDel)node().del()).outOfBoundsExcThrown()) {
            // an out of bounds exception may be thrown
             X2 = X2.exc(Xa.NV().join(Xb.NV()), oob);
        }

        // Xv is a path map with paths N and NV equal to the path value for 
        // X2.N.
	PathMap Xv = ts.pathMap();
	Xv = Xv.N(X2.N());
	Xv = Xv.NV(X2.N());

        // X is the pathmap for the increment. 
	PathMap X = X2.N(ts.notTaken()).NV(La.join(X2.NV())).join(Xv);

        lc.constrain(new LabelConstraint(new NamedLabel("X.nv", 
                                                        "Label of the value " + aie,
                                                        X.NV()).
                                                   join("X.n", 
                                                        "label of successful evaluation of array access " + aie,
                                                        X.N()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("La",
                                                        "Label of the array base type",
                                                        La),
                                         A.labelEnv(),
                                         aie.position()) {
                     public String msg() {
                         return "Label of array access is not less " + 
                                "restrictive than the label for the array " +
                                "base type.";
                     }
                     public String detailMsg() { 
                         return "More information is revealed by the successul " +
                                "evaluation of the array access " + aie + 
                                " than is allowed to flow to elements of the " + 
                                "array. Elements of the array can only " +
                                "contain information up to the label of the " +
                                "array base type, La.";
                     }
                     public String technicalMsg() {
                         return "Invalid increment: " + namedLhs() + " is more " +
                                "restrictive than the label of array element.";
                     }                     
         }
         );

	return X(aie.index(index).array(array), X);
    }
 
    public Node labelCheck(LabelChecker lc)
	throws SemanticException
    {
	JifContext A = lc.jifContext();
	JifTypeSystem ts = lc.jifTypeSystem();

	ArrayAccess aie = (ArrayAccess) node();

	Expr array = (Expr) lc.context(A).labelCheck(aie.array());
	PathMap Xa = X(array);

	A = (JifContext) A.pushBlock();
	A.setPc(Xa.N());

	Expr index = (Expr) lc.context(A).labelCheck(aie.index());
	PathMap Xb = X(index);

        A = (JifContext) A.pop();

	Label La = arrayBaseLabel(array, ts);

	Type npe = ts.NullPointerException();
	Type oob = ts.OutOfBoundsException();

        PathMap X2 = Xa.join(Xb);
        if (!((JifArrayAccessDel)node().del()).arrayIsNeverNull()) {
            // a null pointer exception may be thrown
            X2 = X2.exc(Xa.NV(), npe);             
        }
        if (((JifArrayAccessDel)node().del()).outOfBoundsExcThrown()) {
            // an out of bounds exception may be thrown
             X2 = X2.exc(Xa.NV().join(Xb.NV()), oob);
        }

	PathMap X = X2.NV(La.join(X2.NV()));

	return X(aie.index(index).array(array), X);
    }

    private Type arrayType(Expr array, JifTypeSystem ts) {
        Type arrayType = array.type();
        if (array instanceof Local) {
            arrayType = ((Local)array).localInstance().type();	
        }

        return ts.unlabel(arrayType); 
    }

    private Label arrayBaseLabel(Expr array, JifTypeSystem ts) {
        Type arrayType = arrayType(array, ts);	
        return ts.labelOfType(((ArrayType)arrayType).base());	
    }

}
