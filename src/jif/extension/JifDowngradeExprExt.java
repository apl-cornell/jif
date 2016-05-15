package jif.extension;

import jif.JifOptions;
import jif.ast.DowngradeExpr;
import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.main.Options;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>DowngradeExpr</code> node.
 * 
 *  @see jif.ast.DowngradeExpr
 */
public abstract class JifDowngradeExprExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifDowngradeExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    /**
     * TODO: document me!
     * 
     * @throws SemanticException
     */
    protected JifContext declassifyConstraintContext(LabelChecker lc,
            JifContext A, Label downgradeFrom, Label downgradeTo)
                    throws SemanticException {
        return A;
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        final DowngradeExpr d = (DowngradeExpr) node();

        JifContext A = lc.jifContext();
        A = (JifContext) d.del().enterScope(A);

        // get the label on e without pc environment
        Expr e = (Expr) lc.context(A).labelCheck(d.expr());
        PathMap Xe = getPathMap(e);

        Xe = downgradeExprPathMap(lc.context(A), Xe);

        Label downgradeTo = d.label().label();
        Label downgradeFrom = null;

        boolean boundSpecified;
        if (d.bound() != null) {
            boundSpecified = true;
            downgradeFrom = d.bound().label();
        } else {
            boundSpecified = false;
            downgradeFrom = lc.typeSystem().freshLabelVariable(d.position(),
                    "downgrade_from",
                    "The label the downgrade expression is downgrading from");
        }

        Label inferedFrom = lc.typeSystem().freshLabelVariable(d.position(),
                "infered_downgrade_from",
                "The label of the target label of downgraded expression");
        inferLabelFrom(lc, d.position(), A, d, inferedFrom, Xe.NV(),
                downgradeFrom);

        // The pc at the point of declassification is dependent on the expression to declassify
        // terminating normally.
        A = (JifContext) A.pushBlock();
        updateContextAfterExpr(lc, A, Xe);
        lc = lc.context(A);

        Label inferedTo = lc.typeSystem().freshLabelVariable(d.position(),
                "infered_downgrade_to",
                "The label of the target label of downgraded expression");
        inferLabelTo(lc, d.position(), A, inferedTo, Xe.NV(), downgradeTo);
        checkDowngradeFromBound(lc, A, Xe, d, inferedFrom, inferedTo,
                boundSpecified);

        JifContext dA =
                declassifyConstraintContext(lc, A, inferedFrom, inferedTo);
        checkOneDimenOnly(lc, dA, inferedFrom, inferedTo, d.position());

        checkAuthority(lc, dA, inferedFrom, inferedTo, d.position());

        if (!((JifOptions) Options.global).nonRobustness) {
            checkRobustness(lc, dA, inferedFrom, inferedTo, d.position());
        }
        PathMap X = Xe.NV(lc.upperBound(dA.pc(), inferedTo));

        return updatePathMap(d.expr(e), X);
    }

    /**
     * Utility method for updating the context after checking the expression.
     *
     * Useful for overriding in projects like fabric.
     */
    public void updateContextAfterExpr(LabelChecker lc, JifContext A, PathMap Xe) {
        A.setPc(Xe.N(), lc);
    }

    abstract void inferLabelFrom(LabelChecker lc, Position pos, JifContext A,
            DowngradeExpr d, Label inferredFrom, Label exp, Label from)
                    throws SemanticException;

    abstract void inferLabelTo(LabelChecker lc, Position pos, JifContext A,
            Label l, Label exp, Label to) throws SemanticException;

    /**
     * @throws SemanticException
     * 
     */
    protected void checkDowngradeFromBound(LabelChecker lc, JifContext A,
            PathMap Xe, final DowngradeExpr d, Label downgradeFrom,
            Label downgradeTo, boolean boundSpecified)
                    throws SemanticException {
        Label from = downgradeFrom;
        lc.constrain(new NamedLabel("expr.nv", Xe.NV()),
                boundSpecified ? LabelConstraint.LEQ : LabelConstraint.EQUAL,
                new NamedLabel("downgrade_bound", from), A.labelEnv(),
                d.position(),
                boundSpecified, /* report this constraint if the bound was specified*/
                new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The label of the expression to "
                                + d.downgradeKind() + " is "
                                + "more restrictive than the label of data that "
                                + "the " + d.downgradeKind()
                                + " expression is allowed to "
                                + d.downgradeKind() + ".";
                    }

                    @Override
                    public String detailMsg() {
                        return "This " + d.downgradeKind()
                                + " expression is allowed to " + ""
                                + d.downgradeKind()
                                + " information labeled up to " + namedRhs()
                                + ". However, the label of the "
                                + "expression to " + d.downgradeKind() + " is "
                                + namedLhs()
                                + ", which is more restrictive than is "
                                + "allowed.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid " + d.downgradeKind() + ": NV of the "
                                + "expression is out of bound.";
                    }
                });
    }

    /**
     * TODO: document me!
     * 
     * @throws SemanticException
     */
    protected PathMap downgradeExprPathMap(LabelChecker lc, PathMap Xe)
            throws SemanticException {
        return Xe;
    }

    /**
     * Check that only the integrity/confidentiality is downgraded, and not
     * the other dimension.
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException
     */
    protected abstract void checkOneDimenOnly(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException;

    /**
     * Check the authority condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException
     */
    protected abstract void checkAuthority(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException;

    /**
     * Check the robustness condition
     * @param lc
     * @param labelFrom
     * @param labelTo
     * @throws SemanticException
     */
    protected abstract void checkRobustness(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException;
}
