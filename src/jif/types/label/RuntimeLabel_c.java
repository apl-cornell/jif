package jif.types.label;

import java.util.Collection;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>RuntimeLabel</code> interface.
 */
public class RuntimeLabel_c extends Label_c implements RuntimeLabel {
    public RuntimeLabel_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
        setDescription("static representation of the " +
                        "join of all runtime representable labels");
    }

    public boolean isComparable() { return true; }
    public boolean isCovariant() { return false; }
    public boolean isEnumerable() { return false; }
    public boolean isCanonical() { return true; }
    public boolean isDisambiguated() { return true; }
    public boolean isSingleton() { return true; }

    public boolean isRuntimeRepresentable() { return true; }
    public Label singletonComponent() {
        return this;
    }


    public Collection components() {
        throw new InternalCompilerError(
        "Cannot list components of an infinite label.");
    }
    public String toString() {
        return "<all-runtime>";
    }
    public String componentString(Set printedLabels) {
        return "<all-runtime>";
    }
    public boolean equalsImpl(TypeObject o) {
        return o instanceof RuntimeLabel;
    }

    public int hashCode() { return 532708; }
    public boolean leq_(Label L, LabelEnv env) {
        // TS already tested against TOP and Runtime.  Anything else is false.
        return false;
    }
}
