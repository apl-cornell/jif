package jif.extension;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Block;
import polyglot.ast.Initializer;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Initializer</code> node. 
 * 
 *  @see polyglot.ast.Initializer
 */
public class JifInitializerExt extends JifExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifInitializerExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Initializer ib = (Initializer) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) ib.del().enterScope(A);

        // @@@@@What this hell is this?
        //!@!
        //Label Li = ts.freshCovariantLabel(ib.position(), "static");
        Label Li = ts.unknownLabel(ib.position()); // temporarily replace with this to get it compiling
        A.setPc(Li, lc);
        A.setCurrentCodePCBound(Li);

        // reset "ph"
        A.clearPH();

        // Now, check the body of the method in the new context.

        Block body = (Block) lc.context(A).labelCheck(ib.body());
        PathMap X = getPathMap(body);

        // X[node()] join X[r] <= Lr (== Li) 
        lc.constrain(
                new NamedLabel("X(initializer).n", X.N()).join(lc,
                        "X(initializer).r", X.R()),
                LabelConstraint.LEQ, new NamedLabel("init_pc", Li),
                A.labelEnv(), ib.position(), false, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The information revealed by the normal "
                                + "termination of the initializer "
                                + "may be more restrictive than the "
                                + "information that should be revealed by "
                                + "the execution of this initializer";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid initializer: " + namedLhs()
                                + " contains secret information.";
                    }
                });

        // X[nv] join X[rv] <= Lrv (== 0)
        // lc.constrainLE(X.NV().join(X.RV()), ts.notTaken(), ib.position());

        return updatePathMap(ib.body(body), X);
    }
}
