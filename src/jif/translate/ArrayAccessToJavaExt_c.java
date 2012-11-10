package jif.translate;

import polyglot.ast.ArrayAccess;
import polyglot.ast.Node;
import polyglot.util.SerialVersionUID;

public class ArrayAccessToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) {
        ArrayAccess n = (ArrayAccess) node();
        return rw.java_nf().ArrayAccess(n.position(), n.array(), n.index());
    }
}
