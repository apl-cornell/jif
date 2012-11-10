package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
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
public class AmbJunctivePrincipalNode_c extends PrincipalNode_c implements
        AmbJunctivePrincipalNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected PrincipalNode left;
    protected PrincipalNode right;
    protected boolean isConjunction;

    public AmbJunctivePrincipalNode_c(Position pos, PrincipalNode left,
            PrincipalNode right, boolean isConjunction) {
        super(pos);
        this.left = left;
        this.right = right;
        this.isConjunction = isConjunction;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
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
            p =
                    ts.conjunctivePrincipal(position(), left.principal(),
                            right.principal());
        } else {
            p =
                    ts.disjunctivePrincipal(position(), left.principal(),
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
        PrincipalNode l = (PrincipalNode) visitChild(this.left, v);
        PrincipalNode r = (PrincipalNode) visitChild(this.right, v);
        return reconstruct(l, r);
    }

    protected AmbJunctivePrincipalNode_c reconstruct(PrincipalNode l,
            PrincipalNode r) {
        if (this.left == l && this.right == r) {
            return this;
        }
        AmbJunctivePrincipalNode_c n = (AmbJunctivePrincipalNode_c) this.copy();
        n.left = l;
        n.right = r;
        return n;
    }

}
