package jif.types;

import jif.types.principal.ExternalPrincipal;
import polyglot.types.Type;
import polyglot.types.VarInstance_c;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PrincipalInstance</code> interface. 
 */
public class PrincipalInstance_c extends VarInstance_c
        implements PrincipalInstance {
    private static final long serialVersionUID = SerialVersionUID.generate();

    ExternalPrincipal principal;

    public PrincipalInstance_c(JifTypeSystem ts, Position pos,
            ExternalPrincipal p) {
        super(ts, pos, ts.Public().Static().Final(), ts.Principal(), p.name());
        this.principal = p;
    }

    @Override
    public ExternalPrincipal principal() {
        return principal;
    }

    @Override
    public PrincipalInstance principal(ExternalPrincipal principal) {
        PrincipalInstance_c n = (PrincipalInstance_c) copy();
        n.principal = principal;
        return n;
    }

    @Override
    public String toString() {
        return "principal " + name();
    }

    @Override
    public void setType(Type t) {
        //do nothing
    }
}
