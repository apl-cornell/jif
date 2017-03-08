package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>AmbPrincipalNode</code> interface,
 * representing an ambiguous conjunctive or disjunctive principal. 
 */
public class AmbJunctivePrincipalNode_c extends PrincipalNode_c
        implements AmbJunctivePrincipalNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected PrincipalNode left;
    protected PrincipalNode right;
    protected boolean isConjunction;

//    @Deprecated
    public AmbJunctivePrincipalNode_c(Position pos, PrincipalNode left,
            PrincipalNode right, boolean isConjunction) {
        this(pos, left, right, isConjunction, null);
    }

    public AmbJunctivePrincipalNode_c(Position pos, PrincipalNode left,
            PrincipalNode right, boolean isConjunction, Ext ext) {
        super(pos, ext);
        this.left = left;
        this.right = right;
        this.isConjunction = isConjunction;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    protected <N extends AmbJunctivePrincipalNode_c> N left(N n,
            PrincipalNode left) {
        if (n.left == left) return n;
        n = copyIfNeeded(n);
        n.left = left;
        return n;
    }

    protected <N extends AmbJunctivePrincipalNode_c> N right(N n,
            PrincipalNode right) {
        if (n.right == right) return n;
        n = copyIfNeeded(n);
        n.right = right;
        return n;
    }

    @Override
    public String toString() {
        return left + (isConjunction ? "&" : ",") + right;
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (!left.isDisambiguated() || !right.isDisambiguated()) {
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

        Principal p;
        if (this.isConjunction) {
            p = ts.conjunctivePrincipal(position(), left.principal(),
                    right.principal());
        } else {
            p = ts.disjunctivePrincipal(position(), left.principal(),
                    right.principal());
        }
        return nf.CanonicalPrincipalNode(position(), p);
    }

    /**
     * Visit this term in evaluation order.
     */
    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }

    @Override
    public Term firstChild() {
        return null;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        PrincipalNode l = visitChild(this.left, v);
        PrincipalNode r = visitChild(this.right, v);
        return reconstruct(this, l, r);
    }

    protected <N extends AmbJunctivePrincipalNode_c> N reconstruct(N n,
            PrincipalNode l, PrincipalNode r) {
        n = left(n, l);
        n = right(n, r);
        return n;
    }

}
