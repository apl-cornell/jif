package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.Switch;
import polyglot.types.SemanticException;

public class SwitchToJavaExt_c extends ToJavaExt_c {
    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Switch n = (Switch)node();
        return rw.java_nf().Switch(n.position(), n.expr(), n.elements());
    }
}
