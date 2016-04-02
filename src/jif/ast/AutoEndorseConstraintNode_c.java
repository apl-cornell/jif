package jif.ast;

import jif.types.AutoEndorseConstraint;
import jif.types.AutoEndorseConstraint_c;
import jif.types.JifTypeSystem;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

public class AutoEndorseConstraintNode_c
        extends ConstraintNode_c<AutoEndorseConstraint>
        implements AutoEndorseConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelNode endorseTo;

//    @Deprecated
    public AutoEndorseConstraintNode_c(Position pos, LabelNode endorseTo) {
        this(pos, endorseTo, null);
    }

    public AutoEndorseConstraintNode_c(Position pos, LabelNode endorseTo,
            Ext ext) {
        super(pos, ext);
        this.endorseTo = endorseTo;
    }

    @Override
    public LabelNode endorseTo() {
        return this.endorseTo;
    }

    @Override
    public AutoEndorseConstraintNode endorseTo(LabelNode endorseTo) {
        return endorseTo(this, endorseTo);
    }

    protected <N extends AutoEndorseConstraintNode_c> N endorseTo(N n,
            LabelNode endorseTo) {
        if (n.endorseTo == endorseTo) return n;
        n = copyIfNeeded(n);
        n.endorseTo = endorseTo;
        if (constraint() != null) {
            n.setConstraint(((AutoEndorseConstraint_c) constraint())
                    .endorseTo(endorseTo.label()));
        }
        return n;
    }

    protected <N extends AutoEndorseConstraintNode_c> N reconstruct(N n,
            LabelNode endorseTo) {
        n = endorseTo(n, endorseTo);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode endorseTo = visitChild(this.endorseTo, v);
        return reconstruct(this, endorseTo);
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (constraint() == null) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
            return constraint(
                    ts.autoEndorseConstraint(position(), endorseTo.label()));
        }

        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("autoendorse(");
        print(endorseTo, w, tr);
        w.write(")");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
