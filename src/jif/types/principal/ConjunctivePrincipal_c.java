package jif.types.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import jif.translate.PrincipalToJavaExpr;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class ConjunctivePrincipal_c extends Principal_c
        implements ConjunctivePrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<Principal> conjuncts;

    public ConjunctivePrincipal_c(Collection<Principal> conjuncts,
            JifTypeSystem ts, Position pos, PrincipalToJavaExpr toJava) {
        super(ts, pos, toJava);
        this.conjuncts = new LinkedHashSet<Principal>(conjuncts);
        if (conjuncts.size() < 2) {
            throw new InternalCompilerError(
                    "ConjunctivePrincipal should " + "have at least 2 members");
        }
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (Principal p : conjuncts) {
            if (!p.isRuntimeRepresentable()) return false;
        }
        return true;
    }

    @Override
    public boolean isCanonical() {
        for (Principal p : conjuncts) {
            if (!p.isCanonical()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String sep = "&";
        if (Report.should_report(Report.debug, 1)) {
            sb.append("<");
            sep = " and ";
        } else if (Report.should_report(Report.debug, 2)) {
            sb.append("<conjunction: ");
            sep = " and ";
        }
        for (Iterator<Principal> iter = conjuncts.iterator(); iter.hasNext();) {
            Principal p = iter.next();
            if (p instanceof DisjunctivePrincipal) {
                sb.append('(');
                sb.append(p);
                sb.append(')');
            } else {
                sb.append(p);
            }
            if (iter.hasNext()) sb.append(sep);
        }

        if (Report.should_report(Report.debug, 1)) {
            sb.append(">");
        } else if (Report.should_report(Report.debug, 2)) {
            sb.append(">");
        }
        return sb.toString();
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal that = (ConjunctivePrincipal) o;
            return this.conjuncts.equals(that.conjuncts());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return conjuncts.hashCode();
    }

    @Override
    public Set<Principal> conjuncts() {
        return Collections.unmodifiableSet(conjuncts);
    }

    @Override
    public Principal simplify() {
        if (!this.isCanonical()) {
            return this;
        }

        Set<Principal> needed = new LinkedHashSet<Principal>();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Principal p : conjuncts) {
            Principal ci = p.simplify();

            if (ci.hasVariables()) {
                needed.add(ci);
            } else {
                boolean subsumed = false;

                for (Iterator<Principal> j = needed.iterator(); j.hasNext();) {
                    Principal cj = j.next();

                    if (cj.hasVariables()) {
                        continue;
                    }

                    if (jts.actsFor(cj, ci)) {
                        subsumed = true;
                        break;
                    }

                    if (jts.actsFor(ci, cj)) {
                        j.remove();
                    }
                }

                if (!subsumed) needed.add(ci);
            }
        }

        if (needed.equals(conjuncts)) {
            return this;
        }
        if (needed.size() == 1) {
            return needed.iterator().next();
        }

        return new ConjunctivePrincipal_c(needed, (JifTypeSystem) ts,
                position(), toJava);
    }

    @Override
    public Principal subst(LabelSubstitution substitution)
            throws SemanticException {
        Set<Principal> substConjuncts = new HashSet<Principal>();
        for (Principal conjunct : conjuncts) {
            substConjuncts.add(conjunct.subst(substitution));
        }

        if (substConjuncts.size() > 1) {
            return new ConjunctivePrincipal_c(substConjuncts,
                    (JifTypeSystem) ts, position(), toJava);
        } else if (substConjuncts.size() == 1) {
            return substConjuncts.iterator().next();
        } else {
            throw new InternalCompilerError(
                    "No principals left after substitution.");
        }

    }

}
