package jif.ast;

import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.LocalInstance;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

public class JifLocalDecl_c extends LocalDecl_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifLocalDecl_c(Position pos, Flags flags, TypeNode type, Id name,
            Expr init) {
        super(pos, flags, type, name, init);
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        TypeNode type = (TypeNode) visitChild(type(), v);
        Id name = (Id) visitChild(id(), v);
        if (v instanceof AmbiguityRemover) {
            // ugly hack to make sure that the local instance
            // has the correct information in it by the time
            // the init expression is disambiguated.
            LocalInstance li = localInstance();
            li.setFlags(flags());
            li.setName(name());
            li.setType(declType());
        }
        Expr init = (Expr) visitChild(init(), v);

        return reconstruct(type, name, init);
    }
}
