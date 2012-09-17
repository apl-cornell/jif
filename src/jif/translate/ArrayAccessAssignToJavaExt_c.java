package jif.translate;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.Node;

public class ArrayAccessAssignToJavaExt_c extends ToJavaExt_c {
    @Override
    public Node toJava(JifToJavaRewriter rw) {
        ArrayAccessAssign n = (ArrayAccessAssign)node();
        return rw.java_nf().ArrayAccessAssign(n.position(), (ArrayAccess)n.left(), n.operator(), n.right());
    }
}
