package jif.translate;

import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class FieldAssignToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        FieldAssign n = (FieldAssign) node();
        return rw.java_nf().FieldAssign(n.position(), (Field) n.left(),
                n.operator(), n.right());
    }
}
