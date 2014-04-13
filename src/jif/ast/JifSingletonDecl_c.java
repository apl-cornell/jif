package jif.ast;

import java.util.List;

import jif.types.Assertion;
import jif.types.JifParsedPolyType;
import polyglot.ast.ClassBody;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.TypeBuilder;

/** An implementation of the <code>JifSingletonDecl</code> interface.
 */
public class JifSingletonDecl_c extends JifClassDecl_c implements
        JifSingletonDecl {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSingletonDecl_c(Position pos, Flags flags, Id name,
            List<ParamDecl> params, TypeNode superClass,
            List<TypeNode> interfaces, List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body) {
        super(pos, flags, name, params, superClass, interfaces, authority,
                constraints, body);

    }

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        JifParsedPolyType ct = (JifParsedPolyType) tb.currentClass();

        if (ct == null) {
            return this;
        }
        ct.setSingleton(true);

        JifSingletonDecl_c n = (JifSingletonDecl_c) super.buildTypes(tb);
        return n;
    }

    @Override
    public String toString() {
        return flags.clearInterface().translate() + ("singleton ") + name + " "
                + body;
    }

}
