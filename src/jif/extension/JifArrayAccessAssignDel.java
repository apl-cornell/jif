package jif.extension;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.ArrayAccessAssign;
import polyglot.types.TypeSystem;

/** The Jif extension of the <code>ArrayAccessAssign</code> node. 
 */
public class JifArrayAccessAssignDel extends JifJL_c {
    public JifArrayAccessAssignDel() {}

    /** 
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in ArrayAccess_c in that it does not
     * throw a null pointer exception if the array is guaranteed to be 
     * non-null
     */
    public List throwTypes(TypeSystem ts) {
        ArrayAccessAssign a = (ArrayAccessAssign)node();
        List l = new ArrayList(5);
        if (a.throwsArrayStoreException()) {
            l.add(ts.ArrayStoreException());
        }

        if (!((JifArrayAccessDel)a.left().del()).arrayIsNeverNull()) {
            l.add(ts.NullPointerException());
        }

        if (((JifArrayAccessDel)a.left().del()).outOfBoundsExcThrown()) {
            l.add(ts.OutOfBoundsException());
        }

        return l;
    }
}
