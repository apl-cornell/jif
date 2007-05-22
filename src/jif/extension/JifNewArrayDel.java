package jif.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jif.types.JifTypeSystem;
import polyglot.ast.Expr;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.visit.TypeChecker;


public class JifNewArrayDel extends JifJL_c
{
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        NewArray na = (NewArray)super.typeCheck(tc);
        if (na.type().isArray()) {
            // strip off the label of the base type, and replace them with variables, and
            // replace the array types with array types that are both const and non-const
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();            
            na = (NewArray)na.type(JifArrayInitDel.relabelBaseType(na.type().toArray(), ts));
            
        }
        return na;
    }
    /** 
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in NewArray_c in that it does not
     * throw a negative array size exception if the indices is guaranteed to be 
     * non-null
     */
    public List throwTypes(TypeSystem ts) {
        List l = new ArrayList(1);
        if (!noNegArraySizeExcThrown()) {
            try {
                l.add(ts.typeForName("java.lang.NegativeArraySizeException"));
            }
            catch (SemanticException e) {
                throw new InternalCompilerError("Cannot find class java.lang.NegativeArraySizeException", e);
            }
        }
        return l;
    }
    /**
     * Check the dim expressions to see if any of them can cause 
     * a NegativeArraySizeException to be thrown
     */
    public boolean noNegArraySizeExcThrown() {
        NewArray na = (NewArray)node();
        List dims = na.dims();
        if (dims == null) return true;
        for (Iterator iter = dims.iterator(); iter.hasNext();) {
            Expr d = (Expr)iter.next();
            JifExprExt ext = (JifExprExt)d.ext();

            Long bound = ext.getNumericLowerBound();
            // if bound is not null, then bound < d
            if (bound == null || bound.longValue() < -1) {
                // the value of d may be less than 0, and so
                // a NegativeArraySizeException may be thrown
                
//                System.err.println("Bound for " +  d + " is " + bound);
                return false;
            }
        }
        return true;
    }
}
