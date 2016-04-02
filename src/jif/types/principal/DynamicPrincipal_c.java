package jif.types.principal;

import java.util.List;

import jif.translate.PrincipalToJavaExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathConstant;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>DynamicPrincipal</code> interface.
 */
public class DynamicPrincipal_c extends Principal_c
        implements DynamicPrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final AccessPath path;

    public DynamicPrincipal_c(AccessPath path, JifTypeSystem ts, Position pos,
            PrincipalToJavaExpr toJava) {
        super(ts, pos, toJava);
        this.path = path;
        if (path instanceof AccessPathConstant) {
            throw new InternalCompilerError(
                    "Don't expect to get AccessPathConstants for dynamic labels");
        }
    }

    @Override
    public AccessPath path() {
        return path;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return path.isCanonical();
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return "<pr-dynamic " + path + ">";
        }
        return path().toString();
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof DynamicPrincipal)) {
            return false;
        }

        DynamicPrincipal that = (DynamicPrincipal) o;
        return (this.path.equals(that.path()));
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return path.throwTypes(ts);
    }

    @Override
    public Principal subst(LabelSubstitution substitution)
            throws SemanticException {
        AccessPath newPath = substitution.substAccessPath(path);
        if (newPath != path) {
            JifTypeSystem ts = typeSystem();
            Principal newDP = ts.pathToPrincipal(this.position(), newPath);
            return substitution.substPrincipal(newDP);
        }
        return substitution.substPrincipal(this);
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return path.labelcheck(A, lc);
    }

}
