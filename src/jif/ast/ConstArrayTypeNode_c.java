package jif.ast;

import jif.types.JifTypeSystem;
import polyglot.ast.ArrayTypeNode_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeBuilder;

/**
 * A <code>ConstArrayTypeNode</code> is a type node for a non-canonical
 * const array type.
 */
public class ConstArrayTypeNode_c extends ArrayTypeNode_c implements ConstArrayTypeNode
{
    public ConstArrayTypeNode_c(Position pos, TypeNode base) {
        super(pos, base);
    }

    @Override
    public Node buildTypes(TypeBuilder tb) {
        JifTypeSystem ts = (JifTypeSystem)tb.typeSystem();
        return type(ts.constArrayOf(position(), base.type()));
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) {
        JifTypeSystem ts = (JifTypeSystem)ar.typeSystem();
        NodeFactory nf = ar.nodeFactory();
        if (!base.isDisambiguated()) {
            return this;
        }

        Type baseType = base.type();

        if (! baseType.isCanonical()) {
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
