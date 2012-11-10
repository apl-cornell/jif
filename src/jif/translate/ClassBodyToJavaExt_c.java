package jif.translate;

import polyglot.ast.ClassBody;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ClassBodyToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ClassBody cb = (ClassBody) node();
        return rw.java_nf().ClassBody(cb.position(), cb.members());
    }
}
