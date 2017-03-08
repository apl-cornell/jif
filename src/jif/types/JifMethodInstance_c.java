package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.label.ProviderLabel;
import polyglot.main.Report;
import polyglot.types.Flags;
import polyglot.types.MethodInstance_c;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>JifMethodInstance</code> interface.
 */
public class JifMethodInstance_c extends MethodInstance_c
        implements JifMethodInstance {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Label pcBound;
    protected Label returnLabel;
    protected List<Assertion> constraints;
    protected boolean isDefaultPCBound;
    protected boolean isDefaultReturnLabel;

    public JifMethodInstance_c(JifTypeSystem ts, Position pos,
            ReferenceType container, Flags flags, Type returnType, String name,
            Label pcBound, boolean isDefaultPCBound,
            List<? extends Type> formalTypes, List<Label> formalArgLabels,
            Label returnLabel, boolean isDefaultReturnLabel,
            List<? extends Type> excTypes, List<Assertion> constraints) {

        super(ts, pos, container, flags, returnType, name, formalTypes,
                excTypes);
        this.constraints = new ArrayList<Assertion>(constraints);

        this.pcBound = pcBound;
        this.isDefaultPCBound = isDefaultPCBound;
        this.returnLabel = returnLabel;
        this.isDefaultReturnLabel = isDefaultReturnLabel;
        this.throwTypes = ListUtil.copy(throwTypes, true);
        this.formalTypes = ListUtil.copy(formalTypes, true);
    }

    @Override
    public Label pcBound() {
        return pcBound;
    }

    @Override
    public void setPCBound(Label pcBound, boolean isDefault) {
        this.pcBound = pcBound;
        this.isDefaultPCBound = isDefault;
    }

    @Override
    public boolean isDefaultPCBound() {
        return isDefaultPCBound;
    }

    @Override
    public Label returnLabel() {
        return returnLabel;
    }

    @Override
    public void setReturnLabel(Label returnLabel, boolean isDefault) {
        this.returnLabel = returnLabel;
        this.isDefaultReturnLabel = isDefault;
    }

    @Override
    public boolean isDefaultReturnLabel() {
        return isDefaultReturnLabel;
    }

    @Override
    public Label returnValueLabel() {
        JifTypeSystem jts = (JifTypeSystem) ts;
        if (returnType.isVoid()) return jts.notTaken();

        return jts.labelOfType(returnType);
    }

    @Override
    public List<Assertion> constraints() {
        return constraints;
    }

    @Override
    public void setConstraints(List<Assertion> constraints) {
        this.constraints = new ArrayList<Assertion>(constraints);
    }

    @Override
    public String toString() {
        String s = "method " + flags.translate() + returnType + " " + name;

        if (pcBound != null) {
            s += pcBound.toString();
        }

        s += "(";

        for (Iterator<Type> i = formalTypes.iterator(); i.hasNext();) {
            Type t = i.next();
            s += t.toString();

            if (i.hasNext()) {
                s += ", ";
            }
        }

        s += ")";

        if (returnLabel != null) {
            s += " : " + returnLabel.toString();
        }

        if (!this.throwTypes.isEmpty()) {
            s += " throws (";

            for (Iterator<Type> i = throwTypes.iterator(); i.hasNext();) {
                Type t = i.next();
                s += t.toString();

                if (i.hasNext()) {
                    s += ", ";
                }
            }

            s += ")";
        }

        if (!constraints.isEmpty()) {
            s += " where ";

            for (Iterator<Assertion> i = constraints.iterator(); i.hasNext();) {
                Assertion c = i.next();
                s += c.toString();

                if (i.hasNext()) {
                    s += ", ";
                }
            }
        }

        return s;
    }

    @Override
    public boolean isCanonical() {
        if (!(super.isCanonical() && pcBound.isCanonical()
                && returnLabel.isCanonical() && listIsCanonical(constraints)
                && formalTypes != null)) {
            return false;
        }

        JifTypeSystem jts = (JifTypeSystem) typeSystem();
        // also need to make sure that every formal type is labeled with an arg label
        for (Type t : formalTypes()) {
            if (!jts.isLabeled(t)
                    || !(jts.labelOfType(t) instanceof ArgLabel)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void subst(VarMap bounds) {
        this.pcBound = bounds.applyTo(pcBound);
        this.returnLabel = bounds.applyTo(returnLabel);
        this.returnType = bounds.applyTo(returnType);

        List<Type> formalTypes = new LinkedList<Type>();
        for (Type t : formalTypes()) {
            formalTypes.add(bounds.applyTo(t));
        }
        this.setFormalTypes(formalTypes);

        List<Type> throwTypes = new LinkedList<Type>();
        for (Type t : throwTypes()) {
            throwTypes.add(bounds.applyTo(t));
        }
        this.setThrowTypes(throwTypes);
    }

    @Override
    public void subst(LabelSubstitution subst) throws SemanticException {
        TypeSubstitutor tsbs = new TypeSubstitutor(subst);
        setPCBound(pcBound().subst(subst), isDefaultPCBound());
        setReturnLabel(returnLabel().subst(subst), isDefaultReturnLabel());
        setReturnType(tsbs.rewriteType(returnType()));

        List<Type> formalTypes = new LinkedList<Type>();
        for (Type t : formalTypes()) {
            formalTypes.add(tsbs.rewriteType(t));
        }
        this.setFormalTypes(formalTypes);

        List<Type> throwTypes = new LinkedList<Type>();
        for (Type t : throwTypes()) {
            throwTypes.add(tsbs.rewriteType(t));
        }
        this.setThrowTypes(throwTypes);

    }

    @Override
    public String debugString() {
        return debugString(true);
    }

    private String debugString(boolean showInstanceKind) {
        JifTypeSystem jts = (JifTypeSystem) ts;
        String s = "";
        if (showInstanceKind) {
            s = "method ";
        }
        s += flags.translate() + jts.unlabel(returnType) + " " + name + "(";

        for (Iterator<Type> i = formalTypes.iterator(); i.hasNext();) {
            Type t = i.next();
            s += jts.unlabel(t).toString();

            if (i.hasNext()) {
                s += ", ";
            }
        }

        s += ")";

        return s;
    }

    @Override
    public String signature() {
        if (Report.should_report(Report.debug, 1)) {
            return fullSignature();
        }
        return debugString(false);
    }

    public String fullSignature() {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        if (!isDefaultPCBound() || Report.should_report(Report.debug, 1)) {
            sb.append(pcBound);
        }
        sb.append("(");

        for (Iterator<Type> i = formalTypes.iterator(); i.hasNext();) {
            Type t = i.next();
            if (Report.should_report(Report.debug, 1)) {
                sb.append(t.toString());
            } else {
                if (t.isClass()) {
                    sb.append(t.toClass().name());
                } else {
                    sb.append(t.toString());
                }
            }

            if (i.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(")");
        if (!isDefaultReturnLabel() || Report.should_report(Report.debug, 1)) {
            sb.append(":");
            sb.append(returnLabel);
        }
        return sb.toString();

    }

    @Override
    public ProviderLabel provider() {
        JifClassType jct = (JifClassType) container;
        return jct.provider();
    }
}
