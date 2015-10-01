package jif.types;

import jif.types.label.Label;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>CallerConstraint</code> interface.
 */
public class AutoEndorseConstraint_c extends TypeObject_c
        implements AutoEndorseConstraint {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Label endorseTo;

    public AutoEndorseConstraint_c(JifTypeSystem ts, Position pos,
            Label endorseTo) {
        super(ts, pos);
        this.endorseTo = endorseTo;
    }

    @Override
    public AutoEndorseConstraint endorseTo(Label endorseTo) {
        AutoEndorseConstraint_c n = (AutoEndorseConstraint_c) copy();
        n.endorseTo = endorseTo;
        return n;
    }

    @Override
    public Label endorseTo() {
        return endorseTo;
    }

    @Override
    public String toString() {
        String s = "autoendorse(";
        s += endorseTo.toString();
        s += ")";
        return s;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }
}
