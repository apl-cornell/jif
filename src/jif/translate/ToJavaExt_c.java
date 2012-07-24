package jif.translate;

import polyglot.ast.Ext_c;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.NodeVisitor;

public abstract class ToJavaExt_c extends Ext_c implements ToJavaExt {
    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        return rw;
    }

    @Override
    public abstract Node toJava(JifToJavaRewriter rw) throws SemanticException;
}
