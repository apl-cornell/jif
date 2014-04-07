/* new-begin */

package jif.extension;

import jif.ast.ReclassifyExpr;
import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.types.label.RifVarLabel;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class JifReclassifyExprExt extends JifExprExt implements Ext {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifReclassifyExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    void inferLabelFrom(LabelChecker lc, Position pos, JifContext A,
            final ReclassifyExpr d, Label inferredFrom, Label exp)
            throws SemanticException {
        // need to add these to constrain the following conditions
        // L(e) <= L(inferedFrom)
        lc.constrain(new NamedLabel("e", exp), LabelConstraint.LEQ,
                new NamedLabel("l", inferredFrom), A.labelEnv(), pos,
                new ConstraintMessage() {
                    //Change these messages!!!!!
                    @Override
                    public String msg() {
                        return "The label of the expression to "
                                + d.downgradeKind()
                                + " is "
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
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        final ReclassifyExpr d = (ReclassifyExpr) node();

        JifContext A = lc.jifContext();
        A = (JifContext) d.del().enterScope(A);

        // get the label on e without pc environment
        Expr e = (Expr) lc.context(A).labelCheck(d.expr());
        PathMap Xe = getPathMap(e);

        // The pc at the point of declassification is dependent on the expression to declassify
        // terminating normally.
        A = (JifContext) A.pushBlock();
        A.setPc(Xe.N(), lc);
        lc = lc.context(A);

        RifVarLabel inferedFrom =
                lc.typeSystem()
                        .freshRifLabelVariable(d.position(),
                                "infered_reclassify_from",
                                "The label of the target label of reclassified expression");
        inferLabelFrom(lc, d.position(), A, d, inferedFrom, Xe.NV());
        RifVarLabel newlbl = inferedFrom.takeTransition(d.actionId());

        //create one more new varlabel newvar
        //add constraint "(d.action) (inferedFrom) <= newvar "
        //update the Xe.NV with the newvar (instead the inferedFrom)

        PathMap X = Xe.NV(lc.upperBound(A.pc(), newlbl));

        return updatePathMap(d.expr(e), X);
    }
}

/* new-end */
