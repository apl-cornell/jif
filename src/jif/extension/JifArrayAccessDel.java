package jif.extension;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Node;
import polyglot.types.TypeSystem;
import polyglot.util.SubtypeSet;

/** The Jif extension of the <code>ArrayAccess</code> node. 
 */
public class JifArrayAccessDel extends JifJL_c
{
    public JifArrayAccessDel() {
    }

    /**
     * This flag records whether the array access by this array access expression
     * is never null. This flag is by default false, but may be set to true by the
     * dataflow analysis performed by jif.visit.NotNullChecker
     */
    private boolean isArrayNeverNull = false;

    public void setArrayIsNeverNull() {
        isArrayNeverNull = true;
    }

    public boolean arrayIsNeverNull() {
        return isArrayNeverNull;
    }

    /**
     * This flag records whether the array access by this array access expression
     * may throw an OutOfBoundsException. This flag is by default true, but may 
     * be set to false by the dataflow analysis performed by 
     * jif.visit.ArrayIndexChecker
     */
    private boolean isOutOfBoundsExcThrown = true;

    public void setNoOutOfBoundsExcThrown() {
        isOutOfBoundsExcThrown = false;
    }
    public boolean outOfBoundsExcThrown() {
        return isOutOfBoundsExcThrown;
    }

    /** 
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in ArrayAccess_c in that it does not
     * throw a null pointer exception if the array is guaranteed to be 
     * non-null
     */
    @Override
    public List throwTypes(TypeSystem ts) {
        List l = new ArrayList(2);
        if (outOfBoundsExcThrown()) {
            l.add(ts.OutOfBoundsException());
        }
        
        if (!arrayIsNeverNull()
                && !fatalExceptions.contains(ts.NullPointerException())) {
            l.add(ts.NullPointerException());
        }

        return l;
    }

    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        super.setFatalExceptions(ts, fatalExceptions);
        if(fatalExceptions.contains(ts.OutOfBoundsException()))
            setNoOutOfBoundsExcThrown();
        if(fatalExceptions.contains(ts.NullPointerException()))
            setArrayIsNeverNull();
    }
    
    
}
