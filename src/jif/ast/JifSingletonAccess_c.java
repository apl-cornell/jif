package jif.ast;

import polyglot.ast.Id;
import polyglot.ast.Expr;
import polyglot.ast.TypeNode;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

import java.util.List;

/** An implementation of the <code>JifSingletonAccess</code> interface.
 */
public class JifSingletonAccess_c extends JifNew_c implements JifSingletonAccess {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSingletonAccess_c(Position pos, TypeNode objectType,
            List<Expr> args) {
        super(pos, objectType, args, null);
    }
}