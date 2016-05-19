package jif.extension;

import java.util.LinkedList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.ExceptionPath;
import jif.types.JifContext;
import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.Path;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;

import polyglot.ast.Block;
import polyglot.ast.Catch;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.Try;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

/** Jif extension of the <code>Try</code> node.
 * 
 *  @see polyglot.ast.Try
 */
public class JifTryExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifTryExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        Try trs = (Try) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) trs.del().enterScope(A);

        Block t = checkTry(lc, A, trs.tryBlock());
        PathMap Xs = getPathMap(t);

        PathMap Xall = ts.pathMap();

        // Check catches
        List<Catch> catches = new LinkedList<Catch>();
        for (Catch cb : trs.catchBlocks()) {
            A = (JifContext) A.pushBlock();

            cb = checkCatch(lc, A, Xs, cb);

            A = (JifContext) A.pop();

            Xall = Xall.join(getPathMap(cb));

            catches.add(cb);
        }

        PathMap Xunc = uncaught(Xs, trs, ts);
        Xall = Xall.join(Xunc);

        PathMap X;

        Block f = trs.finallyBlock();

        if (f != null) {
            // Check finally
            f = checkFinally(lc, A, f, Xall);

            PathMap X2 = getPathMap(f);
            //X = Xall.N(ts.notTaken()).join(X2);
            Label finalPath = ts.bottomLabel();
            for (Path p : X2.paths()) {
                finalPath = lc.upperBound(finalPath, X2.get(p));
            }
            // at this point, final path is equal to the upper bound on all paths out of the finally block
            // This should be the normal path of f. Seeing an exception throw by the try or a catch block
            // reveals that the finally block terminated normally.

            for (Path p : Xall.paths()) {
                if (p instanceof ExceptionPath) {
                    Xall = Xall.set(p, lc.upperBound(Xall.get(p), finalPath));
                }
            }

            X = Xall.join(X2);
        } else {
            X = Xall;
        }

        trs = trs.tryBlock(t).catchBlocks(catches).finallyBlock(f);

        return updatePathMap(trs, X);
    }

    private PathMap uncaught(PathMap X, Try trs, JifTypeSystem ts) {
        PathMap Xp = X;

        for (Path p : X.paths()) {
            if (p instanceof ExceptionPath) {
                ExceptionPath jep = (ExceptionPath) p;

                boolean sat = false;

                for (Catch cb : trs.catchBlocks()) {
                    if (ts.isImplicitCastValid(jep.exception(), cb.catchType())
                            || ts.equals(jep.exception(), cb.catchType())) {

                        sat = true;
                        break;
                    }
                }

                if (sat) {
                    Xp = Xp.set(jep, ts.notTaken());
                }
            }
        }

        return Xp;
    }

    private Label excLabel(PathMap X, Type ct, LabelChecker lc,
            JifTypeSystem ts) {

        Label L = ts.bottomLabel(ct.position());

        for (Path p : X.paths()) {
            if (p instanceof ExceptionPath) {
                ExceptionPath ep = (ExceptionPath) p;

                if (ts.isSubtype(ct, ep.exception())
                        || ts.isSubtype(ep.exception(), ct)) {
                    L = lc.upperBound(L, X.get(ep));
                }
            }
        }

        return L;
    }

    /**
     * Abstracted out for overriding in extensions like Fabric.
     */
    protected Catch checkCatch(LabelChecker lc, JifContext A, PathMap Xtry,
            Catch cb) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();

        // This adds the formal to the environment.
        Formal f = cb.formal();
        final JifLocalInstance vi = (JifLocalInstance) f.localInstance();
        Label Li = vi.label();

        // use the label of the exception as the pc
        A.setPc(Li, lc);

        // label check the formal
        f = (Formal) lc.context(A).labelCheck(cb.formal());

        // If there is a declared label, bind the label of the formal to
        // be equivalent to it, and force this declared label to
        // be at least as high as the pc flow.
        if (ts.isLabeled(f.type().type())) {
            Label declaredLabel = ts.labelOfType(f.type().type());
            lc.constrain(
                    new NamedLabel("local_label",
                            "inferred label of " + f.name(), Li),
                    LabelConstraint.EQUAL,
                    new NamedLabel("declared label of " + f.name(),
                            declaredLabel),
                    A.labelEnv(), f.position(), false,
                    new ConstraintMessage() {
                        @Override
                        public String msg() {
                            return "Declared label of catch block variable "
                                    + vi.name()
                                    + " is incompatible with label constraints.";
                        }
                    });
        }

        // Constrain the variable label to be at least as much as the exc-label.
        Label pc_i = excLabel(Xtry, cb.catchType(), lc, ts);

        final String catchTypeName =
                ts.unlabel(cb.catchType()).toClass().name();
        lc.constrain(
                new NamedLabel("join(pc|where exc_i could be thrown)",
                        "the information that could be revealed "
                                + "by the exception " + catchTypeName + " "
                                + "being thrown",
                        pc_i),
                LabelConstraint.LEQ,
                new NamedLabel("label_exc_i",
                        "label of variable " + vi.name(), Li),
                A.labelEnv(), f.position(), false, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Label of thrown exceptions of type "
                                + catchTypeName
                                + " not less restrictive than the label of "
                                + vi.name();
                    }

                    @Override
                    public String detailMsg() {
                        return "More information may be revealed by an exception of "
                                + "type " + catchTypeName
                                + " being thrown than is "
                                + "allowed to flow to " + vi.name() + ".";
                    }
                });

        Block si = (Block) lc.context(A).labelCheck(cb.body());
        PathMap Xi = getPathMap(si);

        return (Catch) updatePathMap(cb.formal(f).body(si), Xi);
    }

    /**
     * Abstrated to allow for easier overriding.
     */
    protected Block checkFinally(LabelChecker lc, JifContext A, Block f, PathMap
            Xprev) throws SemanticException {
        // the pc of A is the same pc used to label check the try block
        return (Block) lc.context(A).labelCheck(f);
    }

    /**
     * Abstrated to allow for easier overriding.
     */
    protected Block checkTry(LabelChecker lc, JifContext A, Block t) throws
        SemanticException {
        return (Block) lc.context(A).labelCheck(t);
    }
}
