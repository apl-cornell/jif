package jif.translate;

import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class FieldDeclToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected FieldInstance fi = null;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        FieldDecl n = (FieldDecl) this.node();
        this.fi = n.fieldInstance();
        return super.toJavaEnter(rw);
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        FieldDecl n = (FieldDecl) node();

        // if it is an instance field with an initializing expression, we need
        // the initialiazation to the initializer method.
        if (!n.flags().isStatic() && n.init() != null) {
            Expr init = n.init();
            if (init instanceof ArrayInit) {
                Type base = fi.type().toArray().base();
                init = rw.java_nf().NewArray(Position.compilerGenerated(),
                        rw.typeToJava(base, Position.compilerGenerated()), 1,
                        (ArrayInit) init);
            }
            rw.addInitializer(fi, init);
            n = n.init(null);
        }

        n = rw.java_nf().FieldDecl(n.position(), n.flags(), n.type(), n.id(),
                n.init(), n.javadoc());
        if (n.init() == null && n.flags().isFinal()) {
            // Strip "final" to allow translated constructor to assign to it.
            n = n.flags(n.flags().clear(Flags.FINAL));
        }
        n = n.fieldInstance(null);

        return n;
    }
}
