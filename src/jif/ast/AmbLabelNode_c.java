package jif.ast;

import polyglot.ast.Ambiguous;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;

/** An ambiguous label node. */
public abstract class AmbLabelNode_c extends LabelNode_c
        implements LabelNode, Ambiguous {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Deprecated
    public AmbLabelNode_c(Position pos) {
        this(pos, null);
    }

    public AmbLabelNode_c(Position pos, Ext ext) {
        super(pos, ext);
    }

    @Override
    public final boolean isDisambiguated() {
        return false;
    }

    /** Disambiguate the type of this node. */
    @Override
    public abstract Node disambiguate(AmbiguityRemover ar)
            throws SemanticException;
}
