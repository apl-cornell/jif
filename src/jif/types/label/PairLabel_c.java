package jif.types.label;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jif.translate.LabelToJavaExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class PairLabel_c extends Label_c implements PairLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final ConfPolicy confPolicy;
    private final IntegPolicy integPolicy;

    public PairLabel_c(JifTypeSystem ts, ConfPolicy confPolicy,
            IntegPolicy integPolicy, Position pos, LabelToJavaExpr trans) {
        super(ts, pos, trans);
        this.confPolicy = confPolicy;
        this.integPolicy = integPolicy;
    }

    @Override
    public ConfPolicy confPolicy() {
        return this.confPolicy;
    }

    @Override
    public IntegPolicy integPolicy() {
        return this.integPolicy;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return confPolicy.isRuntimeRepresentable()
                && integPolicy.isRuntimeRepresentable();
    }

    @Override
    public boolean isCovariant() {
        return false;
    }

    @Override
    public boolean isComparable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return confPolicy.isCanonical() && integPolicy.isCanonical();
    }

    @Override
    public boolean isEnumerable() {
        return true;
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return isCanonical();
    }

    @Override
    public boolean isBottom() {
        return confPolicy.isBottom() && integPolicy.isBottom();
    }

    @Override
    public boolean isTop() {
        return confPolicy.isTop() && integPolicy.isTop();
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof PairLabel_c)) {
            return false;
        }
        PairLabel_c that = (PairLabel_c) o;
        return (this.confPolicy.equals(that.confPolicy))
                && (this.integPolicy.equals(that.integPolicy));
    }

    @Override
    protected Label simplifyImpl() {
        ConfPolicy cp = (ConfPolicy) confPolicy.simplify();
        IntegPolicy ip = (IntegPolicy) integPolicy.simplify();
        if (cp != confPolicy || ip != integPolicy) {
            return ((JifTypeSystem) ts).pairLabel(position, cp, ip);
        }
        return this;
    }

    @Override
    public ConfPolicy confProjection() {
        return confPolicy();
    }

    @Override
    public IntegPolicy integProjection() {
        return integPolicy();
    }

    @Override
    public int hashCode() {
        return confPolicy.hashCode() ^ integPolicy.hashCode();
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return toString(printedLabels, true);
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        return toString(printedLabels, false);
    }

    public String toString(Set<Label> printedLabels, boolean topLevel) {
        StringBuffer sb = new StringBuffer();
        if (topLevel) sb.append("{");
        if (Report.should_report(Report.debug, 2)) {
            sb.append("<pair " + confPolicy.toString(printedLabels) + " ; "
                    + integPolicy.toString(printedLabels) + ">");
        } else if (Report.should_report(Report.debug, 1)) {
            sb.append(confPolicy.toString(printedLabels) + "; "
                    + integPolicy.toString(printedLabels));
        } else {
            String cs = "";
            if (!topLevel || !confPolicy.isBottomConfidentiality()) {
                cs = confPolicy.toString(printedLabels);
            }
            String is = "";
            if (!topLevel || !integPolicy.isTopIntegrity()) {
                is = integPolicy.toString(printedLabels);
            }
            if (cs.length() > 0 && is.length() > 0) {
                sb.append(cs + "; " + is);
            } else {
                sb.append(cs + is);
            }
        }
        if (topLevel) sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        if (L instanceof PairLabel) {
            PairLabel that = (PairLabel) L;
//            System.out.println("***Comparing " + this + " to " + that);
//            System.out.println("   to wit " + this.confPolicy() + " to " + that.confPolicy());
//            System.out.println("   and " + this.integPolicy() + " to " + that.integPolicy());
//            System.out.println("   " + env.leq(this.confPolicy(), that.confPolicy()));
//            System.out.println("   " + env.leq(this.integPolicy(), that.integPolicy()));
            return env.leq(this.confPolicy(), that.confPolicy(), state)
                    && env.leq(this.integPolicy(), that.integPolicy(), state);
        }
        return false;
    }

    @Override
    public Label subst(LabelSubstitution substitution)
            throws SemanticException {
        PairLabel lbl = this;
        if (substitution.recurseIntoChildren(lbl)) {
            ConfPolicy newCP =
                    (ConfPolicy) lbl.confPolicy().subst(substitution);
            IntegPolicy newIP =
                    (IntegPolicy) lbl.integPolicy().subst(substitution);

            if (newCP != this.confPolicy || newIP != this.integPolicy) {
                lbl = ((JifTypeSystem) ts).pairLabel(position, newCP, newIP);
            }
        }
        return substitution.substLabel(lbl);
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        throwTypes.addAll(confPolicy.throwTypes(ts));
        throwTypes.addAll(integPolicy.throwTypes(ts));
        return throwTypes;
    }

    @Override
    public boolean hasWritersToReaders() {
        return confPolicy.hasWritersToReaders()
                || integPolicy.hasWritersToReaders();
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());

        A = (JifContext) A.pushBlock();

        PathMap Xc = confPolicy.labelCheck(A, lc);
        X = X.join(Xc);

        updateContextForSecond(lc, A, X);
        PathMap Xi = integPolicy.labelCheck(A, lc);
        X = X.join(Xi);
        return X;
    }

    /**
     * Utility method for updating the context for checking the second part of
     * the pair.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForSecond(LabelChecker lc, JifContext A,
            PathMap Xfirst) {
        A.setPc(Xfirst.N(), lc);
    }
}
