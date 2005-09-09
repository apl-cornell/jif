package jif.extension;

import jif.ast.*;
import jif.types.*;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.label.AccessPath;
import polyglot.ast.*;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
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
            Expr left = b.left();
            Expr right = b.right();
            
            PrincipalNode actor, granter;
            if (left instanceof PrincipalNode)
                actor = (PrincipalNode)left;
            else {
                // try running the type checker on left
                left = checkPrincipalExpr(ar, left);
                actor = nf.AmbPrincipalNode(left.position(), left);
            }
            
            if (right instanceof PrincipalNode) {
                granter = (PrincipalNode)right;
            }
            else {
                // try running the type checker on right
                right = checkPrincipalExpr(ar,right);
                granter = nf.AmbPrincipalNode(right.position(), right);
            }

            actor = (PrincipalNode)actor.disambiguate(ar);
            granter = (PrincipalNode)granter.disambiguate(ar);
            return nf.ActsFor(ifNode.position(), actor, granter, ifNode.consequent(), ifNode.alternative());
        }
        return super.disambiguate(ar);
    }

    private Expr checkPrincipalExpr(AmbiguityRemover ar, Expr expr) throws SemanticException {
        JifNodeFactory nf = (JifNodeFactory)ar.nodeFactory();
        JifTypeSystem ts = (JifTypeSystem)ar.typeSystem();
        TypeChecker tc = new TypeChecker(ar.job(), ts, nf);
        tc = (TypeChecker) tc.context(ar.context());
        expr = (Expr)expr.visit(tc);

        if (expr.type() != null && expr.type().isCanonical() && 
                !JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            // illegal dynamic principal. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap = JifUtil.exprToAccessPath(expr, (JifContext)ar.context()); 
            ap.verify((JifContext)ar.context());

            // previous line should throw an exception, but throw this just to
            // be safe.
            throw new SemanticDetailedException(
                "Illegal dynamic principal.",
                "Only final access paths or principal expressions can be used as a dynamic principal. " +
                "A final access path is an expression starting with either \"this\" or a final " +
                "local variable \"v\", followed by zero or more final field accesses. That is, " +
                "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a " +
                "final local variables, and each field f1 to fn is a final field. A principal expression " +
                "is either a principal parameter, or an external principal.",
                expr.position());                                        
        }
        return expr;        
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
                                "An expression used in a label test must be either a final access path, principal parameter or a constant principal",
                                b.left().position());
                    }
                    lhs = nf.LabelExpr(b.left().position(), 
                                       JifUtil.exprToLabel(ts, b.left(), (JifContext)tc.context()));
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
                                       JifUtil.exprToLabel(ts, b.right(), (JifContext)tc.context()));
                }
                LabelIf labelIf = nf.LabelIf(ifNode.position(), lhs, rhs, ifNode.consequent(), ifNode.alternative());
                
                // now typecheck the label-if node.
                return labelIf.visit(tc);
                
            }
        }
        return super.typeCheck(tc);
    }
}