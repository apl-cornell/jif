package jif.ast;

import polyglot.ast.Id;
import polyglot.ast.Node_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>JifSingletonAccess</code> interface.
 */
public class JifSingletonAccess_c extends Node_c implements JifSingletonAccess {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSingletonAccess_c(Position pos, Id name) {
        super(pos);
    }
}