package jif.extension;

import java.util.Collections;
import java.util.List;

import jif.ast.JifNodeFactory;
import jif.ast.LabelExpr;
import jif.ast.PrincipalExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPath;
import jif.types.principal.Principal;
import polyglot.ast.Binary;
import polyglot.ast.Binary.Operator;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Precedence;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;
import polyglot.util.SubtypeSet;
import polyglot.visit.TypeChecker;

public class JifBinaryDel extends JifDel_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public static final Binary.Operator ACTSFOR =
            new Operator("actsfor", Precedence.RELATIONAL);
    public static final Binary.Operator EQUIV =
            new Operator("equiv", Precedence.RELATIONAL);
    private boolean isAEFatal;

    public JifBinaryDel() {
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        Binary b = (Binary) node();
        JifNodeFactory nf = (JifNodeFactory) tc.nodeFactory();
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        boolean leftLabel = ts.isLabel(b.left().type());
        boolean rightLabel = ts.isLabel(b.right().type());
        if ((b.operator() == Binary.LE || b.operator() == EQUIV)
                && (leftLabel || rightLabel)) {
            if (!(leftLabel && rightLabel)) {
                throw new SemanticException(
                        "The operator " + b.operator()
                                + " requires both operands to be labels.",
                        b.position());
            }

            // we have a label comparison
            // make sure that both left and right are LabelExprs.
            LabelExpr lhs;
            if (b.left() instanceof LabelExpr) {
                lhs = (LabelExpr) b.left();
            } else {
                if (!ts.isFinalAccessExprOrConst(b.left())) {
                    throw new SemanticException(
                            "An expression used in a label test must be either a final access path, principal parameter or a constant principal",
                            b.left().position());
                }
                lhs = nf.LabelExpr(b.left().position(), ts.exprToLabel(ts,
                        b.left(), (JifContext) tc.context()));
                lhs = (LabelExpr) lhs.visit(tc);
            }
            LabelExpr rhs;
            if (b.right() instanceof LabelExpr) {
                rhs = (LabelExpr) b.right();
            } else {
                if (!ts.isFinalAccessExprOrConst(b.right())) {
                    throw new SemanticException(
                            "An expression used in a label test must either be a final access path or a \"new label\"",
                            b.right().position());
                }
                rhs = nf.LabelExpr(b.right().position(), ts.exprToLabel(ts,
                        b.right(), (JifContext) tc.context()));
                rhs = (LabelExpr) rhs.visit(tc);
            }
            return b.left(lhs).right(rhs).type(ts.Boolean());
        }

        boolean leftPrinc =
                ts.isImplicitCastValid(b.left().type(), ts.Principal());
        boolean rightPrinc =
                ts.isImplicitCastValid(b.right().type(), ts.Principal());
        if (b.operator() == ACTSFOR) {
            if (!leftPrinc && !leftLabel) {
                throw new SemanticException(
                        "The left-hand side of the " + b.operator()
                                + " must be a label or a principal.",
                        b.left().position());
            }

            if (!rightPrinc) {
                throw new SemanticException(
                        "The right-hand side of the " + b.operator()
                                + " must be a principal.",
                        b.right().position());
            }

            // We have an actsfor comparison.
            Expr lhs = b.left();
            if (leftPrinc) {
                // Make sure the left side is a principal.
                checkPrincipalExpr(tc, lhs);
            } else {
                // Make sure the left side is a LabelExpr.
                if (!(lhs instanceof LabelExpr)) {
                    if (!ts.isFinalAccessExprOrConst(lhs)) {
                        throw new SemanticException(
                                "An expression used in a label test must be "
                                        + "either a final access path or a "
                                        + "\"new label\"",
                                lhs.position());
                    }

                    lhs = nf.LabelExpr(lhs.position(),
                            ts.exprToLabel(ts, lhs, (JifContext) tc.context()));
                }
            }

            // Make sure the right side is a principal.
            checkPrincipalExpr(tc, b.right());
            return b.left(lhs).type(ts.Boolean());
        }

        if (b.operator() == EQUIV && (leftPrinc || rightPrinc)) {
            if (!(leftPrinc && rightPrinc)) {
                throw new SemanticException(
                        "The operator " + b.operator()
                                + " requires both operands to be principals.",
                        b.position());
            }

            // we have a principal equality test
            // make sure that both left and right are principals.
            checkPrincipalExpr(tc, b.left());
            checkPrincipalExpr(tc, b.right());
            return b.type(ts.Boolean());
        }

        if (b.operator() == EQUIV) {
            throw new SemanticException(
                    "The equiv operator requires either both operands to be principals, or both operands to be labels.",
                    b.position());
        }

        return super.typeCheck(tc);
    }

    private void checkPrincipalExpr(TypeChecker tc, Expr expr)
            throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        if (expr instanceof PrincipalExpr) return;

        if (expr.type() != null && expr.type().isCanonical()
                && !ts.isFinalAccessExprOrConst(expr)) {
            // illegal dynamic principal. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap =
                    ts.exprToAccessPath(expr, (JifContext) tc.context());
            ap.verify((JifContext) tc.context());

            // previous line should throw an exception, but throw this just to
            // be safe.
            throw new SemanticDetailedException("Illegal dynamic principal.",
                    "Only final access paths or principal expressions can be used as a dynamic principal. "
                            + "A final access path is an expression starting with either \"this\" or a final "
                            + "local variable \"v\", followed by zero or more final field accesses. That is, "
                            + "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a "
                            + "final local variables, and each field f1 to fn is a final field. A principal expression "
                            + "is either a principal parameter, or an external principal.",
                    expr.position());
        }

        Principal p = ts.exprToPrincipal(ts, expr, (JifContext) tc.context());
        if (!p.isRuntimeRepresentable()) {
            throw new SemanticDetailedException(
                    "A principal used in an actsfor must be runtime-representable.",
                    "Both principals used in an actsfor test must be "
                            + "represented at runtime, since the actsfor test is a dynamic "
                            + "test. The principal " + p
                            + " is not represented at runtime, and thus cannot be used "
                            + "in an actsfor test.",
                    expr.position());
        }
    }

    /**
     *  List of Types of exceptions that might get thrown.
     * 
     *  This differs from the method defined in Field_c in that it does not
     * throw a null pointer exception if the receiver is guaranteed to be
     * non-null
     */
    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        Binary be = (Binary) node();
        if (be.throwsArithmeticException()
                && !fatalExceptions.contains(ts.ArithmeticException())) {
            return Collections.<Type> singletonList(ts.ArithmeticException());
        }
        return Collections.emptyList();
    }

    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        super.setFatalExceptions(ts, fatalExceptions);
        if (fatalExceptions.contains(ts.ArithmeticException()))
            isAEFatal = true;
    }

    public boolean throwsArithmeticException() {
        if (isAEFatal) return false;
        Binary be = (Binary) node();
        return be.throwsArithmeticException();
    }

}
