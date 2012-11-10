package jif.types.label;

import java.util.HashSet;
import java.util.Set;

import jif.types.JifTypeSystem;
import polyglot.types.TypeObject;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PolicyLabel</code> interface. 
 */
public abstract class Policy_c extends TypeObject_c implements Policy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public Policy_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }

    @Override
    public final String toString() {
        return toString(new HashSet<Label>());
    }

    @Override
    public abstract boolean equalsImpl(TypeObject t);

    @Override
    public boolean hasWritersToReaders() {
        return false;
    }

    @Override
    public boolean hasVariables() {
        return false;
    }

    @Override
    abstract public String toString(Set<Label> printedLabels);

    @Override
    public Policy_c copy() {
        Policy_c p = (Policy_c) super.copy();
        p.simplified = null;
        return p;
    }

    private Policy simplified = null;

    @Override
    public final Policy simplify() {
        // memoize the result
        if (simplified == null) {
            simplified = this.simplifyImpl();
        }
        return simplified;
    }

    protected abstract Policy simplifyImpl();

}
