package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.label.ProviderLabel;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;

/** An implementation of the <code>JifConstructorInstance</code> interface. 
 */
public class JifConstructorInstance_c extends ConstructorInstance_c
implements JifConstructorInstance
{
    protected Label pcBound;
    protected Label returnLabel;
    protected List<Assertion> constraints;
    protected boolean isDefaultPCBound;
    protected boolean isDefaultReturnLabel;

    public JifConstructorInstance_c(JifTypeSystem ts, Position pos,
            ClassType container, Flags flags,
            Label pcBound, boolean isDefaultPCBound, Label returnLabel, 
            boolean isDefaultReturnLabel, List<Type> formalTypes,
            List<Label> formalArgLabels, List<Type> excTypes,
            List<Assertion> constraints) {

        super(ts, pos, container, flags, formalTypes, excTypes);
        this.pcBound = pcBound;
        this.returnLabel = returnLabel;
        this.constraints = new ArrayList<Assertion>(constraints);

        this.pcBound = pcBound;
        this.isDefaultPCBound = isDefaultPCBound;
        this.returnLabel = returnLabel;
        this.isDefaultReturnLabel = isDefaultReturnLabel;
        this.throwTypes = TypedList.copyAndCheck(throwTypes, 
                                                 Type.class, 
                                                 true);
        this.formalTypes = TypedList.copyAndCheck(formalTypes, 
                                                  Type.class, 
                                                  true);
    }


    @Override
    public Label pcBound() {
        return pcBound;
    }

    public Label externalPC() {
        return pcBound;
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
    public void  setPCBound(Label pcBound, boolean isDefault) {
        this.pcBound = pcBound;
        this.isDefaultPCBound = isDefault;
    }

    @Override
    public boolean isDefaultPCBound() {
        return isDefaultPCBound;
    }

    @Override
    public List<Assertion> constraints() {
        return constraints;
    }

    @Override
    public void setConstraints(List<Assertion> constraints) {
        this.constraints = new ArrayList<Assertion>(constraints);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isCanonical() {
        if (!(super.isCanonical()
                && pcBound.isCanonical()
                && returnLabel.isCanonical()
                && listIsCanonical(constraints)
                && formalTypes != null)) {
            return false;
        }

        JifTypeSystem jts = (JifTypeSystem)typeSystem();
        // also need to make sure that every formal type is labeled with an arg label
        for (Iterator<Type> i = formalTypes().iterator(); i.hasNext(); ) {
            Type t = i.next();
            if (!jts.isLabeled(t) || !(jts.labelOfType(t) instanceof ArgLabel)) return false;
        }    
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void subst(VarMap bounds) {
        if (this.pcBound != null) 
            this.pcBound = bounds.applyTo(pcBound);

        if (this.returnLabel != null) 
            this.returnLabel = bounds.applyTo(returnLabel);

        List<Type> formalTypes = new LinkedList<Type>();
        for (Iterator<Type> i = formalTypes().iterator(); i.hasNext(); ) {
            Type t = i.next();
            formalTypes.add(bounds.applyTo(t));
        }
        this.setFormalTypes(formalTypes);

        List<Type> throwTypes = new LinkedList<Type>();
        for (Iterator<Type> i = throwTypes().iterator(); i.hasNext(); ) {
            Type t = i.next();
            throwTypes.add(bounds.applyTo(t));
        }

        this.setThrowTypes(throwTypes);
    }    

    @SuppressWarnings("unchecked")
    @Override
    public void subst(LabelSubstitution subst) throws SemanticException {
        TypeSubstitutor tsbs = new TypeSubstitutor(subst);
        setPCBound(pcBound().subst(subst), isDefaultPCBound());
        setReturnLabel(returnLabel().subst(subst), isDefaultReturnLabel());

        List<Type> formalTypes = new LinkedList<Type>();
        for (Iterator<Type> i = formalTypes().iterator(); i.hasNext(); ) {
            Type t = i.next();
            formalTypes.add(tsbs.rewriteType(t));
        }
        this.setFormalTypes(formalTypes);

        List<Type> throwTypes = new LinkedList<Type>();
        for (Iterator<Type> i = throwTypes().iterator(); i.hasNext(); ) {
            Type t = i.next();
            throwTypes.add(tsbs.rewriteType(t));
        }
        this.setThrowTypes(throwTypes);

    }
    @Override
    public String debugString() {
        return debugString(true);
    }
    
    @SuppressWarnings("unchecked")
    private String debugString(boolean showInstanceKind) {
        String s = "";
        if (showInstanceKind) {
            s = "constructor ";
        }
        s += flags.translate() + container + "(";

        for (Iterator<Type> i = formalTypes.iterator(); i.hasNext(); ) {
            Type t = i.next();
            s += ((JifTypeSystem)ts).unlabel(t).toString();

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
    
    @SuppressWarnings("unchecked")
    public String fullSignature() {
        String s = container.toString();
        if (!isDefaultPCBound() || Report.should_report(Report.debug, 1)) {
            s += pcBound;
        }
        s += "(";

        for (Iterator<Type> i = formalTypes.iterator(); i.hasNext(); ) {
            Type t = i.next();
            s += t.toString();

            if (i.hasNext()) {
                s += ",";
            }
        }
        s += ")";
        if (!isDefaultReturnLabel() || Report.should_report(Report.debug, 1)) {
            s += ":" + returnLabel;
        }

        return s;
    }

    @Override
    public ProviderLabel provider() {
        if (container instanceof JifClassType) {
            JifClassType jct = (JifClassType) container;
            return jct.provider();
        }
        throw new InternalCompilerError(
                "Expected JifClassType for container, but got "
                        + container.getClass());
    }

}
