package jif.ast;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign_c;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * A <code>ArrayAccessAssign_c</code> represents a Java assignment expression
 * to an array element.  For instance, <code>A[3] = e</code>.
 * 
 * The class of the <code>Expr</code> returned by
 * <code>ArrayAccessAssign_c.left()</code>is guaranteed to be an
 * <code>ArrayAccess</code>.
 */
@Deprecated
// XXX should be replaced with extension
public class JifArrayAccessAssign_c extends ArrayAccessAssign_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Deprecated
    public JifArrayAccessAssign_c(Position pos, ArrayAccess left, Operator op,
            Expr right) {
        this(pos, left, op, right, null);
    }

    public JifArrayAccessAssign_c(Position pos, ArrayAccess left, Operator op,
            Expr right, Ext ext) {
        super(pos, left, op, right, ext);
    }

    /** Since Jif's type system makes arrays invariant
     * on the base type, no array store exceptions can
     * be thrown. */
    @Override
    public boolean throwsArrayStoreException() {
        return false;
    }

}
