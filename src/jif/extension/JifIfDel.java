package jif.extension;

import jif.ast.*;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Binary;
import polyglot.ast.If;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeChecker;

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
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        If ifNode = (If)node();
        if (ifNode.cond() instanceof Binary && ((Binary)ifNode.cond()).operator() == Binary.LE) {
            // could have a label if...
            Binary b = (Binary)ifNode.cond();
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
            
            if (ts.isLabel(b.left().type()) || ts.isLabel(b.right().type())) {
                // replace the "if (L <= L') { ... } else { ... }" node with
                // a LabelIf node.
                JifNodeFactory nf = (JifNodeFactory)tc.nodeFactory();
                LabelExpr lhs;
                if (b.left() instanceof LabelExpr) {
                    lhs = (LabelExpr)b.left();
                }
                else {
                    if (!JifUtil.isFinalAccessExprOrConst(ts, b.left())) {
                        throw new SemanticException(
                                "An expression used in a label test must either be a final access path or a \"new label\"",
                                b.left().position());
                    }
                    lhs = nf.LabelExpr(b.left().position(), 
                                       JifUtil.exprToLabel(ts, b.left(), tc.context().currentClass()));
                }
                LabelExpr rhs;
                if (b.right() instanceof LabelExpr) {
                    rhs = (LabelExpr)b.right();
                }
                else {
                    if (!JifUtil.isFinalAccessExprOrConst(ts, b.right())) {
                        throw new SemanticException(
                                "An expression used in a label test must either be a final access path or a \"new label\"",
                                b.right().position());
                    }
                    rhs = nf.LabelExpr(b.right().position(),
                                       JifUtil.exprToLabel(ts, b.right(), tc.context().currentClass()));
                }
                LabelIf labelIf = nf.LabelIf(ifNode.position(), lhs, rhs, ifNode.consequent(), ifNode.alternative());
                
                // now typecheck the label-if node.
                return labelIf.visit(tc);
                
            }
        }
        return super.typeCheck(tc);
    }
}