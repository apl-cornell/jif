package jif.types;

import java.util.*;

import jif.types.label.ArgLabel;
import jif.types.label.Label;
import polyglot.ext.jl.types.MethodInstance_c;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.util.TypedList;

/** An implementation of the <code>JifMethodInstance</code> interface.
 */
public class JifMethodInstance_c extends MethodInstance_c
                                implements JifMethodInstance
{
    protected Label startLabel;
    protected Label returnLabel;
    protected List constraints;
    protected boolean isDefaultStartLabel;
    protected boolean isDefaultReturnLabel;

    public JifMethodInstance_c(JifTypeSystem ts, Position pos,
	    ReferenceType container, Flags flags, Type returnType,
	    String name, Label startLabel, boolean isDefaultStartLabel,
            List formalTypes, List formalArgLabels,
	    Label returnLabel, boolean isDefaultReturnLabel,
            List excTypes, List constraints) {

	super(ts, pos, container, flags, returnType, name, formalTypes, excTypes);
	this.constraints = TypedList.copyAndCheck(constraints, Assertion.class, true);

	this.startLabel = startLabel;
        this.isDefaultStartLabel = isDefaultStartLabel;
	this.returnLabel = returnLabel;
        this.isDefaultReturnLabel = isDefaultReturnLabel;
	this.throwTypes = TypedList.copyAndCheck(throwTypes,
					       Type.class,
					       true);
	this.formalTypes = TypedList.copyAndCheck(formalTypes,
	                                          Type.class,
	                                          true);
    }

    public Label startLabel() {
	return startLabel;
    }

//    public Label externalPC() {
//	return startLabel;
//    }

    public void setStartLabel(Label startLabel, boolean isDefault) {
        this.startLabel = startLabel;
        this.isDefaultStartLabel = isDefault;
    }
    public boolean isDefaultStartLabel() {
        return isDefaultStartLabel;
    }

    public Label returnLabel() {
	return returnLabel;
    }

    public void setReturnLabel(Label returnLabel, boolean isDefault) {
        this.returnLabel = returnLabel;
	this.isDefaultReturnLabel = isDefault;
    }
    public boolean isDefaultReturnLabel() {
        return isDefaultReturnLabel;
    }

    public Label returnValueLabel() {
	JifTypeSystem jts = (JifTypeSystem) ts;
	if (returnType.isVoid())
	    return jts.notTaken();

	return jts.labelOfType(returnType);
    }

    public List constraints() {
	return constraints;
    }

    public void setConstraints(List constraints) {
	this.constraints = TypedList.copyAndCheck(constraints, Assertion.class, true);
    }

    public String toString() {
	String s = "method " + flags.translate() + returnType +
	    " " + name;

	if (startLabel != null) {
	    s += startLabel.toString();
	}

	s += "(";

	for (Iterator i = formalTypes.iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    s += t.toString();

	    if (i.hasNext()) {
		s += ", ";
	    }
	}

	s += ")";

	if (returnLabel != null) {
	    s += " : " + returnLabel.toString();
	}

	if (! this.throwTypes.isEmpty()) {
	    s += " throws (";

	    for (Iterator i = throwTypes.iterator(); i.hasNext(); ) {
		Type t = (Type) i.next();
		s += t.toString();

		if (i.hasNext()) {
		    s += ", ";
		}
	    }

	    s += ")";
	}

	if (! constraints.isEmpty()) {
	    s += " where ";

	    for (Iterator i = constraints.iterator(); i.hasNext(); ) {
		Assertion c = (Assertion) i.next();
		s += c.toString();

		if (i.hasNext()) {
		    s += ", ";
		}
	    }
	}

	return s;
    }

    public boolean isCanonical() {
        if (!(super.isCanonical()
                && startLabel.isCanonical()
                && returnLabel.isCanonical()
                && listIsCanonical(constraints)
                && formalTypes != null)) {
            return false;
        }

        JifTypeSystem jts = (JifTypeSystem)typeSystem();
        // also need to make sure that every formal type is labeled with an arg label
        for (Iterator i = formalTypes().iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            if (!jts.isLabeled(t) || !(jts.labelOfType(t) instanceof ArgLabel)) {
                return false;
            }
        }
        return true;
    }

    public void subst(VarMap bounds) {
	this.startLabel = bounds.applyTo(startLabel);
	this.returnLabel = bounds.applyTo(returnLabel);
	this.returnType = bounds.applyTo(returnType);

	List formalTypes = new LinkedList();
	for (Iterator i = formalTypes().iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    formalTypes.add(bounds.applyTo(t));
	}
	this.setFormalTypes(formalTypes);

        List throwTypes = new LinkedList();
        for (Iterator i = throwTypes().iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            throwTypes.add(bounds.applyTo(t));
        }
        this.setThrowTypes(throwTypes);
    }


    public String debugString() {
        return debugString(true);
    }
    private String debugString(boolean showInstanceKind) {
        JifTypeSystem jts = (JifTypeSystem) ts;
        String s = "";
        if (showInstanceKind) {
            s = "method ";
        }
	s += flags.translate() + jts.unlabel(returnType) +
	    " " + name + "(";

	for (Iterator i = formalTypes.iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    s += jts.unlabel(t).toString();

	    if (i.hasNext()) {
	        s += ", ";
	    }
	}

	s += ")";

	return s;
    }

    public String signature() {
        if (Report.should_report(Report.debug, 1)) { 
            return fullSignature();
        }
        return debugString();
    }
    public String fullSignature() { 
	StringBuffer sb = new StringBuffer();
        sb.append(name);
        if (!isDefaultStartLabel() || Report.should_report(Report.debug, 1)) {
            sb.append(startLabel);
        }
        sb.append("(");

        for (Iterator i = formalTypes.iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            if (Report.should_report(Report.debug, 1)) {
                sb.append(t.toString());
            }
            else {
                if (t.isClass()) {
                    sb.append(t.toClass().name());
                }
                else {
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
}
