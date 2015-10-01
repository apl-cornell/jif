package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifMethodInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Return;
import polyglot.types.CodeInstance;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Return</code> node. 
 * 
 *  @see polyglot.ast.Return 
 */
public class JifReturnExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifReturnExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        Return rs = (Return) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) rs.del().enterScope(A);

        Expr e = null;

        PathMap X;

        if (rs.expr() == null) {
            X = ts.pathMap();
            X = X.R(A.pc());
            X = X.NV(ts.notTaken());
        } else {
            e = (Expr) lc.context(A).labelCheck(rs.expr());

            PathMap Xe = getPathMap(e);

            X = Xe.N(ts.notTaken()).NV(ts.notTaken());
            X = X.R(Xe.N());

            CodeInstance ci = A.currentCode();
            if (!(ci instanceof MethodInstance)) {
                throw new SemanticException(
                        "Cannot return a value from " + ci + ".");
            }
            JifMethodInstance mi = (JifMethodInstance) ci;
            // Type retType = A.instantiate(mi.returnType()); 
            final Type retType = mi.returnType();

            Label Lr = lc.upperBound(mi.returnLabel(), ts.callSitePCLabel(mi));
            Label Lrv = null;

            if (ts.isLabeled(retType)) {
                Lrv = lc.upperBound(ts.labelOfType(retType), Lr);
            } else {
                throw new InternalCompilerError(
                        "Unexpected return type: " + retType);
            }

            lc.constrain(
                    new NamedLabel("rv", "the label of the value returned",
                            Xe.NV()),
                    LabelConstraint.LEQ,
                    new NamedLabel("Lrv", "return value label of the method",
                            Lrv),
                    A.labelEnv(), rs.position(), new ConstraintMessage() {
                        @Override
                        public String msg() {
                            return "This method may return a value with "
                                    + "a more restrictive label than the "
                                    + "declared return value label.";
                        }

                        @Override
                        public String detailMsg() {
                            return msg() + " The declared return type "
                                    + "of this method is " + retType
                                    + ". As such, values returned by this "
                                    + "method can have a label of at most "
                                    + namedRhs() + ".";
                        }

                        @Override
                        public String technicalMsg() {
                            return "this method may return a value "
                                    + "with a more restrictive label "
                                    + "than the declared return value label.";
                        }
                    });

            // Must check that the expression type is a subtype of the declared
            // return type.  Most of this is done in typeCheck, but if they are
            // instantitation types, we must add constraints for the labels.
            SubtypeChecker subtypeChecker =
                    new SubtypeChecker(retType, e.type());
            subtypeChecker.addSubtypeConstraints(lc.context(A), e.position());
        }

        return updatePathMap(rs.expr(e), X);
    }
}
