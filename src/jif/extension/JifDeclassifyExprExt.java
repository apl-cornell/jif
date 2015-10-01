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
 * The Jif extension of the <code>DeclassifyExpr</code> node.
 * 
 * @see jif.ast.DeclassifyExpr
 */
public class JifDeclassifyExprExt extends JifDowngradeExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifDeclassifyExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    protected void checkOneDimenOnly(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        checkOneDimen(lc, A, labelFrom, labelTo, pos, true);
    }

    protected static void checkOneDimen(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos, boolean isExpr)
                    throws SemanticException {
        final String exprOrStmt = (isExpr ? "expression" : "statement");

        JifTypeSystem jts = lc.jifTypeSystem();
        Label topConfLabel = jts.pairLabel(pos, jts.topConfPolicy(pos),
                jts.bottomIntegPolicy(pos));

        lc.constrain(new NamedLabel("declass_from", labelFrom),
                LabelConstraint.LEQ,
                new NamedLabel("declass_to", labelTo).join(lc,
                        "top_confidentiality", topConfLabel),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Declassify " + exprOrStmt
                                + "s cannot downgrade integrity.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The declass_from label has lower integrity than the "
                                + "declass_to label; declassify " + exprOrStmt
                                + "s " + "cannot downgrade integrity.";
                    }
                });
    }

    @Override
    protected void checkAuthority(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        checkAuth(lc, A, labelFrom, labelTo, pos, true);
    }

    protected static void checkAuth(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos, boolean isExpr)
                    throws SemanticException {
        final String exprOrStmt = (isExpr ? "expression" : "statement");

        Label authLabel = A.authLabel();
        lc.constrain(new NamedLabel("declass_from", labelFrom),
                LabelConstraint.LEQ, new NamedLabel("declass_to", labelTo)
                        .join(lc, "auth_label", authLabel),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The method does not have sufficient "
                                + "authority to declassify this " + exprOrStmt
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

                        return "The " + exprOrStmt + " to declassify has label "
                                + namedRhs() + ", and the " + exprOrStmt + " "
                                + "should be downgraded to label "
                                + "declass_to. However, the method has "
                                + "the authority of " + sb.toString() + ". "
                                + "The authority of other principals is "
                                + "required to perform the declassify.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid declassify: the method does "
                                + "not have sufficient authorities.";
                    }
                });
    }

    @Override
    protected void checkRobustness(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        checkRobustDecl(lc, A, labelFrom, labelTo, pos, true);
    }

    protected static void checkRobustDecl(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos, boolean isExpr)
                    throws SemanticException {

        final String exprOrStmt = (isExpr ? "expression" : "statement");

        JifTypeSystem jts = lc.typeSystem();
        Label pcInteg = jts.writersToReadersLabel(pos, A.pc());

        lc.constrain(new NamedLabel("declass_from", labelFrom),
                LabelConstraint.LEQ, new NamedLabel("declass_to", labelTo)
                        .join(lc, "pc_integrity", pcInteg),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Declassification not robust: a new reader "
                                + "may influence the decision to "
                                + "declassify.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The declassification of this " + exprOrStmt
                                + " is "
                                + "not robust; at least one of the principals that is "
                                + "allowed to read the information after "
                                + "declassification may be able to influence the "
                                + "decision to declassify.";
                    }
                });

        Label fromInteg = jts.writersToReadersLabel(pos, labelFrom);
        lc.constrain(new NamedLabel("declass_from_label", labelFrom),
                LabelConstraint.LEQ,
                new NamedLabel("declass_to_label", labelTo).join(lc,
                        "from_label_integrity", fromInteg),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Declassification not robust: a new reader "
                                + "may influence the data to be "
                                + "declassified.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The declassification of this " + exprOrStmt
                                + " is "
                                + "not robust; at least one of the principals that is "
                                + "allowed to read the information after "
                                + "declassification may be able to influence the "
                                + "data to be declassified.";
                    }
                });
    }

    @Override
    void inferLabelFrom(LabelChecker lc, Position pos, JifContext A,
            final DowngradeExpr d, Label inferredFrom, Label exp, Label from)
                    throws SemanticException {
        // need to add these to constrain the following conditions
        // L(e) <= L(inferedFrom) <= L(from)
        // since the first part is checked already, we only check the second
        // part here
        lc.constrain(new NamedLabel("l", inferredFrom), LabelConstraint.LEQ,
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

    // this method infer the right integrity label when no one is provided in
    // declassification
    @Override
    void inferLabelTo(LabelChecker lc, Position pos, JifContext A, Label l,
            Label exp, Label to) throws SemanticException {
        // the integrity label should be in between of I(exp) and I(to)
        // the confidential label should equal to C(to)
        JifTypeSystem jts = lc.jifTypeSystem();
        Label botConfLabel = jts.pairLabel(pos, jts.bottomConfPolicy(pos),
                jts.topIntegPolicy(pos));
        Label botIntegLabel = jts.pairLabel(pos, jts.topConfPolicy(pos),
                jts.bottomIntegPolicy(pos));
        lc.constrain(new NamedLabel("exp_I", lc.lowerBound(exp, botConfLabel)),
                LabelConstraint.LEQ,
                new NamedLabel("l_I", lc.lowerBound(l, botConfLabel)),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The integrity of the expression to declassify is"
                                + " more restrictive than that specified in the declassify to label.";
                    }

                    @Override
                    public String detailMsg() {
                        return "This declassify expression is allowed to declassify"
                                + " integrity labeled up to " + namedRhs()
                                + ". However, the integrity of the "
                                + "expression to declassify is " + namedLhs()
                                + ", which is more restrictive than is "
                                + "allowed.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid declassify: integrity of "
                                + "expression is out of bound.";
                    }
                });
        lc.constrain(new NamedLabel("l_I", lc.lowerBound(l, botConfLabel)),
                LabelConstraint.LEQ,
                new NamedLabel("to_I", lc.lowerBound(to, botConfLabel)),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The integrity of the expression to declassify is"
                                + " more restrictive than that specified in the declassify to label.";
                    }

                    @Override
                    public String detailMsg() {
                        return "This declassify expression is allowed to declassify"
                                + " integrity labeled up to " + namedRhs()
                                + ". However, the integrity of the "
                                + "expression to declassify is " + namedLhs()
                                + ", which is more restrictive than is "
                                + "allowed.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid declassify: integrity of "
                                + "expression is out of bound.";
                    }
                });
        // this constrain may only fail due to the robustness check
        lc.constrain(new NamedLabel("to_C", lc.lowerBound(to, botIntegLabel)),
                LabelConstraint.EQUAL,
                new NamedLabel("l_C", lc.lowerBound(l, botIntegLabel)),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Declassification not robust: a new reader "
                                + "may influence the decision to "
                                + "declassify.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The declassification of this " + "expression"
                                + " is "
                                + "not robust; at least one of the principals that is "
                                + "allowed to read the information after "
                                + "declassification may be able to influence the "
                                + "decision to declassify.";
                    }
                });
    }

    // this method call adds a new constrain on the real downgradeTo label if
    // the integrity part is omitted
    /*
     * void inferLabel(LabelChecker lc, JifContext A, Label l, Label exp, Label
     * to, Position pos) throws SemanticException { // need to add these to
     * constrain the following conditions // l >= to meet (top,bot) // l >= exp
     * meet (bot,top) if (shouldInfer(to)) { JifTypeSystem jts =
     * lc.jifTypeSystem(); Label topConfLabel = jts.pairLabel(pos,
     * jts.topConfPolicy(pos), jts .bottomIntegPolicy(pos)); lc.constrain(new
     * NamedLabel("conf_to", lc.lowerBound(to, topConfLabel)),
     * LabelConstraint.LEQ, new NamedLabel( "infer_to", l), A.labelEnv(), pos,
     * new ConstraintMessage() { public String msg() { return "impossible"; }
     * public String detailMsg() { return "impossible"; } }); Label botConfLabel
     * = jts.pairLabel(pos, jts.bottomConfPolicy(pos), jts.topIntegPolicy(pos));
     * lc.constrain(new NamedLabel("int_to", lc.lowerBound(exp, botConfLabel)),
     * LabelConstraint.LEQ, new NamedLabel( "infer_to", l), A.labelEnv(), pos,
     * new ConstraintMessage() { public String msg() { return "impossible"; }
     * public String detailMsg() { return "impossible"; } }); } else {
     * lc.constrain(new NamedLabel("infer_to", l), LabelConstraint.EQUAL, new
     * NamedLabel("downgrade_to", to), A.labelEnv(), pos, new
     * ConstraintMessage() { public String msg() { return "impossible"; } public
     * String detailMsg() { return "impossible"; } }); } }
     */
}
