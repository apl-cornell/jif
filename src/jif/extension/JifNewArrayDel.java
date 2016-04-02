package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.types.JifTypeSystem;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;
import polyglot.util.SubtypeSet;
import polyglot.visit.TypeChecker;

public class JifNewArrayDel extends JifDel_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        NewArray na = (NewArray) super.typeCheck(tc);
        if (na.type().isArray()) {
            // strip off the label of the base type, and replace them with variables, and
            // replace the array types with array types that are both const and non-const
            JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
            na = (NewArray) na.type(
                    JifArrayInitDel.relabelBaseType(na.type().toArray(), ts));

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
    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> l = new ArrayList<Type>(1);
        try {
            Type nase = ts.typeForName("java.lang.NegativeArraySizeException");
            if (!noNegArraySizeExcThrown() && !fatalExceptions.contains(nase)) {
                l.add(nase);
            }
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Cannot find class java.lang.NegativeArraySizeException",
                    e);
        }
        return l;
    }

    private boolean noNegArraySizeExcThrown = false;

    public void setNoNegArraySizeExcThrown() {
        noNegArraySizeExcThrown = true;
    }

    public boolean noNegArraySizeExcThrown() {
        return noNegArraySizeExcThrown;
    }

    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        super.setFatalExceptions(ts, fatalExceptions);
        Type nase;
        try {
            nase = ts.typeForName("java.lang.NegativeArraySizeException");
            if (fatalExceptions.contains(nase)) setNoNegArraySizeExcThrown();
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Cannot find class java.lang.NegativeArraySizeException",
                    e);
        }
    }
}
