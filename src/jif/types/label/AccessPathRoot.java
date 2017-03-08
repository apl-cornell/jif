package jif.types.label;

import jif.types.JifContext;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * Represents a final access path root.
 * 
 * @see jif.types.label.AccessPath
 */
public abstract class AccessPathRoot extends AccessPath {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected AccessPathRoot(Position pos) {
        super(pos);
    }

    @Override
    public boolean isUninterpreted() {
        return false;
    }

    @Override
    public final AccessPathRoot root() {
        return this;
    }

    @Override
    public PathMap labelcheck(JifContext A, LabelChecker lc) {
        throw new UnsupportedOperationException(
                "Cannot labelcheck an " + this.getClass());
    }

    @Override
    public boolean equivalentTo(AccessPath p, LabelEnv env) {
        return (this.equals(p));
    }

}
