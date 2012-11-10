package jif.extension;

import java.util.List;

import polyglot.ast.Assign;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>FieldAssign</code> node.
 */
public class JifFieldAssignDel extends JifAssignDel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifFieldAssignDel() {
    }

    /**
     * This differs from the method defined in FieldAssign_c in that it does not
     * throw a null pointer exception if the receiver is guaranteed to be
     * non-null
     */
    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> l = super.throwTypes(ts);

        Assign a = (Assign) node();
        if (!((JifFieldDel) a.left().del()).targetIsNeverNull()) {
            l.add(ts.NullPointerException());
        }

        return l;
    }

}
