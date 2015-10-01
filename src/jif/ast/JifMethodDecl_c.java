package jif.ast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jif.types.Assertion;
import jif.types.DefaultSignature;
import jif.types.JifMethodInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.TypeSubstitutor;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathLocal;
import jif.types.label.AccessPathRoot;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import polyglot.ast.Block;
import polyglot.ast.Ext;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Javadoc;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>JifMethod</code> interface.
 */
// XXX Should be replaced with extension
@Deprecated
public class JifMethodDecl_c extends MethodDecl_c implements JifMethodDecl {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelNode startLabel;
    protected LabelNode returnLabel;
    protected List<ConstraintNode<Assertion>> constraints;

//    @Deprecated
    public JifMethodDecl_c(Position pos, Flags flags, TypeNode returnType,
            Id name, LabelNode startLabel, List<Formal> formals,
            LabelNode returnLabel, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc) {
        this(pos, flags, returnType, name, startLabel, formals, returnLabel,
                throwTypes, constraints, body, javadoc, null);
    }

    public JifMethodDecl_c(Position pos, Flags flags, TypeNode returnType,
            Id name, LabelNode startLabel, List<Formal> formals,
            LabelNode returnLabel, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc, Ext ext) {
        super(pos, flags, returnType, name, formals, throwTypes, body, javadoc,
                ext);
        this.startLabel = startLabel;
        this.returnLabel = returnLabel;
        this.constraints = ListUtil.copy(constraints, true);
    }

    @Override
    public LabelNode startLabel() {
        return this.startLabel;
    }

    @Override
    public JifMethodDecl startLabel(LabelNode startLabel) {
        return startLabel(this, startLabel);
    }

