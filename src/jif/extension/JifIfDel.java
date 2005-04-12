package jif.extension;

import jif.ast.JifNodeFactory;
import jif.ast.JifUtil;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Binary;
import polyglot.ast.If;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.AmbiguityRemover;

/**
 * The Jif extension of the <code>FieldAssign</code> node.
 */
public class JifIfDel extends JifJL_c {
    public JifIfDel() {
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        // replace this with an actsfor node if appropriate
        If ifNode = (If)node();
        if (ifNode.cond() instanceof Binary && ((Binary)ifNode.cond()).operator() == JifBinaryDel.ACTSFOR) {
            // replace the "if (.. actsfor ..) { ... } else { ... }" node with
            // an actsfor node.
            JifNodeFactory nf = (JifNodeFactory)ar.nodeFactory();
            JifTypeSystem ts = (JifTypeSystem)ar.typeSystem();
            Binary b = (Binary)ifNode.cond();
            Principal actor= JifUtil.exprToPrincipal(ts, b.left(), ar.context().currentClass());
            Principal granter = JifUtil.exprToPrincipal(ts, b.right(), ar.context().currentClass());
            return nf.ActsFor(ifNode.position(), actor, granter, ifNode.consequent(), ifNode.alternative());
        }
        return super.disambiguate(ar);
    }
}