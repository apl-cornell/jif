package jif.extension;

import jif.ast.JifNodeFactory;
import jif.ast.JifUtil;
import jif.ast.LabelExpr;
import jif.ast.PrincipalExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPath;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ast.Binary.Operator;
import static polyglot.ast.Binary.GE;
import static polyglot.ast.Binary.LE;
import polyglot.types.SemanticException;
import polyglot.visit.TypeChecker;

public class JifBinaryDel extends JifJL_c
{
    // ambiguous operators
    public static final Binary.Operator EQUIV    = new Operator("equiv", Precedence.RELATIONAL);
    public static final Binary.Operator TRUST_GE = new Operator("≽",     Precedence.RELATIONAL);
    // also LE, GE
    
    // disambiguous operators
    public static final Operator RELABELS_TO     = new Operator("flowsto",    Precedence.RELATIONAL);
    public static final Operator ACTSFOR         = new Operator("actsfor",    Precedence.RELATIONAL);
    public static final Operator AUTHORIZES      = new Operator("authorizes", Precedence.RELATIONAL);
    public static final Operator ENFORCES        = new Operator("enforces",   Precedence.RELATIONAL);
    public static final Operator PRINCIPAL_EQUIV = new Operator("(principal) equiv", Precedence.RELATIONAL);
    public static final Operator LABEL_EQUIV     = new Operator("(label)     equiv", Precedence.RELATIONAL);

    public JifBinaryDel() { }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        Binary b = node();
        JifNodeFactory nf = (JifNodeFactory)tc.nodeFactory();
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        boolean leftLabel = ts.isLabel(b.left().type());
        boolean rightLabel = ts.isLabel(b.right().type());
        if ((b.operator() == Binary.LE || b.operator() == EQUIV)
                && (leftLabel || rightLabel)) {
            if (!(leftLabel && rightLabel)) {
                throw new SemanticException("The operator " + b.operator() + " requires both operands to be labels.", b.position());
            }

            // we have a label comparison
            // make sure that both left and right are LabelExprs.
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
                lhs = (LabelExpr)lhs.visit(tc);
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
                rhs = (LabelExpr)rhs.visit(tc);
            }
            return b.left(lhs).right(rhs).type(ts.Boolean());
        }
        
        boolean leftPrinc = ts.isImplicitCastValid(b.left().type(), ts.PrincipalType());
        boolean rightPrinc = ts.isImplicitCastValid(b.right().type(), ts.PrincipalType());
        if (b.operator() == ACTSFOR) {
            if (!leftPrinc && !leftLabel) {
                throw new SemanticException("The left-hand side of the "
                        + b.operator() + " must be a label or a principal.", b.left()
                        .position());
            }
            
            if (!rightPrinc) {
                throw new SemanticException("The right-hand side of the "
                        + b.operator() + " must be a principal.", b.right()
                        .position());
            }
            
            // We have an actsfor comparison.
            Expr lhs = b.left();
            if (leftPrinc) {
                // Make sure the left side is a principal.
                checkPrincipalExpr(tc, lhs);
            } else {
                // Make sure the left side is a LabelExpr.
                if (!(lhs instanceof LabelExpr)) {
                    if (!JifUtil.isFinalAccessExprOrConst(ts, lhs)) {
                        throw new SemanticException(
                                "An expression used in a label test must be "
                                        + "either a final access path or a "
                                        + "\"new label\"", lhs.position());
                    }
                    
                    lhs =
                            nf.LabelExpr(lhs.position(), JifUtil.exprToLabel(
                                    ts, lhs, (JifContext) tc.context()));
                }
            }
            
            // Make sure the right side is a principal.
            checkPrincipalExpr(tc, b.right());
            return b.left(lhs).type(ts.Boolean());
        }
        
        if (b.operator() == EQUIV && (leftPrinc || rightPrinc)) {
            if (!(leftPrinc && rightPrinc)) {
                throw new SemanticException("The operator " + b.operator() + " requires both operands to be principals.", b.position());
            }
            
            // we have a principal equality test
            // make sure that both left and right are principals.
            checkPrincipalExpr(tc, b.left());
            checkPrincipalExpr(tc, b.right());
            return b.type(ts.Boolean());            
        }
        
        if (b.operator() == EQUIV) {
            throw new SemanticException("The equiv operator requires either both operands to be principals, or both operands to be labels.", b.position());
        }
        
        return super.typeCheck(tc);
    }

    /**
     * This uses type information to specify which version of an overloaded
     * operator is intended, and returns an updated node.
     * 
     * @throws SemanticException if the expression is invalid
     */
    private Node disambiguateRelations(JifTypeSystem ts) throws SemanticException {
        
        // the left (l) and right (r) types are either
        // a principal (p), a label (l), or neither (n).
        // thus for example, if l(eft) is a p(rincipal), then lp is true.
        
        boolean lp = ts.isPrincipal(node().left().type());
        boolean ll = ts.isLabel(node().left().type());
        boolean ln = !lp && !ll;
        
        boolean rp = ts.isPrincipal(node().right().type());
        boolean rl = ts.isLabel(node().right().type());
        boolean rn = !rp && !rl;
        
        Operator result = node().operator();
        
        if (node().operator() == GE) {
                 if (lp && rl) result = ENFORCES;        // p >= l
            else if (ll && rp) result = AUTHORIZES;      // l >= p
            else if (lp && rp) result = ACTSFOR;         // p >= p
            else if (ln && rn) result = GE;              // n >= n
            else throw new SemanticException();
        }
        
        else if (node().operator() == LE) {
                 if (ll && rl) result = RELABELS_TO;     // l <= l
            else if (ln && rn) result = LE;              // n <= n
            else throw new SemanticException();
        }
        
        else if (node().operator() == EQUIV) {
                 if (lp && rp) result = PRINCIPAL_EQUIV; // p equiv p
            else if (ll && rl) result = LABEL_EQUIV;     // l equiv l
            else throw new SemanticException();
        }
        
        else if (node().operator() == TRUST_GE) {
                 if (lp && rp) result = ACTSFOR;         // p ≽ p
            else if (ll && rp) result = AUTHORIZES;      // l ≽ p
            else if (lp && rl) result = ENFORCES;        // p ≽ l
            else throw new SemanticException();
        }
        
        return node().operator(result);
    }
    
    private void checkPrincipalExpr(TypeChecker tc, Expr expr) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        if (expr instanceof PrincipalExpr) return;

        if (expr.type() != null && expr.type().isCanonical() && 
                !JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            // illegal dynamic principal. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap = JifUtil.exprToAccessPath(expr, (JifContext)tc.context()); 
            ap.verify((JifContext)tc.context());

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

        Principal p = JifUtil.exprToPrincipal(ts, expr, (JifContext)tc.context());
        if (!p.isRuntimeRepresentable()) {
            throw new SemanticDetailedException(
                    "A principal used in an actsfor must be runtime-representable.",                    
                    "Both principals used in an actsfor test must be " +
                    "represented at runtime, since the actsfor test is a dynamic " +
                    "test. The principal " + p + 
                    " is not represented at runtime, and thus cannot be used " +
                    "in an actsfor test.",
                    expr.position());
        }
        
    }

    @Override
    public Binary node() {
        return (Binary) super.node();
    }
}