    protected <N extends JifMethodDecl_c> N startLabel(N n,
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
    public JifMethodDecl returnLabel(LabelNode returnLabel) {
        return returnLabel(this, returnLabel);
    }

    protected <N extends JifMethodDecl_c> N returnLabel(N n,
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
    public JifMethodDecl constraints(
            List<ConstraintNode<Assertion>> constraints) {
        return constraints(this, constraints);
    }

    protected <N extends JifMethodDecl_c> N constraints(N n,
            List<ConstraintNode<Assertion>> constraints) {
        if (CollectionUtil.equals(n.constraints, constraints)) return n;
        n = copyIfNeeded(n);
        n.constraints = ListUtil.copy(constraints, true);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Id name = visitChild(this.name, v);
        TypeNode returnType = visitChild(this.returnType, v);
        LabelNode startLabel = visitChild(this.startLabel, v);
        List<Formal> formals = visitList(this.formals, v);
        LabelNode returnLabel = visitChild(this.returnLabel, v);
        List<TypeNode> throwTypes = visitList(this.throwTypes, v);
        List<ConstraintNode<Assertion>> constraints =
                visitList(this.constraints, v);
        Block body = visitChild(this.body, v);
        return reconstruct(this, name, returnType, startLabel, formals,
                returnLabel, throwTypes, constraints, body);
    }

    protected <N extends JifMethodDecl_c> N reconstruct(N n, Id name,
            TypeNode returnType, LabelNode startLabel, List<Formal> formals,
            LabelNode returnLabel, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body) {
        n = super.reconstruct(n, returnType, name, formals, throwTypes, body);
        n = startLabel(n, startLabel);
        n = returnLabel(n, returnLabel);
        n = constraints(n, constraints);
        return n;
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifMethodDecl n = (JifMethodDecl) super.disambiguate(ar);

        JifMethodInstance jmi = (JifMethodInstance) n.methodInstance();
        JifTypeSystem jts = (JifTypeSystem) ar.typeSystem();

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
        jmi.setFormalTypes(formalTypes);

        // return type
        if (!n.returnType().isDisambiguated()) {
            // return type node not disambiguated yet
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        DefaultSignature ds = jts.defaultSignature();

        if (n.startLabel() != null && !n.startLabel().isDisambiguated()) {
            // the startlabel node hasn't been disambiguated yet
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        Type declrt = n.returnType().type();
        if (!declrt.isVoid() && !jts.isLabeled(declrt)) {
            // return type isn't labeled. Add the default label.
            declrt = jts.labeledType(declrt.position(), declrt,
                    ds.defaultReturnValueLabel(n));
            n = (JifMethodDecl) n.returnType(n.returnType().type(declrt));
        }
        jmi.setReturnType(declrt);

        if (n.returnLabel() != null && !n.returnLabel().isDisambiguated()) {
            // the return label node hasn't been disambiguated yet
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        Label Li; // pc bound for the method
        boolean isDefaultPCBound = false;
        if (n.startLabel() == null) {
            Li = ds.defaultPCBound(n.position(), n.name());
            isDefaultPCBound = true;
        } else {
            Li = n.startLabel().label();
        }
        jmi.setPCBound(Li, isDefaultPCBound);

        Label Lr; // return label
        boolean isDefaultReturnLabel = false;
        if (n.returnLabel() == null) {
            Lr = ds.defaultReturnLabel(n);
            isDefaultReturnLabel = true;
        } else {
            Lr = n.returnLabel().label();
        }
        jmi.setReturnLabel(Lr, isDefaultReturnLabel);

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
        jmi.setThrowTypes(newThrowTypes);

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
        jmi.setConstraints(constraints);

        renameArgs(jmi, new TypeSubstitutor(new ArgRenamer(false)));

        return n.methodInstance(jmi);
    }

    /**
     * Rename the arg labels and arg roots. This is needed to make sure
     * that during substitution of args in a recursive method call,
     * we don't confuse the
     *
     * FIXME: This javadoc trails off...
     */
    public static JifMethodInstance unrenameArgs(JifMethodInstance jmi) {
        jmi = (JifMethodInstance) jmi.copy();
        try {
            renameArgs(jmi, new TypeSubstitutor(new ArgRenamer(true)));
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected semantic", e);
        }
        return jmi;
    }

    /**
     * Rename the arg labels and arg roots. This is needed to make sure
     * that during substitution of args in a recursive method call,
     * we don't confuse the
     *
     * FIXME: This javadoc trails off...
     */
    private static void renameArgs(JifMethodInstance jmi, TypeSubstitutor tsub)
            throws SemanticException {
        // formal types
        List<Type> newFormalTypes =
                new ArrayList<Type>(jmi.formalTypes().size());
        List<? extends Type> formalTypes = jmi.formalTypes();
        for (Type t : formalTypes) {
            newFormalTypes.add(tsub.rewriteType(t));
        }
        jmi.setFormalTypes(newFormalTypes);

        // return type
        jmi.setReturnType(tsub.rewriteType(jmi.returnType()));

        // pc bound label
        jmi.setPCBound(tsub.rewriteLabel(jmi.pcBound()),
                jmi.isDefaultPCBound());

        // return label
        jmi.setReturnLabel(tsub.rewriteLabel(jmi.returnLabel()),
                jmi.isDefaultReturnLabel());

        // throw types
        List<Type> newThrowTypes = new ArrayList<Type>(jmi.throwTypes().size());
        List<? extends Type> throwTypes = jmi.throwTypes();
        for (Type t : throwTypes) {
            newThrowTypes.add(tsub.rewriteType(t));
        }
        jmi.setThrowTypes(newThrowTypes);

        // constraints
        List<Assertion> constraints =
                new ArrayList<Assertion>(jmi.constraints().size());
        for (Assertion c : jmi.constraints()) {
            constraints.add(tsub.rewriteAssertion(c));
        }
        jmi.setConstraints(constraints);
    }

    private static class ArgRenamer extends LabelSubstitution {
        boolean revertToOriginal;

        ArgRenamer(boolean revertToOriginal) {
            this.revertToOriginal = revertToOriginal;
        }

        @Override
        public Label substLabel(Label L) {
            if (L instanceof ArgLabel) {
                ArgLabel al = (ArgLabel) L;
                if (!revertToOriginal && !al.name().endsWith("'")) {
                    // change the name to end with a prime
                    al = (ArgLabel) al.copy();
                    al.setName(al.name() + "'");
                    return al;
                }
                if (revertToOriginal && al.name().endsWith("'")) {
                    // change the name to remove the prime
                    al = (ArgLabel) al.copy();
                    al.setName(al.name().substring(0, al.name().length() - 1));
                    return al;
                }
            }

            return L;
        }

        @Override
        public AccessPath substAccessPath(AccessPath ap) {
            AccessPathRoot r = ap.root();
            if (r instanceof AccessPathLocal) {
                AccessPathLocal apl = (AccessPathLocal) r;
                if (!revertToOriginal && !apl.name().endsWith("'")) {
                    apl = apl.name(apl.name() + "'");
                    AccessPath newPath = ap.subst(r, apl);
                    return newPath;
                }
                if (revertToOriginal && apl.name().endsWith("'")) {
                    apl = apl.name(
                            apl.name().substring(0, apl.name().length() - 1));
                    AccessPath newPath = ap.subst(r, apl);
                    return newPath;
                }
            }
            return ap;
        }
    }
}

/**
 *  This class substitutes all signature ArgLabels, DynamicArgLabels and
 * ArgPrincipals in a procedure declaration with appropriate non-signature
 * labels/principals.
 */
//class SignatureArgSubstitution extends ArgLabelSubstitution {
//public SignatureArgSubstitution(List nonSigArgLabels) {
//super(nonSigArgLabels, false);
//}
//public Label substLabel(Label L) {
//L = super.substLabel(L);

//if (L instanceof DynamicArgLabel) {
//DynamicArgLabel dal = (DynamicArgLabel)L;
//JifTypeSystem jts = (JifTypeSystem)dal.typeSystem();
//L = jts.dynamicArgLabel(dal.position(),
//dal.uid(),
//dal.name(),
//dal.label(),
//dal.index(),
//false);
//}
//return L;
//}

//public Principal substPrincipal(Principal p) {
//p = super.substPrincipal(p);
//if (p instanceof ArgPrincipal) {
//ArgPrincipal dap = (ArgPrincipal)p;
//JifTypeSystem jts = (JifTypeSystem)dap.typeSystem();
//p = jts.argPrincipal(dap.position(),
//dap.uid(),
//dap.name(),
//dap.label(),
//dap.index(),
//false);
//}
//return p;
//}
//}
