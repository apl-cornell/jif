package jif.types.label;

import java.util.Collection;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/**
 * An implementation of the <code>NotTaken</code> interface. 
 */
public class NotTaken_c extends Label_c implements NotTaken {
    protected NotTaken_c() {
    }

    public NotTaken_c(JifTypeSystem ts, Position pos) {
	super(ts, pos);
        this.description = "pseudo-label representing " + 
                           "a path that cannot be taken";
    }

    public Collection components() {
	throw new InternalCompilerError(
	    "Cannot list components of the not-taken pseudo-label.");
    }
    public boolean isCovariant() { return false; }
    public boolean isComparable() { return false; }
    public boolean isEnumerable() { return false; }
    public boolean isCanonical() { return true; }
    public boolean isRuntimeRepresentable() { return false; }

    public String toString() { return "0"; }
    public String componentString() { return "0"; }

    public boolean equalsImpl(TypeObject o) {
	return o instanceof NotTaken;
    }

    public int hashCode() { return 39870; }

    public boolean leq_(Label L, LabelEnv env) {
        throw new InternalCompilerError("Cannot compare \"" + this + "\".");
    }
    public void translate(Resolver c, CodeWriter w) {
        throw new InternalCompilerError("Cannot translate label \"" + this + "\".");
    }
    public Label subst(LocalInstance arg, Label l) {
        return this;
    }
    public Label subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
}
