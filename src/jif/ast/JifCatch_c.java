package jif.ast;

import jif.extension.JifFormalDel;
import polyglot.ast.Block;
import polyglot.ast.Catch;
import polyglot.ast.Catch_c;
import polyglot.ast.Formal;
import polyglot.ast.NodeOps;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class JifCatch_c extends Catch_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifCatch_c(Position pos, Formal formal, Block body) {
        super(pos, formal, body);
        setIsCatchFormal(formal);
    }

    @Override
    public Catch formal(Formal formal) {
        setIsCatchFormal(formal);
        return super.formal(formal);
    }

    @Override
    protected <N extends Catch_c> N reconstruct(N n, Formal formal, Block body) {
        setIsCatchFormal(formal);
        return super.reconstruct(n, formal, body);
    }

    /**
     * Sets the delegate to know that it is in a catch clause.
     */
    protected static void setIsCatchFormal(Formal formal) {
        NodeOps del = formal.del();
        if (del instanceof JifFormalDel) {
            JifFormalDel jfdel = (JifFormalDel) del;
            jfdel.setIsCatchFormal(true);
        }
    }
}
