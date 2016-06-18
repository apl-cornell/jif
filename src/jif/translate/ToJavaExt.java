package jif.translate;

import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.NodeVisitor;

public interface ToJavaExt extends Ext {
    NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException;

    /**
     * @param childVisitor
     *          The visitor that was used to rewrite node()'s children.
     */
    Node toJava(JifToJavaRewriter rw, NodeVisitor childVisitor)
            throws SemanticException;
}
