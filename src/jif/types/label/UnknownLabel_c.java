package jif.types.label;

import java.util.Collections;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>UnknownLabel</code> interface.
 */
public class UnknownLabel_c extends Label_c implements UnknownLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public UnknownLabel_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }

    @Override
    public boolean isComparable() {
        return false;
    }

    @Override
    public boolean isEnumerable() {
        return false;
    }

    @Override
    public boolean isCanonical() {
        return false;
    }

    @Override
    public boolean isDisambiguatedImpl() {
        return false;
    }

    @Override
    public boolean isCovariant() {
        return false;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return false;
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        return "<unknown label>";
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)
                && this.description() != null) {
            return "<unknown label: " + this.description() + ">";
        }
        return "<unknown label>";
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return 234334;
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        throw new InternalCompilerError("Cannot compare unknown label.");
    }

    @Override
    public Set<Variable> variables() {
        return Collections.emptySet();
    }

}
