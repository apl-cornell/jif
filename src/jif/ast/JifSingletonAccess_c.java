package jif.ast;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.TypeNode;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>JifSingletonAccess</code> interface.
 */
public class JifSingletonAccess_c extends JifNew_c implements
        JifSingletonAccess {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSingletonAccess_c(Position pos, TypeNode objectType,
            List<Expr> args) {
        super(pos, null, objectType, args, null);
    }
}
