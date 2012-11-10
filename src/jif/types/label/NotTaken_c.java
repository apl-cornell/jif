package jif.types.label;

import java.util.Collections;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * An implementation of the <code>NotTaken</code> interface.
 */
public class NotTaken_c extends Label_c implements NotTaken {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected NotTaken_c() {
    }

    public NotTaken_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.description =
                "pseudo-label representing " + "a path that cannot be taken";
    }

    @Override
    public boolean isCovariant() {
        return false;
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
        return true;
    }

    @Override
    public boolean isDisambiguatedImpl() {
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return false;
    }

    @Override
    public String toString() {
        return "0";
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        return "0";
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        return o instanceof NotTaken;
    }

    @Override
    public int hashCode() {
        return 39870;
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        throw new InternalCompilerError("Cannot compare \"" + this + "\".");
    }

    @Override
    public Set<Variable> variables() {
        return Collections.emptySet();
    }

}
