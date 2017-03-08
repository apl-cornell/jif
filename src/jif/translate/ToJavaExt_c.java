package jif.translate;

import polyglot.ast.Ext_c;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public abstract class ToJavaExt_c extends Ext_c implements ToJavaExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        return rw;
    }

    public abstract Node toJava(JifToJavaRewriter rw) throws SemanticException;

    @Override
    public Node toJava(JifToJavaRewriter rw, NodeVisitor childVisitor)
            throws SemanticException {
        return toJava(rw);
    }
}
