package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import polyglot.ast.Id;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifDynamicLabel_c extends Label_c implements RifDynamicLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id name;
    private Label label;

    public RifDynamicLabel_c(Id name, Label label, JifTypeSystem ts,
            Position pos) {
        super(ts, pos);
        this.name = name;
        this.label = label;
    }

    @Override
    public Id getName() {
        return this.name;
    }

    @Override
    public Label getLabel() {
        return this.label;
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

    //is this correct???
    @Override
    public boolean leq_(Label L, LabelEnv H, SearchState state) {
        if (L instanceof RifDynamicLabel) {
            RifDynamicLabel that = (RifDynamicLabel) L;
            return this.name.toString() == that.getName().toString()
                    && this.label.leq_(that.getLabel(), H, state);

        }
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

        return this.name + "(" + this.label.componentString() + ")";
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof RifDynamicLabel)) {
            return false;
        }
        RifDynamicLabel that = (RifDynamicLabel) o;
        boolean b1 = this.name.id().equals(getName().id());
        boolean b2 = (this.label.equalsImpl(that.getLabel()));

        return b1 && b2;
    }

}
