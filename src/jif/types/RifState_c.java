package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Id;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public class RifState_c implements RifState {

    private Id name;
    private List<Principal> principals;
    private boolean current;

    public RifState_c(Id name, List<Principal> principals, boolean current) {
        this.name = name;
        this.principals = principals;
        this.current = current;
    }

    @Override
    public Id name() {
        return this.name;
    }

    @Override
    public List<Principal> principals() {
        return this.principals;
    }

    @Override
    public boolean isCurrent() {
        return this.current;
    }

    @Override
    public boolean isCanonical() {
        for (Principal p : this.principals) {
            if (!p.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (Principal p : this.principals) {
            if (!p.isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        StringBuffer sb = new StringBuffer(name.toString());
        sb.append(":{");
        Iterator<Principal> ip = this.principals.iterator();
        while (ip.hasNext()) {
            Principal p = ip.next();
            if (!p.isTopPrincipal()) sb.append(p.toString());
            if (ip.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        for (Principal p : this.principals) {
            throwTypes.addAll(p.throwTypes(ts));
        }
        return throwTypes;
    }

    @Override
    public RifComponent subst(LabelSubstitution substitution)
            throws SemanticException {
        List<Principal> l = new LinkedList<Principal>();

        for (Principal p : this.principals) {
            Principal newprincipal = p.subst(substitution);
            l.add(newprincipal);
        }
        RifState newstate = new RifState_c(this.name, l, this.current);
        return newstate;
    }

    @Override
    public boolean isBottomConfidentiality() {
        for (Principal p : this.principals) {
            if (!p.isBottomPrincipal()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTopConfidentiality() {
        for (Principal p : this.principals) {
            if (!p.isTopPrincipal()) {
                return false;
            }
        }
        return true;
    }
}
