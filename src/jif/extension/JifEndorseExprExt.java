package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.ast.DowngradeExpr;
import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * The Jif extension of the <code>EndorseExpr</code> node.
 * 
 * @see jif.ast.EndorseExpr
 */
public class JifEndorseExprExt extends JifDowngradeExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifEndorseExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    protected void checkOneDimenOnly(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        checkOneDimen(lc, A, labelFrom, labelTo, pos, true, false);
    }

    protected static void checkOneDimen(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos, boolean isExpr,
            final boolean isAutoEndorse) throws SemanticException {
        final String exprOrStmt = (isExpr ? "expression" : "statement");
        JifTypeSystem jts = lc.jifTypeSystem();
        Label botIntegLabel = jts.pairLabel(pos, jts.topConfPolicy(pos),
                jts.bottomIntegPolicy(pos));

        lc.constrain(
                new NamedLabel(isAutoEndorse ? "pcBound" : "endorse_from",
                        labelFrom).meet(lc, "bottom_integ", botIntegLabel),
                LabelConstraint.LEQ,
                new NamedLabel(isAutoEndorse ? "autoendorse_to" : "endorse_to",
                        labelTo),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        if (isAutoEndorse)
                            return "Auto-endorse cannot downgrade confidentiality.";
                        return "Endorse " + exprOrStmt
                                + "s cannot downgrade confidentiality.";
                    }

                    @Override
                    public String detailMsg() {
                        if (isAutoEndorse)
                            return "The auto endorse label has lower confidentiality than the start label of the method.";
                        return "The endorse_to label has lower confidentiality than the "
                                + "endorse_from label; endorse " + exprOrStmt
                                + "s " + "cannot downgrade confidentiality.";
                    }
                });
    }

    @Override
    protected void checkAuthority(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        checkAuth(lc, A, labelFrom, labelTo, pos, true, false);
    }

    protected static void checkAuth(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos, boolean isExpr,
            final boolean isAutoEndorse) throws SemanticException {
        Label authLabel = A.authLabelInteg();

        final String exprOrStmt = (isExpr ? "expression" : "statement");
        lc.constrain(
                new NamedLabel(isAutoEndorse ? "pcBound" : "endorse_from",
                        labelFrom).meet(lc, "auth_label", authLabel),
                LabelConstraint.LEQ,
                new NamedLabel(isAutoEndorse ? "autoendorse_to" : "endorse_to",
                        labelTo),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The method does not have sufficient "
                                + "authority to "
                                + (isAutoEndorse ? "auto-endorse this method"
                                        : "endorse this " + exprOrStmt)
                                + ".";
                    }

                    @Override
                    public String detailMsg() {
                        StringBuffer sb = new StringBuffer();
                        Set<Principal> authorities = A.authority();
                        if (authorities.isEmpty()) {
                            sb.append("no principals");
                        } else {
                            sb.append("the following principals: ");
                        }
                        for (Iterator<Principal> iter =
                                authorities.iterator(); iter.hasNext();) {
                            Principal p = iter.next();
                            sb.append(p.toString());
                            if (iter.hasNext()) {
                                sb.append(", ");
                            }
                        }

                        if (isAutoEndorse) {
                            return "The start label of this method is "
                                    + namedLhs()
                                    + ", and the auto-endorse label is "
                                    + namedRhs() + ". However, the method has "
                                    + "the authority of " + sb.toString() + ". "
                                    + "The authority of other principals is "
                                    + "required to perform the endorse.";
                        }

                        return "The " + exprOrStmt + " to endorse has label "
                                + namedLhs() + ", and the " + exprOrStmt + " "
                                + "should be downgraded to label " + namedRhs()
                                + ". However, the method has "
                                + "the authority of " + sb.toString() + ". "
                                + "The authority of other principals is "
                                + "required to perform the endorse.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid endorse: the method does "
                                + "not have sufficient authorities.";
                    }
                });
    }

    @Override
    protected void checkRobustness(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        checkRobustEndorse(lc, A, labelFrom, labelTo, pos, true);
    }

    protected static void checkRobustEndorse(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos, boolean isExpr)
                    throws SemanticException {

        JifTypeSystem jts = lc.typeSystem();
        final String exprOrStmt = (isExpr ? "expression" : "statement");
        Label pcInteg = lc.upperBound(A.pc(), jts.pairLabel(pos,
                jts.topConfPolicy(pos), jts.bottomIntegPolicy(pos)));

        lc.constrain(
                new NamedLabel("endorse_from_label", labelFrom).meet(lc,
                        "pc_integrity", pcInteg),
                LabelConstraint.LEQ,
                new NamedLabel("endorse_to_label", labelTo), A.labelEnv(), pos,
                new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Endorsement not robust: a removed writer "
                                + "may influence the decision to " + "endorse.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The endorsement of this " + exprOrStmt + " is "
                                + "not robust; at least one of the principals that is "
                                + "regarded as no longer influencing the information after "
                                + "endorsement may be able to influence the "
                                + "decision to endorse.";
                    }
                });
    }

    // since no elegant solution similar to declassification exists now, just use from label as the inferred label
    @Override
    void inferLabelFrom(LabelChecker lc, Position pos, JifContext A,
            final DowngradeExpr d, Label inferredFrom, Label exp, Label from)
                    throws SemanticException {
        lc.constrain(new NamedLabel("l", inferredFrom), LabelConstraint.EQUAL,
                new NamedLabel("from", from), A.labelEnv(), pos,
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

    @Override
    // since no elegant solution similar to declassification exists now, just use to label as the inferred label
    void inferLabelTo(LabelChecker lc, Position pos, JifContext A,
            Label inferred, Label exp, Label to) throws SemanticException {
        // to = inferred
        lc.constrain(new NamedLabel("to label", to), LabelConstraint.EQUAL,
                new NamedLabel("infered to label", inferred), A.labelEnv(), pos,
                new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The confidentiality of the expression to endorse is"
                                + "more restrictive than that specified in the endorse to label.";
                    }

                    @Override
                    public String detailMsg() {
                        return "This endorse expression is allowed to endorse"
                                + " confidentiality labeled up to " + namedRhs()
                                + ". However, the confidentiality of the "
                                + "expression to endorse is " + namedLhs()
                                + ", which is more restrictive than is "
                                + "allowed.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid endorse: confidentiality of "
                                + "expression is out of bound.";
                    }
                });
    }

    // this method call adds a new constrain on the real downgradeTo label if
    // the confidential part is omitted
    /*
     * void inferLabel(LabelChecker lc, JifContext A, Label l, Label exp, Label
     * to, Position pos) throws SemanticException { // need to add these to
     * constrain the following conditions // l >= to meet (bot,top) // l >= exp
     * meet (top,bot) if (shouldInfer(to)) { JifTypeSystem jts =
     * lc.jifTypeSystem(); Label topIntegLabel = jts.pairLabel(pos,
     * jts.bottomConfPolicy(pos), jts.topIntegPolicy(pos)); lc.constrain(new
     * NamedLabel("conf_to", lc.lowerBound(to, topIntegLabel)),
     * LabelConstraint.LEQ, new NamedLabel( "infer_to", l), A.labelEnv(), pos,
     * new ConstraintMessage() { public String msg() { return "impossible"; }
     * public String detailMsg() { return "impossible"; } }); Label
     * botIntegLabel = jts.pairLabel(pos, jts.topConfPolicy(pos),
     * jts.bottomIntegPolicy(pos)); lc.constrain(new NamedLabel("int_to",
     * lc.lowerBound(exp, botIntegLabel)), LabelConstraint.LEQ, new NamedLabel(
     * "infer_to", l), A.labelEnv(), pos, new ConstraintMessage() { public
     * String msg() { return "impossible"; } public String detailMsg() { return
     * "impossible"; } }); } else { lc.constrain(new NamedLabel("infer_to", l),
     * LabelConstraint.EQUAL, new NamedLabel("downgrade_to", to), A.labelEnv(),
     * pos, new ConstraintMessage() { public String msg() { return "impossible";
     * } public String detailMsg() { return "impossible"; } }); } }
     */
}
