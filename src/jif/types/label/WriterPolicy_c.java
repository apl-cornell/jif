package jif.types.label;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class WriterPolicy_c extends Policy_c implements WriterPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Principal owner;
    private final Principal writer;

    public WriterPolicy_c(Principal owner, Principal writer, JifTypeSystem ts,
            Position pos) {
        super(ts, pos);
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
        this.writer = writer;
    }

    @Override
    public Principal owner() {
        return this.owner;
    }

    @Override
    public Principal writer() {
        return writer;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return owner.isCanonical() && writer.isCanonical();
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return owner.isRuntimeRepresentable()
                && writer.isRuntimeRepresentable();
    }

    @Override
    protected Policy simplifyImpl() {
        return this;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof WriterPolicy_c) {
            WriterPolicy_c that = (WriterPolicy_c) o;
            if (this.owner == that.owner
                    || (this.owner != null && this.owner.equals(that.owner))) {
                return this.writer.equals(that.writer);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (owner == null ? 0 : owner.hashCode())
                ^ (writer == null ? 0 : writer.hashCode()) ^ 1234352;
    }

    @Override
    public boolean leq_(IntegPolicy p, LabelEnv env, SearchState state) {
        if (this.isBottomIntegrity() || p.isTopIntegrity()) return true;

        if (p instanceof WriterPolicy) {
            WriterPolicy that = (WriterPolicy) p;
            // this = { o  <- .. wi .. }
            // that = { o' <- .. wj' .. }

            // o >= o'
            if (!env.actsFor(this.owner, that.owner())) {
                return false;
            }

            // for all i . wi >= o || exists j . wi >= wj'
            return env.actsFor(this.writer(), that.owner())
                    || env.actsFor(this.writer(), that.writer());
        }

        if (p instanceof IntegProjectionPolicy_c) {
            Label lowb =
                    env.findLowerBound(((IntegProjectionPolicy_c) p).label());
            return env.leq(this, lowb.integProjection());
        }

        return false;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        StringBuffer sb = new StringBuffer(owner.toString());
        sb.append("←");
        if (!writer.isTopPrincipal()) sb.append(writer.toString());
        return sb.toString();
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        throwTypes.addAll(owner.throwTypes(ts));
        throwTypes.addAll(writer.throwTypes(ts));
        return throwTypes;
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        boolean changed = false;

        Principal newOwner = owner.subst(substitution);
        if (newOwner != owner) changed = true;
        Principal newWriter = writer.subst(substitution);
        if (newWriter != writer) changed = true;

        if (!changed) return substitution.substPolicy(this).simplify();

        JifTypeSystem ts = (JifTypeSystem) typeSystem();
        WriterPolicy newPolicy =
                ts.writerPolicy(this.position(), newOwner, newWriter);
        return substitution.substPolicy(newPolicy).simplify();
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X = owner.labelCheck(A, lc);
        updateContextForWriter(lc, A, X);
        PathMap Xr = writer.labelCheck(A, lc);
        X = X.join(Xr);
        return X;
    }

    /**
     * Utility method for updating the context for checking the writer.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForWriter(LabelChecker lc, JifContext A,
            PathMap Xowner) {
        A.setPc(Xowner.N(), lc);
    }

//    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
//        return toJava.toJava((WriterPolicy)this, rw);
//    }

    @Override
    public boolean isBottomIntegrity() {
        return owner.isTopPrincipal() && writer.isTopPrincipal();
    }

    @Override
    public boolean isTopIntegrity() {
        return owner.isBottomPrincipal() && writer.isBottomPrincipal();
    }

    @Override
    public boolean isTop() {
        return isTopIntegrity();
    }

    @Override
    public boolean isBottom() {
        return isBottomIntegrity();
    }

    @Override
    public IntegPolicy meet(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public IntegPolicy join(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.join(this, p);
    }
}
