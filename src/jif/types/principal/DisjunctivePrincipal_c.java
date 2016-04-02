package jif.types.principal;

import java.util.Collection;
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

public class DisjunctivePrincipal_c extends Principal_c
        implements DisjunctivePrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<Principal> disjuncts;

    public DisjunctivePrincipal_c(Collection<Principal> disjuncts,
            JifTypeSystem ts, Position pos, PrincipalToJavaExpr toJava) {
        super(ts, pos, toJava);
        this.disjuncts = new LinkedHashSet<Principal>(disjuncts);
        if (disjuncts.size() < 2) {
            throw new InternalCompilerError(
                    "DisjunctivePrincipal should " + "have at least 2 members");
        }
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (Principal p : disjuncts) {
            if (!p.isRuntimeRepresentable()) return false;
        }
        return true;
    }

    @Override
    public boolean isCanonical() {
        for (Principal p : disjuncts) {
            if (!p.isCanonical()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String sep = ",";
        if (Report.should_report(Report.debug, 1)) {
            sb.append("<");
            sep = " or ";
        } else if (Report.should_report(Report.debug, 2)) {
            sb.append("<disjunction: ");
            sep = " or ";
        }
        for (Iterator<Principal> iter = disjuncts.iterator(); iter.hasNext();) {
            sb.append(iter.next());
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
        if (o instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal that = (DisjunctivePrincipal) o;
            return this.disjuncts.equals(that.disjuncts());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return disjuncts.hashCode();
    }

    @Override
    public Set<Principal> disjuncts() {
        return disjuncts;
    }

    @Override
    public Principal subst(LabelSubstitution substitution)
            throws SemanticException {
        Set<Principal> substDisjuncts = new HashSet<Principal>();
        for (Principal disjunct : disjuncts) {
            substDisjuncts.add(disjunct.subst(substitution));
        }
        if (substDisjuncts.size() > 1) {
            return new DisjunctivePrincipal_c(substDisjuncts,
                    (JifTypeSystem) ts, position(), toJava);
        } else if (substDisjuncts.size() == 1) {
            return substDisjuncts.iterator().next();
        } else {
            throw new InternalCompilerError(
                    "No principals left after substitution.");
        }
    }
}
