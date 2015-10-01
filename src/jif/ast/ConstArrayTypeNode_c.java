package jif.ast;

import jif.types.JifTypeSystem;
import polyglot.ast.ArrayTypeNode_c;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeBuilder;

/**
 * A <code>ConstArrayTypeNode</code> is a type node for a non-canonical
 * const array type.
 */
// XXX should be replaced with extension 
@Deprecated
public class ConstArrayTypeNode_c extends ArrayTypeNode_c
        implements ConstArrayTypeNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public ConstArrayTypeNode_c(Position pos, TypeNode base) {
        this(pos, base, null);
    }

    public ConstArrayTypeNode_c(Position pos, TypeNode base, Ext ext) {
        super(pos, base, ext);
    }

    @Override
    public Node buildTypes(TypeBuilder tb) {
        JifTypeSystem ts = (JifTypeSystem) tb.typeSystem();
        return type(ts.constArrayOf(position(), base.type()));
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) {
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        NodeFactory nf = ar.nodeFactory();
        if (!base.isDisambiguated()) {
            return this;
        }

        Type baseType = base.type();

        if (!baseType.isCanonical()) {
            return this;
        }

        return nf.CanonicalTypeNode(position(),
                ts.constArrayOf(position(), baseType));
    }

    @Override
    public String toString() {
        return base.toString() + "const []";
    }
}
