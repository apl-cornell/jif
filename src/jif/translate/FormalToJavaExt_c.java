package jif.translate;

import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class FormalToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Formal n = (Formal) node();
        Formal newN = rw.nodeFactory().Formal(n.position(), n.flags(), n.type(),
                n.id());
        LocalInstance li = n.localInstance();

        newN = newN.localInstance(
                rw.typeSystem().localInstance(li.position(), li.flags(),
                        rw.typeSystem().unknownType(li.position()), li.name()));
        return newN;
    }
}
