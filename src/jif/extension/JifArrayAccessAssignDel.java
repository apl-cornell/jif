package jif.extension;

import java.util.List;

import jif.types.ConstArrayType;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>ArrayAccessAssign</code> node.
 */
public class JifArrayAccessAssignDel extends JifAssignDel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifArrayAccessAssignDel() {
    }

    /**
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in ArrayAccess_c in that it does not
     * throw a null pointer exception if the array is guaranteed to be
     * non-null
     */
    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        // NB: Jif's type system makes arrays invariant on the base type, so no
        // array store exceptions can be thrown.
        ArrayAccessAssign a = (ArrayAccessAssign) node();
        List<Type> l = super.throwTypes(ts);
        if (!((JifArrayAccessDel) a.left().del()).arrayIsNeverNull()) {
            l.add(ts.NullPointerException());
        }

        if (((JifArrayAccessDel) a.left().del()).outOfBoundsExcThrown()) {
            l.add(ts.OutOfBoundsException());
        }

        return l;
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        ArrayAccessAssign aa = (ArrayAccessAssign) super.typeCheck(tc);
        Expr array = aa.left().array();
        if (array.type() instanceof ConstArrayType) {
            ConstArrayType cat = (ConstArrayType) array.type();
            if (cat.isConst()) {
                throw new SemanticException(
                        "Cannot assign to elements of const arrays.",
                        aa.position());
            }
        }
        return aa;
    }

}
