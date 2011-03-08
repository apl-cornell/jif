package jif.types.label;

import java.util.Set;

import jif.types.JifClassType;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;

public class ProviderLabel_c extends Label_c implements ProviderLabel {
    
    /**
     * The class that this is labelling.
     */
    protected JifClassType classType;
    
    public ProviderLabel_c(Position pos, JifClassType classType) {
        this.classType = classType;
    }

    @Override
    public JifClassType classType() {
        return classType;
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
        // Only leq if equal to this parameter, which is checked before this
        // method is called.
        return false;
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
