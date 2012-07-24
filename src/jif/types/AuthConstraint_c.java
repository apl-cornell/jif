package jif.types;

import java.util.Iterator;
import java.util.List;

import jif.types.principal.Principal;
import polyglot.types.TypeObject_c;
import polyglot.util.ListUtil;
import polyglot.util.Position;

/** An implementation of the <code>AuthConstraint</code> interface.
 */
public class AuthConstraint_c extends TypeObject_c implements AuthConstraint {
    protected List<Principal> principals;

    public AuthConstraint_c(JifTypeSystem ts, Position pos,
            List<Principal> principals) {
        super(ts, pos);
        this.principals = ListUtil.copy(principals, true);
    }

    @Override
    public AuthConstraint principals(List<Principal> principals) {
        AuthConstraint_c n = (AuthConstraint_c) copy();
        n.principals = ListUtil.copy(principals, true);
        return n;
    }

    @Override
    public List<Principal> principals() {
        return principals;
    }

    @Override
    public String toString() {
        String s = "authority(";
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
