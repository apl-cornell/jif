package jif.types.label;

import java.util.Set;

import jif.types.JifConstructorInstance;
import jif.types.JifMethodInstance;
import jif.types.JifProcedureInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.CodeInstance;
import polyglot.types.ProcedureInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.types.VarInstance;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * This label is used as the label of the real argument.
 * The purpose is to avoid having to re-interpret labels at each call.
 */
public class ArgLabel_c extends Label_c implements ArgLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final VarInstance vi;
    private CodeInstance ci; // code instance containing vi, if relevant
    private String name;
    private Label upperBound;

    protected ArgLabel_c() {
        vi = null;
        ci = null;
        name = null;
    }

    public ArgLabel_c(JifTypeSystem ts, VarInstance vi, CodeInstance ci,
            Position pos) {
        super(ts, pos);
        this.vi = vi;
        this.ci = ci;
        this.name = vi.name();
        setDescription();
    }

    public ArgLabel_c(JifTypeSystem ts, ProcedureInstance pi, String name,
            Position pos) {
        super(ts, pos);
        this.vi = null;
        this.ci = pi;
        this.name = name;
    }

    private void setDescription() {
        if (vi != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("polymorphic label of formal argument ");
            sb.append(vi.name());
            if (ci instanceof JifProcedureInstance) {
                sb.append(" of ");
                if (ci instanceof JifMethodInstance) {
                    sb.append("method ");
                    sb.append(((JifMethodInstance) ci).name());

                } else if (ci instanceof JifConstructorInstance) {
                    sb.append("constructor");
                } else {
                    sb.append(((JifProcedureInstance) ci).debugString());
                }
            }
            sb.append(" (bounded above by ");
            sb.append(upperBound);
            sb.append(")");

            this.setDescription(sb.toString());
        }
    }

    @Override
    public VarInstance formalInstance() {
        return vi;
    }

    @Override
    public Label upperBound() {
        return upperBound;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setUpperBound(Label upperBound) {
        this.upperBound = upperBound;
        setDescription();
    }

    @Override
    public void setCodeInstance(CodeInstance ci) {
        this.ci = ci;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return false;
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
    public boolean isEnumerable() {
        return true;
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return upperBound != null;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof ArgLabel_c)) {
            return false;
        }
        ArgLabel_c that = (ArgLabel_c) o;

        // use pointer equality for vi instead of equals
        // to ensure that we don't confuse e.g., local instances
        // with the same name.
        return (this.ci == that.ci
                || (this.ci != null && this.ci.equals(that.ci)))
                && this.vi == that.vi && this.name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return (vi == null ? 234 : vi.hashCode()) ^ 2346882;
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        if (printedLabels.contains(this)) {
            if (Report.should_report(Report.debug, 2)) {
                return "<arg " + nicename() + ">";
            } else if (Report.should_report(Report.debug, 1)) {
                return "<arg " + name + ">";
            }
            return nicename();
        }
        printedLabels.add(this);

        if (Report.should_report(Report.debug, 2)) {
            String ub = upperBound == null ? "-"
                    : upperBound.toString(printedLabels);
            return "<arg " + name + " " + ub + ">";
        } else if (Report.should_report(Report.debug, 1)) {
            String ub = upperBound == null ? "-"
                    : upperBound.toString(printedLabels);
            return "<arg " + nicename() + " " + ub + ">";
        }
        return nicename();
    }

    private String nicename() {
        return vi == null ? name : vi.name();
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        // Should not recurse here, but allow the Label Env to do the recursion
        // on upperBound(), to avoid infinite loops.
        return false;
    }

    @Override
    public Label subst(LabelSubstitution substitution)
            throws SemanticException {
        ArgLabel lbl = this;
        if (substitution.recurseIntoChildren(lbl)) {
            if (!substitution.stackContains(this)) {
                if (lbl.upperBound() != null) {
                    substitution.pushLabel(this);
                    Label newBound = lbl.upperBound().subst(substitution);

                    if (newBound != lbl.upperBound()) {
                        lbl = (ArgLabel) lbl.copy();
                        lbl.setUpperBound(newBound);
                    }
                    substitution.popLabel(this);
                }
            } else {
                // the stack already contains this label, so don't call the
                // substitution recursively
            }
        }
        return substitution.substLabel(lbl);

    }

    @Override
    public String description() {
        setDescription();
        return super.description();
    }

}
