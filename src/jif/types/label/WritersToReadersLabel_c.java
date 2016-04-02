package jif.types.label;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.translate.CannotLabelToJavaExpr_c;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>DynamicLabel</code> interface.
 */
public class WritersToReadersLabel_c extends Label_c
        implements WritersToReadersLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    final Label label;

    public WritersToReadersLabel_c(Label label, JifTypeSystem ts,
            Position pos) {
        super(ts, pos, new CannotLabelToJavaExpr_c());
        this.label = label;
        setDescription("finds an upper bound of " + label
                + " and converts the writers of the bound into readers");
    }

    @Override
    public Label label() {
        return label;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return false;
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
        return true;
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return label.isCanonical();
    }

    @Override
    public boolean isEnumerable() {
        return true;
    }

    @Override
    public IntegPolicy integProjection() {
        return ((JifTypeSystem) ts).bottomIntegPolicy(position());
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof WritersToReadersLabel)) {
            return false;
        }
        WritersToReadersLabel that = (WritersToReadersLabel) o;
        return (this.label.equals(that.label()));
    }

    @Override
    public int hashCode() {
        return label.hashCode() ^ 597829;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return componentString(printedLabels);
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        if (Report.should_report(Report.debug, 1)) {
            return "<writersToReaders " + label + ">";
        }
        return "writersToReaders(" + label() + ")";
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        return false;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return Collections.emptyList();
    }

    @Override
    public Label subst(LabelSubstitution substitution)
            throws SemanticException {
        WritersToReadersLabel lbl = this;
        if (substitution.recurseIntoChildren(lbl)) {
            Label newLabel = lbl.label().subst(substitution);

            if (newLabel != lbl.label()) {
                JifTypeSystem ts = typeSystem();
                lbl = ts.writersToReadersLabel(lbl.position(), newLabel);
            }
        }
        return substitution.substLabel(lbl);
    }

    @Override
    public boolean hasWritersToReaders() {
        return true;
    }

    @Override
    public Label transform(LabelEnv env) {
        Label bound = env.findUpperBound(label());
        return transformImpl(bound);
    }

    protected static Label transformImpl(Label label) {
        JifTypeSystem ts = label.typeSystem();
        if (label instanceof VarLabel_c || label instanceof ProviderLabel) {
            // cant do anything.
            return ts.writersToReadersLabel(label.position(), label);
        } else if (label instanceof PairLabel) {
            PairLabel pl = (PairLabel) label;
            ConfPolicy newCP = transformIntegToConf(pl.integPolicy());
            return ts.pairLabel(pl.position(), newCP,
                    ts.bottomIntegPolicy(pl.position()));
        } else if (label instanceof JoinLabel) {
            JoinLabel L = (JoinLabel) label;

            Set<Label> comps = new LinkedHashSet<Label>();
            for (Label c : L.joinComponents()) {
                comps.add(transformImpl(c));
            }
            return ts.meetLabel(label.position(), comps);
        } else if (label instanceof MeetLabel) {
            MeetLabel L = (MeetLabel) label;

            Set<Label> comps = new LinkedHashSet<Label>();
            for (Label c : L.meetComponents()) {
                comps.add(transformImpl(c));
            }
            return ts.joinLabel(label.position(), comps);
        }

        throw new InternalCompilerError(
                "WritersToReaders undefined " + "for " + label);
    }

    protected static ConfPolicy transformIntegToConf(IntegPolicy pol) {
        JifTypeSystem ts = (JifTypeSystem) pol.typeSystem();
        if (pol instanceof WriterPolicy) {
            WriterPolicy wp = (WriterPolicy) pol;
            return ts.readerPolicy(wp.position(), wp.owner(), wp.writer());
        }
        if (pol instanceof JoinIntegPolicy_c) {
            @SuppressWarnings("unchecked")
            JoinPolicy_c<IntegPolicy> jp = (JoinPolicy_c<IntegPolicy>) pol;
            Set<ConfPolicy> newPols =
                    new HashSet<ConfPolicy>(jp.joinComponents().size());
            for (IntegPolicy ip : jp.joinComponents()) {
                ConfPolicy cp = transformIntegToConf(ip);
                newPols.add(cp);
            }
            return ts.meetConfPolicy(jp.position(), newPols);
        }
        if (pol instanceof MeetIntegPolicy_c) {
            @SuppressWarnings("unchecked")
            MeetPolicy_c<IntegPolicy> mp = (MeetPolicy_c<IntegPolicy>) pol;
            Set<ConfPolicy> newPols =
                    new HashSet<ConfPolicy>(mp.meetComponents().size());
            for (IntegPolicy ip : mp.meetComponents()) {
                ConfPolicy cp = transformIntegToConf(ip);
                newPols.add(cp);
            }
            return ts.joinConfPolicy(mp.position(), newPols);
        }
        //XXX: can we do anything about projections?
        throw new InternalCompilerError("Unexpected integ policy: " + pol);
    }

}
