package jif.translate;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.Node;
import polyglot.util.SerialVersionUID;

public class ArrayAccessAssignToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) {
        ArrayAccessAssign n = (ArrayAccessAssign) node();
        return rw.java_nf().ArrayAccessAssign(n.position(),
                (ArrayAccess) n.left(), n.operator(), n.right());
    }
}
