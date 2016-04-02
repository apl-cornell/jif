package jif.types.label;

import java.util.List;
import java.util.Set;

import jif.translate.LabelToJavaExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>DynamicLabel</code> interface.
 */
public class DynamicLabel_c extends Label_c implements DynamicLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final AccessPath path;

    public DynamicLabel_c(AccessPath path, JifTypeSystem ts, Position pos,
            LabelToJavaExpr trans) {
        super(ts, pos, trans);
        this.path = path;
        if (path instanceof AccessPathConstant) {
            throw new InternalCompilerError(
                    "Don't expect to get AccessPathConstants for dynamic labels");
        }
        setDescription(ts.accessPathDescrip(path, "label"));
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
    public boolean isCovariant() {
        return false;
    }

    @Override
    public boolean isComparable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return isCanonical();
    }

    @Override
    public boolean isEnumerable() {
        return true;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof DynamicLabel)) {
            return false;
        }
        DynamicLabel that = (DynamicLabel) o;
        return (this.path.equals(that.path()));
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        if (Report.should_report(Report.debug, 1)) {
            return "<dynamic " + path + ">";
        }
        return "*" + path();
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        // can be leq than L if L is also a dynamic label with an access path
        // equivalent to this one.
        if (L instanceof DynamicLabel) {
            DynamicLabel that = (DynamicLabel) L;
//            System.out.println("Checking if " + this + " <= " + L + " : " + env.equivalentAccessPaths(this.path, that.path()));
            if (env.equivalentAccessPaths(this.path, that.path())) {
                return true;
            }
        }
        // can only be equal if the dynamic label is equal to this,
        // or through use of the label env, both taken care of outside
        // this method.
        return false;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return path.throwTypes(ts);
    }

    @Override
    public Label subst(LabelSubstitution substitution)
            throws SemanticException {
        AccessPath newPath = substitution.substAccessPath(path);
        if (newPath != path) {
            JifTypeSystem ts = typeSystem();
            Label newDL = ts.pathToLabel(this.position(), newPath);
            return substitution.substLabel(newDL);
        }
        return substitution.substLabel(this);
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return path.labelcheck(A, lc);
    }
}
