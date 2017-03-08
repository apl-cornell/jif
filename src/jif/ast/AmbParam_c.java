package jif.ast;

import jif.types.JifTypeSystem;
import jif.types.JifVarInstance;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.PrincipalInstance;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Ext;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.VarInstance;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>AmbParam</code> interface.
 */
public class AmbParam_c extends Node_c implements AmbParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Id name;
    protected ParamInstance pi;

//    @Deprecated
    public AmbParam_c(Position pos, Id name, ParamInstance pi) {
        this(pos, name, pi, null);
    }

    public AmbParam_c(Position pos, Id name, ParamInstance pi, Ext ext) {
        super(pos, ext);
        this.name = name;
        this.pi = pi;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    public String name() {
        return this.name.id();
    }

    public AmbParam name(String name) {
        return name(this, name);
    }

    protected <N extends AmbParam_c> N name(N n, String name) {
        if (n.name.id().equals(name)) return n;
        return id(n, n.name.id(name));
    }

    public AmbParam id(Id id) {
        return id(this, id);
    }

    protected <N extends AmbParam_c> N id(N n, Id id) {
        if (n.name == id) return n;
        n = copyIfNeeded(n);
        n.name = id;
        return n;
    }

    @Override
    public Param parameter() {
        throw new InternalCompilerError("No parameter yet");
    }

    @Override
    public String toString() {
        return name + "{amb}";
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        if (this.name == null) return this;

        Id name = visitChild(this.name, v);
        return reconstruct(this, name);
    }

    protected <N extends AmbParam_c> N reconstruct(N n, Id name) {
        n = id(n, name);
        return n;
    }

    /**
     * Count the number of times disambiguate has been called, to allow reporting
     * of meaningful error messages.
     */
    private int disambCount = 0;

    /**
     * The maximum number of times that disambiguate can be called before
     * we allow an error message to be reported. Should be less than
     * polyglot.frontend.Scheduler.MAX_RUN_COUNT, but big enough so that
     * the largest cycle of recursive formals can be resolved. e.g.,
     * m(int{a1} a0, int{a2} a1, ...., int{a0} ak),  MAX_DISAMB_CALLS would
     * need to be bigger than k.
     */
    protected static final int MAX_DISAMB_CALLS = 100;

    /** Disambiguates <code>name</code>.
     *  If <code>name</code> is a <tt>VarInstance</tt>, we get a dynamic label/principal
     *  node. If <code>name</code> is a <tt>PrincipalInstance</tt>, we get the same
     *  principal. If <code>name</code> is a <tt>ParamInstance</tt>, we get a <tt>ParamLabel</tt>
     *  or a <tt>ParamPrincipal</tt>.
     */
    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        Context c = sc.context();
        VarInstance vi = c.findVariable(name.id());

        if (!vi.isCanonical() && pi == null
                && disambCount++ < MAX_DISAMB_CALLS) {
            // not yet ready to disambiguate
            sc.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        if (vi instanceof JifVarInstance) {
            return varToParam((JifVarInstance) vi, sc);
        }

        if (vi instanceof PrincipalInstance) {
            return principalToParam((PrincipalInstance) vi, sc);
        }

        if (vi instanceof ParamInstance) {
            return paramToParam((ParamInstance) vi, sc);
        }

        throw new SemanticDetailedException(
                vi + " cannot be used as parameter.",
                "The variable " + name
                        + " is not suitable for use as a parameter.",
                this.position());
    }

    /** Turns a <code>JifVarInstance</code> object into a label node or
     *  a principal node */
    protected Node varToParam(JifVarInstance vi, AmbiguityRemover sc)
            throws SemanticException {

        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        if (vi.flags().isFinal()) {
            if (ts.isLabel(vi.type()) || (pi != null && pi.isLabel())) {
                Label l = ts.dynamicLabel(position(),
                        ts.varInstanceToAccessPath(vi, this.position()));
                return nf.CanonicalLabelNode(position(), l);
            }

            if (ts.isImplicitCastValid(vi.type(), ts.Principal())
                    || (pi != null && pi.isPrincipal())) {
                Principal p = ts.dynamicPrincipal(position(),
                        ts.varInstanceToAccessPath(vi, this.position()));
                return nf.CanonicalPrincipalNode(position(), p);
            }
            throw new SemanticDetailedException(
                    "Only final variables of type \"label\" or \"principal\" may be used as class parameters.",
                    "Only final variables of type \"label\" or \"principal\" may be used as class parameters. "
                            + "The variable " + vi.name()
                            + " is not of type \"label\", nor of type \"principal\".",
                    position());
        }

        throw new SemanticDetailedException(
                "Only final variables of type \"label\" or \"principal\" may be used as class parameters.",
                "Only final variables of type \"label\" or \"principal\" may be used as class parameters. "
                        + "The variable " + vi.name() + " is not final.",
                position());
    }

    /** Turns a <code>PrincipalInstance</code> object into a principal node. */
    protected Node principalToParam(PrincipalInstance vi, AmbiguityRemover sc) {
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        return nf.CanonicalPrincipalNode(position(), vi.principal());
    }

    /** Turns a <code>PramaInstance</code> object into a label node or a
     *  principal node.
     */
    protected Node paramToParam(ParamInstance pi, AmbiguityRemover sc) {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        if (pi.isCovariantLabel()) {
            // <covariant label uid> => <covariant-label uid>
            Label L = ts.covariantLabel(position(), pi);
            return nf.CanonicalLabelNode(position(), L);
        }

        if (pi.isInvariantLabel()) {
            // <param label uid> => <label-param uid>
            Label L = ts.paramLabel(position(), pi);
            L.setDescription("label parameter " + pi.name() + " of class "
                    + pi.container().fullName());
            return nf.CanonicalLabelNode(position(), L);
        }

        if (pi.isPrincipal()) {
            // <param principal uid> => <principal-param uid>
            Principal p = ts.principalParam(position(), pi);
            return nf.CanonicalPrincipalNode(position(), p);
        }

        throw new InternalCompilerError("Unrecognized parameter type for " + pi,
                position());
    }
}
