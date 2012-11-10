package jif.types.label;

import java.util.Set;

import jif.JifOptions;
import jif.translate.LabelToJavaExpr;
import jif.types.JifClassType;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class ProviderLabel_c extends Label_c implements ProviderLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    /**
     * The class that this is labelling.
     */
    protected JifClassType classType;

    protected boolean isTrusted;

    public ProviderLabel_c(JifClassType classType, LabelToJavaExpr toJava) {
        super(classType.typeSystem(), classType.position(), toJava);
        this.classType = classType;
        this.isTrusted = ((JifOptions) Options.global).trustedProviders;
    }

    @Override
    public ProviderLabel position(Position pos) {
        ProviderLabel_c copy = (ProviderLabel_c) copy();
        copy.position = pos;
        return copy;
    }

    @Override
    public JifClassType classType() {
        return classType;
    }

    @Override
    public ConfPolicy confProjection() {
        if (!isTrusted) return super.confProjection();
        return typeSystem().bottomConfPolicy(position);
    }

    @Override
    public IntegPolicy integProjection() {
        if (!isTrusted) return super.integProjection();
        return typeSystem().bottomIntegPolicy(position);
    }

    @Override
    public boolean isBottom() {
        return isTrusted;
    }

    @Override
    public boolean isTrusted() {
        return isTrusted;
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
    public boolean isEnumerable() {
        return true;
    }

    @Override
    public boolean leq_(Label L, LabelEnv H, SearchState state) {
        // If this provider is not trusted, then this <= L leq only if equal
        // this == L, which is checked before this method is called.
        return isTrusted;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return isCanonical();
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return componentString(printedLabels);
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        if (Report.should_report(Report.debug, 1)) {
            return "<provider " + classType.fullName() + ">";
        }

        return classType.fullName() + ".provider";
    }

    @Override
    public boolean equalsImpl(TypeObject t) {
        if (this == t) return true;
        if (!(t instanceof ProviderLabel_c)) return false;

        ProviderLabel_c that = (ProviderLabel_c) t;
        return classType.equals(that.classType);
    }

}
