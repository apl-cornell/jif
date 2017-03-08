package jif.ast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jif.types.Assertion;
import jif.types.DefaultSignature;
import jif.types.JifConstructorInstance;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Block;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Ext;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Javadoc;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CollectionUtil;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

/**
 * An implementation of the <code>JifConstructor</code> interface.
 */
@Deprecated
// XXX should be replaced with extension
public class JifConstructorDecl_c extends ConstructorDecl_c
        implements JifConstructorDecl {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelNode startLabel;
    protected LabelNode returnLabel;
    protected List<ConstraintNode<Assertion>> constraints;

//    @Deprecated
    public JifConstructorDecl_c(Position pos, Flags flags, Id name,
            LabelNode startLabel, LabelNode returnLabel, List<Formal> formals,
            List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc) {
        this(pos, flags, name, startLabel, returnLabel, formals, throwTypes,
                constraints, body, javadoc, null);
    }

    public JifConstructorDecl_c(Position pos, Flags flags, Id name,
            LabelNode startLabel, LabelNode returnLabel, List<Formal> formals,
            List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc, Ext ext) {
        super(pos, flags, name, formals, throwTypes, body, javadoc, ext);
        this.startLabel = startLabel;
        this.returnLabel = returnLabel;
        this.constraints = ListUtil.copy(constraints, true);
    }

    @Override
    public LabelNode startLabel() {
        return this.startLabel;
    }

    @Override
    public JifConstructorDecl startLabel(LabelNode startLabel) {
        return startLabel(this, startLabel);
    }

    protected <N extends JifConstructorDecl_c> N startLabel(N n,
            LabelNode startLabel) {
        if (n.startLabel == startLabel) return n;
        n = copyIfNeeded(n);
        n.startLabel = startLabel;
        return n;
    }

    @Override
    public LabelNode returnLabel() {
        return this.returnLabel;
    }

    @Override
    public JifConstructorDecl returnLabel(LabelNode returnLabel) {
        return returnLabel(this, returnLabel);
    }

    protected <N extends JifConstructorDecl_c> N returnLabel(N n,
            LabelNode returnLabel) {
        if (n.returnLabel == returnLabel) return n;
        n = copyIfNeeded(n);
        n.returnLabel = returnLabel;
        return n;
    }

    @Override
    public List<ConstraintNode<Assertion>> constraints() {
        return this.constraints;
    }

    @Override
    public JifConstructorDecl constraints(
            List<ConstraintNode<Assertion>> constraints) {
        return constraints(this, constraints);
    }

    protected <N extends JifConstructorDecl_c> N constraints(N n,
            List<ConstraintNode<Assertion>> constraints) {
        if (CollectionUtil.equals(n.constraints, constraints)) return n;
        n = copyIfNeeded(n);
        n.constraints = ListUtil.copy(constraints, true);
        return n;
    }

    protected <N extends JifConstructorDecl_c> N reconstruct(N n, Id name,
            LabelNode startLabel, LabelNode returnLabel, List<Formal> formals,
            List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body) {
        n = super.reconstruct(n, name, formals, throwTypes, body);
        n = startLabel(n, startLabel);
        n = returnLabel(n, returnLabel);
        n = constraints(n, constraints);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Id name = visitChild(this.name, v);
        LabelNode startLabel = visitChild(this.startLabel, v);
        LabelNode returnLabel = visitChild(this.returnLabel, v);
        List<Formal> formals = visitList(this.formals, v);
        List<TypeNode> throwTypes = visitList(this.throwTypes, v);
        List<ConstraintNode<Assertion>> constraints =
                visitList(this.constraints, v);
        Block body = visitChild(this.body, v);
        return reconstruct(this, name, startLabel, returnLabel, formals,
                throwTypes, constraints, body);
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifConstructorDecl n = (JifConstructorDecl_c) super.disambiguate(ar);

        JifConstructorInstance jci =
                (JifConstructorInstance) n.constructorInstance();
        JifTypeSystem jts = (JifTypeSystem) ar.typeSystem();

        if (n.startLabel() != null && !n.startLabel().isDisambiguated()) {
            // the startlabel node hasn't been disambiguated yet
            return n;
        }

        if (n.returnLabel() != null && !n.returnLabel().isDisambiguated()) {
            // the return label node hasn't been disambiguated yet
            return n;
        }

        // set the formal types
        List<Type> formalTypes = new ArrayList<Type>(n.formals().size());
        List<Formal> formals = n.formals();
        for (Formal f : formals) {
            if (!f.isDisambiguated()) {
                // formals are not disambiguated yet.
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            formalTypes.add(f.declType());
        }
        jci.setFormalTypes(formalTypes);

        Label Li; // start label
        boolean isDefaultPCBound = false;
        DefaultSignature ds = jts.defaultSignature();
        if (n.startLabel() == null) {
            Li = ds.defaultPCBound(n.position(), n.name());
            isDefaultPCBound = true;
        } else {
            Li = n.startLabel().label();

            // Automagically ensure that the begin label is at least as high as
            // the provider label. This ensures that code will be unable to
            // affect data that the provider is not trusted to affect. It also
            // ensures the behaviour of confidential code will not be leaked.
            Li = jts.join(Li, jci.provider());
        }
        jci.setPCBound(Li, isDefaultPCBound);

        Label Lr; // return label
        boolean isDefaultReturnLabel = false;
        if (n.returnLabel() == null) {
            Lr = ds.defaultReturnLabel(n);
            isDefaultReturnLabel = true;
        } else {
            Lr = n.returnLabel().label();
        }
        jci.setReturnLabel(Lr, isDefaultReturnLabel);

        // set the labels for the throwTypes.
        List<Type> newThrowTypes = new LinkedList<Type>();
        List<TypeNode> throwTypes = n.throwTypes();
        for (TypeNode tn : throwTypes) {
            if (!tn.isDisambiguated()) {
                // throw types haven't been disambiguated yet.
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }

            Type xt = tn.type();
            if (!jts.isLabeled(xt)) {
                // default exception label is the return label
                xt = jts.labeledType(xt.position(), xt, Lr);
            }
            newThrowTypes.add(xt);
        }
        jci.setThrowTypes(newThrowTypes);

        List<Assertion> constraints =
                new ArrayList<Assertion>(n.constraints().size());
        for (ConstraintNode<Assertion> cn : n.constraints()) {
            if (!cn.isDisambiguated()) {
                // constraint nodes haven't been disambiguated yet.
                ar.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }
            constraints.addAll(cn.constraints());
        }
        jci.setConstraints(constraints);

        return n.constructorInstance(jci);
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {

        Node n = super.typeCheck(tc);
        JifConstructorDecl_c jcd = (JifConstructorDecl_c) n;
        jcd.checkConstructorCall(tc);

        return jcd;
    }

    /**
     * Checks that if there is an explicit constructor call in the constructor
     * body that the call is all right.
     *
     * In particular, if this is a java class or one of the ancestors of this
     * class is "untrusted" then the explicit constructor call must be
     * the first statement in the constructor body.
     *
     * Moreover, if this is a Jif class, but the superclass is not a Jif class,
     * then first statement must be a default constructor call.
     * @throws SemanticException
     */
    private void checkConstructorCall(TypeChecker tc) throws SemanticException {

        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();

        ClassType ct = tc.context().currentClass();

        // ignore java.lang.Object
        if (ts.equals(ct, ts.Object())) return;

        ClassType untrusted = ts.hasUntrustedAncestor(ct);
        if (ts.isSignature(ct)) {
            // If ct is not a jif class, then the first statement of the body
            // had better be a constructor call (which is the normal Java
            // rule).
            checkFirstStmtConstructorCall(
                    "The first statement of a constructor "
                            + "of a Java class must be a constructor call.",
                    true, false);
        } else if (!ts.isSignature(ct) && untrusted != null) {
            // If ct is a Jif class, but the super class is an
            // untrusted Java class, then the first statement of the body
            // must be an explicit call to the default super constructor:
            // "super()". If it wasn't, then due to the translation of
            // Jif constructors, a malicious (non-Jif) superclass access
            // final fields before they have been initialized.
            checkFirstStmtConstructorCall(
                    "The first statement of a constructor "
                            + "of a Jif class with an untrusted Java superclass "
                            + "must be an explicit call to the default super constructor,"
                            + "\"super()\".",
                    false, true);
        } else if (!ts.isSignature(ct) && ts.isSignature(ct.superType())) {
            // this is a Jif class, but it's superclass is a trusted Java class.
            // The first statement must either be a "this(...)" constructor
            // call, or a "super()" call. That is, the constructor cannot
            // call any super constructor other than the default constuctor,
            // since in translation, the Jif class has no opportunity to
            // marshal the arguments before the super constructor call
            // happens.
            checkFirstStmtConstructorCall("The first statement of a "
                    + "constructor of a Jif class with a Java superclass "
                    + "must be either a \"this(...)\" constructor call, or "
                    + "a call to the default super constructor, "
                    + "\"super()\".", true, true);
        }
    }

    /**
     *
     * @param message
     * @param allowThisCalls if false then first statement must be super(); if true then it may be a call to this(...) or super().
     * @throws SemanticException
     */
    private void checkFirstStmtConstructorCall(String message,
            boolean allowThisCalls, boolean superCallMustBeDefault)
                    throws SemanticException {
        if (body == null) {
            // this must be a native constructor.
            return;
        }
        if (body().statements().size() < 1) {
            throw new SemanticException("Empty constructor body.", position());
        }
        Stmt s = body().statements().get(0);
        if (!(s instanceof ConstructorCall)) {
            throw new SemanticException(message, position());
        }

        ConstructorCall cc = (ConstructorCall) s;
        if (!allowThisCalls && cc.kind() == ConstructorCall.THIS) {
            throw new SemanticException(message, position());
        }

        if (superCallMustBeDefault && cc.kind() == ConstructorCall.SUPER
                && cc.arguments().size() > 0) {
            throw new SemanticException(message, position());
        }

    }
}
