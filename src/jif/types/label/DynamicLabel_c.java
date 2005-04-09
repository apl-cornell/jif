package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;

/** An implementation of the <code>DynamicLabel</code> interface. 
 */
public class DynamicLabel_c extends Label_c implements DynamicLabel {
    private final AccessPath path;

    public DynamicLabel_c(AccessPath path, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.path = path;
        if (path instanceof AccessPathConstant) {
            throw new InternalCompilerError("Don't expect to get AccessPathConstants for dynamic labels");
        }
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
    public boolean isDisambiguated() { return isCanonical(); }
    public boolean isEnumerable() {
        return true;
    }
    public boolean equalsImpl(TypeObject o) {
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

    public boolean leq_(Label L, LabelEnv env) {
        // can only be equal if the dynamic label is equal to this,
        // or through use of the label env, both taken care of outside
        // this method.
        return false;
    }

    public void translate(Resolver c, CodeWriter w) {
        w.write(path.translate(c));
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

    public Label subst(LocalInstance arg, Label l) {
        return this;
    }
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substLabel(this);
    }
}
