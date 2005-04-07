package jif.ast;

import jif.extension.JifFormalDel;
import polyglot.ast.*;
import polyglot.ast.Block;
import polyglot.ast.Catch;
import polyglot.ast.Formal;
import polyglot.ext.jl.ast.Catch_c;
import polyglot.util.Position;

/** An implementation of the <code>JifClassDecl</code> interface.
 */
public class JifCatch_c extends Catch_c
{
    public JifCatch_c(Position pos, Formal formal, Block body) {
        super(pos, formal, body);
        // set the delegate to know that it is in a catch clause
        JL del = formal.del();
        if (del instanceof JifFormalDel) {
            JifFormalDel jfdel = (JifFormalDel)del;
            jfdel.setIsCatchFormal(true);
        }
    }
    
    public Catch formal(Formal formal) {
        // set the delegate to know that it is in a catch clause
        JL del = formal.del();
        if (del instanceof JifFormalDel) {
            JifFormalDel jfdel = (JifFormalDel)del;
            jfdel.setIsCatchFormal(true);
        }

        return super.formal(formal);
    }

    protected Catch_c reconstruct(Formal formal, Block body) {
        // set the delegate to know that it is in a catch clause
        JL del = formal.del();
        if (del instanceof JifFormalDel) {
            JifFormalDel jfdel = (JifFormalDel)del;
            jfdel.setIsCatchFormal(true);
        }

        return super.reconstruct(formal, body);
    }
}