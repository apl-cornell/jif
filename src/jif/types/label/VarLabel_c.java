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

/** An implementation of the <code>VarLabel</code> interface.
 */
public class VarLabel_c extends Label_c implements VarLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final transient int uid = ++counter;
    private static int counter = 0;
    private String name;
    /**
     * Does whatever this variable resolves to need to be runtime representable?
     */
    private boolean mustRuntimeRepresentable = false;

    protected VarLabel_c() {
    }

    public VarLabel_c(String name, String description, JifTypeSystem ts,
            Position pos) {
        super(ts, pos);
        this.name = name;
        setDescription(description);
    }

    @Override
    public boolean isEnumerable() {
        return true;
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
    public boolean isDisambiguatedImpl() {
        return true;
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
    public void setMustRuntimeRepresentable() {
        this.mustRuntimeRepresentable = true;
    }

    @Override
    public boolean mustRuntimeRepresentable() {
        return this.mustRuntimeRepresentable;
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        if (Report.should_report(Report.debug, 2)) {
            return "<var " + name + " " + uid + " "
                    + System.identityHashCode(this) + ">";
        }
        if (Report.should_report(Report.debug, 1)) {
            return "<var " + name + ">";
        }
        return name;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return -56393 + uid;
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        throw new InternalCompilerError("Cannot compare " + this + ".");
    }

    @Override
    public Set<Variable> variableComponents() {
        return Collections.<Variable> singleton(this);
    }

    @Override
    public Set<Variable> variables() {
        return Collections.<Variable> singleton(this);
    }

    @Override
    public String name() {
        return name;
    }
}
