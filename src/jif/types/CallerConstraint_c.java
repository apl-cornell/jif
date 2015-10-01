package jif.types;

import java.util.Iterator;
import java.util.List;

import jif.types.principal.Principal;
import polyglot.types.TypeObject_c;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>CallerConstraint</code> interface.
 */
public class CallerConstraint_c extends TypeObject_c
        implements CallerConstraint {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<Principal> principals;

    public CallerConstraint_c(JifTypeSystem ts, Position pos,
            List<Principal> principals) {
        super(ts, pos);
        this.principals = ListUtil.copy(principals, true);
    }

    @Override
    public CallerConstraint principals(List<Principal> principals) {
        CallerConstraint_c n = (CallerConstraint_c) copy();
        n.principals = ListUtil.copy(principals, true);
        return n;
    }

    @Override
    public List<Principal> principals() {
        return principals;
    }

    @Override
    public String toString() {
        String s = "caller(";
        for (Iterator<Principal> i = principals.iterator(); i.hasNext();) {
            Principal p = i.next();
            s += p;
            if (i.hasNext()) {
                s += ", ";
            }
        }
        s += ")";
        return s;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }
}
