package jif.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jif.ast.JifConstructorDecl;
import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.JifClassType;
import jif.types.JifConstructorInstance;
import jif.types.JifContext;
import jif.types.JifFieldInstance;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.Special;
import polyglot.ast.Stmt;
import polyglot.ast.Unary;
import polyglot.main.Report;
import polyglot.types.FieldInstance;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

/** The Jif extension of the <code>JifConstructorDecl</code> node.
 *
 *  @see polyglot.ast.ConstructorDecl
 *  @see jif.ast.JifConstructorDecl
 */
public class JifConstructorDeclExt extends JifProcedureDeclExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifConstructorDeclExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifConstructorDecl mn = (JifConstructorDecl) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) mn.del().enterScope(A);
        JifConstructorInstance ci =
                (JifConstructorInstance) mn.constructorInstance();

        lc = lc.context(A);

        // check formals
        List<Formal> formals = checkFormals(mn.formals(), ci, lc);

        // First, check the arguments, adjusting the context.
        Label Li = checkEnforceSignature(ci, lc);

        Block body = null;
        PathMap X;

        // Now, check the body of the method in the new context.

        // Visit only the body, not the formal parameters.
        body = checkInitsAndBody(Li, ci, mn.body(), lc);
        X = getPathMap(body);

        if (Report.should_report(jif_verbose, 3))
            Report.report(3, "Body path labels = " + X);

        addReturnConstraints(Li, X, ci, lc, ts.Void());

        mn = (JifConstructorDecl) updatePathMap(mn.formals(formals).body(body),
                X);

        return mn;
    }

    /**
     * Utility method to get the set of field instances of final fields of
     * the given <code>ReferenceType</code> that do not have an initializer.
     */
    protected static Set<JifFieldInstance> uninitFinalFields(
            ReferenceType type) {
        Set<JifFieldInstance> s = new LinkedHashSet<JifFieldInstance>();

        @SuppressWarnings("unchecked")
        List<JifFieldInstance> fields = (List<JifFieldInstance>) type.fields();
        for (JifFieldInstance fi : fields) {
            if (fi.flags().isFinal() && !fi.hasInitializer()) {
                s.add(fi);
            }
        }

        return s;
    }

    /**
     * This method implements the check-inits predicate of the thesis
     * (Figures 4.41-45).
     */
    protected Block checkInitsAndBody(Label Li, JifConstructorInstance ci,
            Block body, LabelChecker lc) throws SemanticException {
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();

        A = (JifContext) A.pushBlock();
        lc = lc.context(A);

        PathMap X = ts.pathMap();
        X = X.N(A.pc());

        // This set is all the uninitialized final variables.
        // These fields need to be initialized before calling the super
        // constructor, if the super constructor is an "untrusted class", i.e.
        // a Java class that isn't one of JifTypeSystem.trustedNonJifClassNames
        // or a Jif class.
        Set<JifFieldInstance> uninitFinalVars = Collections.emptySet();
//                uninitFinalFields(ci.container());

        // let the context know that we are label checking a constructor
        // body, and what the return label of the constructor is.
        A.setCheckingInits(true);

        Label Lr = ci.returnLabel();
        if (Lr == null) {
            Lr = ts.bottomLabel(ci.position());
        }

        // pc can be set to the provider during the init checking phase.
        A.setPc(ts.providerLabel((JifClassType) lc.context().currentClass()),
                lc);

        A.setConstructorReturnLabel(Lr);

        // stmts is the statements in the constructor body.
        List<Stmt> stmts = new LinkedList<Stmt>();

        // The flag preDangerousSuperCall indicates if we are before a call
        // to a "dangerous" super constructor call. A super call is dangerous
        // if the immediate superclass is a Jif class, or if this class has an
        // "untrusted" Java ancestor,
        //          i.e. ts.hasUntrustedAncestor(ci.container()) != null.
        boolean preDangerousSuperCall = true;

        boolean seenSuperCall = false;

        List<Stmt> statements = body.statements();
        for (Stmt s : statements) {
            if (seenSuperCall && uninitFinalVars.isEmpty() && A.checkingInits()
                    && (isEscapingThis(s) || hasStaticFieldAssign(s))) {
                // there won't be a "dangerousSuperCall",
                // and the next statement wants to let a reference to "this"
                // escape, so mark this as the end of the init checking.
                setEndOfInitChecking(lc, ci);
            }

            A = (JifContext) A.pushBlock();
            if (A.checkingInits()) {
                // when we are checking inits, the pc is lower than
                // the start-label of the method, so we explicitly add
                // as an assertion that the caller_pc is less than the pc.
                A.addAssertionLE(ts.callSitePCLabel(ci), A.pc());
            }

            s = (Stmt) lc.context(A).labelCheck(s);
            stmts.add(s);
            A = (JifContext) A.pop();

            PathMap Xs = getPathMap(s);

            if (preDangerousSuperCall) {
                // we're before a potentially dangerous super call, so we
                // can do check to see if we are assigning to a final label
                // field.
                // XXX: skip -- now handled in JifInitChecker.
                //checkFinalFieldAssignment(s, uninitFinalVars, A);

                if (s instanceof ConstructorCall) {
                    ConstructorCall ccs = (ConstructorCall) s;
                    boolean wasDangerousSuperCall = processConstructorCall(ccs,
                            lc, ci, uninitFinalVars);
                    if (wasDangerousSuperCall) {
                        preDangerousSuperCall = false;
                    }
                    if (ccs.kind() == ConstructorCall.SUPER) {
                        seenSuperCall = true;
                    }
                }
            }

            updateContextForNextStmt(lc, A, Xs);

            X = X.N(ts.notTaken()).join(Xs);
        }

        // Let the context know that we are no longer checking field
        // initializations in the constructor.
        setEndOfInitChecking(lc, ci);

        A = (JifContext) A.pop();
        return (Block) updatePathMap(body.statements(stmts), X);
    }

    /**
     * Utility method for updating the context for checking the next statement
     * in the block.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForNextStmt(LabelChecker lc, JifContext A,
        PathMap Xprev) {
        // At this point, the environment A should have been extended
        // to include any declarations of s.  Reset the PC label.
        A.setPc(Xprev.N(), lc);
    }

    /**
     * Determines whether the given statement contains an assignment to a static
     * field.
     */
    private boolean hasStaticFieldAssign(Stmt s) {
        final boolean[] result = new boolean[] { false };
        s.visit(new NodeVisitor() {
            @Override
            public Node override(Node n) {
                return result[0] ? n : null;
            }

            @Override
            public Node leave(Node old, Node n, NodeVisitor v) {
                if (result[0]) return n;

                // Check for static-field assignments.
                if (n instanceof FieldAssign) {
                    FieldInstance fi = ((FieldAssign) n).left().fieldInstance();
                    result[0] = fi.flags().isStatic();
                    return n;
                }

                // Check for pre/post-inc/dec of static fields.
                if (n instanceof Unary) {
                    Unary unary = (Unary) n;
                    if (!(unary.expr() instanceof Field)) return n;

                    Unary.Operator op = unary.operator();
                    if (op == Unary.POST_INC || op == Unary.POST_DEC
                            || op == Unary.PRE_INC || op == Unary.PRE_DEC) {
                        FieldInstance fi =
                                ((Field) unary.expr()).fieldInstance();
                        result[0] = fi.flags().isStatic();
                    }
                    return n;
                }

                return n;
            }
        });
        return result[0];
    }

    /**
     * Does the statement s possibly allow a reference to this to escape?
     */
    private boolean isEscapingThis(Stmt s) {
        final boolean[] result = new boolean[] { false };
        s.visit(new NodeVisitor() {
            @Override
            public Node leave(Node old, Node n, NodeVisitor v) {
                if (n instanceof Call) {
                    Call c = (Call) n;
                    if (c.target() instanceof Expr && JifUtil.effectiveExpr(
                            (Expr) c.target()) instanceof Special) {
                        result[0] = true;
                    }

                    List<Expr> args = c.arguments();
                    for (Expr arg : args) {
                        if (JifUtil.effectiveExpr(arg) instanceof Special) {
                            result[0] = true;
                        }
                    }
                } else if (n instanceof Assign && JifUtil.effectiveExpr(
                        ((Assign) n).right()) instanceof Special) {
                    result[0] = true;
                }
                return n;
            }
        });

        return result[0];
    }

    private boolean processConstructorCall(ConstructorCall ccs, LabelChecker lc,
            JifConstructorInstance ci, Set<JifFieldInstance> uninitFinalVars)
                    throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();
        boolean wasDangerousSuperCall = false;

        if (ccs.kind() == ConstructorCall.THIS) {
            // calling a this(...) constructor.
            // that means we've definitely finished checking inits.
            setEndOfInitChecking(lc, ci);
        } else if (ccs.kind() == ConstructorCall.SUPER) {
            // we are making a super constructor call. Is it
            // a potentially dangerous one?

            if (ts.isSignature(ci.container())) {
                // the class is not a Jif class, but just a signature
                // for a java class. Don't bother throwing any errors.
            } else if (!ts.isSignature(ci.container())
                    && ts.isSignature(ci.container().superType())
                    && ts.hasUntrustedAncestor(ci.container()) == null) {
                // Not a potentially dangerous super call.
                // The immediate super class is a trusted Java
                // class.
                // Although there are uninitialized final vars before
                // the call to super, it's OK, as this is a Jif class,
                // the immediate ancestor (and all ancestors) are
                // "trusted" java classes, which do not access
                // these fields before they are initialized.
            } else {
                // This is a potentially dangerous super call,
                // as code in one of the ancestor classes may
                // access a final field of this class.
                wasDangerousSuperCall = true;

                // Let the context know that we are no longer checking field
                // initializations in the constructor.
                setEndOfInitChecking(lc, ci);

                // We must make sure that all final variables of
                // this class are initialized before the super call.
                for (JifFieldInstance fi : uninitFinalVars) {
                    throw new SemanticDetailedException(
                            "Final field \"" + fi.name()
                                    + "\" must be initialized before "
                                    + "calling the superclass constructor.",
                            "All final fields of a class must "
                                    + "be initialized before the superclass "
                                    + "constructor is called, to prevent "
                                    + "ancestor classes from reading "
                                    + "uninitialized final fields. The "
                                    + "final field \"" + fi.name()
                                    + "\" needs to "
                                    + "be initialized before the superclass "
                                    + "constructor call.",
                            ccs.position());
                }
            }
        }
        return wasDangerousSuperCall;
    }

    protected void setEndOfInitChecking(LabelChecker lc,
            JifConstructorInstance ci) {
        JifContext A = lc.context();
        A.setCheckingInits(false);
        A.setConstructorReturnLabel(null);
        A.setPc(lc.upperBound(A.pc(), lc.typeSystem().callSitePCLabel(ci)), lc);

        // Taint current code PC bound with provider.
        Label providerAndPc =
                lc.typeSystem().join(A.currentCodePCBound(), A.provider());
        A.setCurrentCodePCBound(providerAndPc);
    }

    /**
     * Check if the stmt is an assignment to a final field. Moreover, if
     * the final field is a label, and it is being initialized from a final
     * label, share the uids of the fields.
     *
     * @throws SemanticException
     */
    protected void checkFinalFieldAssignment(Stmt s_,
            Set<JifFieldInstance> uninitFinalVars, JifContext A)
                    throws SemanticException {
        // Added this so that we can initialize final fields within atomic blocks in Fabric programs
        List<Stmt> initializers = new ArrayList<Stmt>();

        if (s_ instanceof Block) {
            Block b = (Block) s_;
            List<Stmt> stmts = b.statements();
            initializers.addAll(stmts);
        } else {
            initializers.add(s_);
        }

        for (Stmt s : initializers) {
            if (!(s instanceof Eval)
                    || !(((Eval) s).expr() instanceof FieldAssign)) {
                // we are not interested in this statement, it's not an
                // assignment
                // to a field
                continue;
            }

            FieldAssign ass = (FieldAssign) ((Eval) s).expr();
            Field f = ass.left();
            JifFieldInstance assFi = (JifFieldInstance) f.fieldInstance();

            if (!(ass.operator() == Assign.ASSIGN
                    && f.target() instanceof Special
                    && ((Special) f.target()).kind() == Special.THIS
                    && assFi.flags().isFinal())) {
                // assignment to something other than a final field of this.
                continue;
            }

            // Remove the field from the set of final vars, since it is
            // initialized here.
            uninitFinalVars.remove(assFi);

            // Note that the constraints specified in check-inits for the
            // "v = E"
            // case (Figure 4.44) are added when we visit the statement "s"
            // normally, so we don't need to handle them specially here.
        }
    }
}
