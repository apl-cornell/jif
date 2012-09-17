package jif.translate;

import polyglot.ast.ArrayAccess;
import polyglot.ast.Node;

public class ArrayAccessToJavaExt_c extends ToJavaExt_c {
    @Override
    public Node toJava(JifToJavaRewriter rw) {
        ArrayAccess n = (ArrayAccess)node();
        return rw.java_nf().ArrayAccess(n.position(), n.array(), n.index());
    }
}
