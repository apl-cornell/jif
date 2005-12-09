package jif.types.label;

import java.util.List;
import java.util.Set;

import jif.ast.JifUtil;
import jif.translate.DynamicLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>DynamicLabel</code> interface. 
 */
public class DynamicLabel_c extends Label_c implements DynamicLabel {
    private final AccessPath path;

    public DynamicLabel_c(AccessPath path, JifTypeSystem ts, Position pos) {
        super(ts, pos, new DynamicLabelToJavaExpr_c()); 
        this.path = path;
        if (path instanceof AccessPathConstant) {
            throw new InternalCompilerError("Don't expect to get AccessPathConstants for dynamic labels");
        }
        setDescription(JifUtil.accessPathDescrip(path, "label"));
    }
    public AccessPath path() {
        return path;
    }
    public boolean isRuntimeRepresentable() {
        return true;
    }
    public boolean isCovariant() {
        return false;
    }
    public boolean isComparable() {
        return true;
    }
    public boolean isCanonical() { return true; }
    protected boolean isDisambiguatedImpl() { return isCanonical(); }
    public boolean isEnumerable() {
        return true;
    }
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (! (o instanceof DynamicLabel)) {
            return false;
        }           
        DynamicLabel that = (DynamicLabel) o;
        return (this.path.equals(that.path()));
    }
    public int hashCode() {
        return path.hashCode();
    }
    
    public String componentString(Set printedLabels) {
        if (Report.should_report(Report.debug, 1)) { 
            return "<dynamic " + path + ">";
        }
        return "*"+path();
    }

    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        // can only be equal if the dynamic label is equal to this,
        // or through use of the label env, both taken care of outside
        // this method.
        return false;
    }

    public List throwTypes(TypeSystem ts) {
        return path.throwTypes(ts);
    }

    public Label subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) {
            return this;
        }
        
        if (newPath instanceof AccessPathConstant) {
            AccessPathConstant apc = (AccessPathConstant)newPath;
            if (!apc.isLabelConstant()) {
                throw new InternalCompilerError("Replaced a dynamic label with a non-label!");
            }
            return (Label)apc.constantValue();
        }
        return ((JifTypeSystem)typeSystem()).dynamicLabel(this.position(), newPath);
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substLabel(this);
    }
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return path.labelcheck(A, lc);
    }
}
