package jif.ast;

import java.util.HashSet;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.LabelLeAssertion;
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

/** An implementation of the <tt>LabelLeAssertionNode</tt> interface. */
public class LabelLeAssertionNode_c extends ConstraintNode_c<LabelLeAssertion>
        implements LabelLeAssertionNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelNode lhs;
    protected LabelNode rhs;
    protected final boolean isEquiv;

//    @Deprecated
    public LabelLeAssertionNode_c(Position pos, LabelNode lhs, LabelNode rhs,
            boolean isEquiv) {
        this(pos, lhs, rhs, isEquiv, null);
    }

    public LabelLeAssertionNode_c(Position pos, LabelNode lhs, LabelNode rhs,
            boolean isEquiv, Ext ext) {
        super(pos, ext);
        this.lhs = lhs;
        this.rhs = rhs;
        this.isEquiv = isEquiv;
    }

    /** Gets the lhs label node. */
    @Override
    public LabelNode lhs() {
        return this.lhs;
    }

    /** Returns a copy of this node with the lhs updated. */
    @Override
    public LabelLeAssertionNode lhs(LabelNode lhs) {
        LabelLeAssertionNode_c n = (LabelLeAssertionNode_c) copy();
        n.lhs = lhs;
        if (constraints() != null) {
            LabelLeAssertion c = constraints().iterator().next();
            n = n.setConstraints((JifTypeSystem) c.typeSystem());
        }
        return n;
    }

    /** Gets the rhs principal. */
    @Override
    public LabelNode rhs() {
        return this.rhs;
    }

    /** Returns a copy of this node with the rhs updated. */
    @Override
    public LabelLeAssertionNode rhs(LabelNode rhs) {
        LabelLeAssertionNode_c n = (LabelLeAssertionNode_c) copy();
        n.rhs = rhs;
        if (constraints() != null) {
            LabelLeAssertion c = constraints().iterator().next();
            n = n.setConstraints((JifTypeSystem) c.typeSystem());
        }
        return n;
    }

    /** Reconstructs this node. */
    protected LabelLeAssertionNode_c reconstruct(LabelNode lhs, LabelNode rhs) {
        if (lhs != this.lhs || rhs != this.rhs) {
            LabelLeAssertionNode_c n = (LabelLeAssertionNode_c) copy();
            return (LabelLeAssertionNode_c) n.lhs(lhs).rhs(rhs);
        }

        return this;
    }

    /** Visits the children of this node. */
    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode lhs = visitChild(this.lhs, v);
        LabelNode rhs = visitChild(this.rhs, v);
        return reconstruct(lhs, rhs);
    }

    /**
     * Builds the type of this node.
     *  
     * @throws SemanticException
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (constraints() == null) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
            return setConstraints(ts);
        }

        return this;
    }

    private LabelLeAssertionNode_c setConstraints(JifTypeSystem ts) {
        if (isEquiv) {
            Set<LabelLeAssertion> cs = new HashSet<LabelLeAssertion>();
            cs.add(ts.labelLeAssertion(position(), lhs.label(), rhs.label()));
            cs.add(ts.labelLeAssertion(position(), rhs.label(), lhs.label()));
            return (LabelLeAssertionNode_c) constraints(cs);
        }
        return (LabelLeAssertionNode_c) constraint(
                ts.labelLeAssertion(position(), lhs.label(), rhs.label()));

    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(lhs, w, tr);
        w.write(" ");
        w.write(isEquiv ? "equiv" : "<=");
        w.write(" ");
        print(rhs, w, tr);
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
