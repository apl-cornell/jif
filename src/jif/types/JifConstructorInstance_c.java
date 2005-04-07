package jif.types;

import java.util.*;

import jif.types.label.ArgLabel;
import jif.types.label.Label;
import polyglot.ext.jl.types.ConstructorInstance_c;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.util.TypedList;

/** An implementation of the <code>JifConstructorInstance</code> interface. 
 */
public class JifConstructorInstance_c extends ConstructorInstance_c
				     implements JifConstructorInstance
{
    protected Label startLabel;
    protected Label returnLabel;
    protected List constraints;
    protected List formalArgLabels;
    protected boolean isDefaultStartLabel;
    protected boolean isDefaultReturnLabel;

    public JifConstructorInstance_c(JifTypeSystem ts, Position pos,
	    ClassType container, Flags flags,
	    Label startLabel, boolean isDefaultStartLabel, Label returnLabel, 
            boolean isDefaultReturnLabel, List formalTypes, List formalArgLabels,
	    List excTypes, List constraints) {

	super(ts, pos, container, flags, formalTypes, excTypes);
	this.startLabel = startLabel;
	this.returnLabel = returnLabel;
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
	this.formalArgLabels = TypedList.copyAndCheck(formalArgLabels, 
                                               Label.class, 
                                               true);
    }


    public Label startLabel() {
	return startLabel;
    }

    public Label externalPC() {
	return startLabel;
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

    public void  setStartLabel(Label startLabel, boolean isDefault) {
	this.startLabel = startLabel;
	this.isDefaultStartLabel = isDefault;
    }

    public boolean isDefaultStartLabel() {
        return isDefaultStartLabel;
    }
    public List formalArgLabels() {
        return formalArgLabels;
    }

    public void setFormalArgLabels(List formalArgLabels) {
        this.formalArgLabels = TypedList.copyAndCheck(formalArgLabels, ArgLabel.class, true);
    }

    public List constraints() {
	return constraints;
    }

    public void setConstraints(List constraints) {
	this.constraints = TypedList.copyAndCheck(constraints, Assertion.class, true);
    }

    public boolean isCanonical() {
        if (!(super.isCanonical()
                && startLabel.isCanonical()
                && returnLabel.isCanonical()
                && listIsCanonical(constraints)
                && formalArgLabels != null
                && formalTypes != null
                && formalArgLabels.size() == formalTypes.size())) {
            return false;
        }
    
    JifTypeSystem jts = (JifTypeSystem)typeSystem();
    // also need to make sure that every formal type is labeled
    for (Iterator i = formalTypes().iterator(); i.hasNext(); ) {
        Type t = (Type) i.next();
        if (!jts.isLabeled(t)) return false;
    }    
    return true;
    }
    
    public void subst(VarMap bounds) {
	if (this.startLabel != null) 
	    this.startLabel = bounds.applyTo(startLabel);
	
	if (this.returnLabel != null) 
	    this.returnLabel = bounds.applyTo(returnLabel);
	
	List formalTypes = new LinkedList();
	for (Iterator i = formalTypes().iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    formalTypes.add(bounds.applyTo(t));
	}
	this.formalTypes(formalTypes);
	
        List throwTypes = new LinkedList();
        for (Iterator i = throwTypes().iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            throwTypes.add(bounds.applyTo(t));
        }
        
        this.throwTypes(throwTypes);
    }    
 
    public String debugString() {
	String s = "constructor " + flags.translate() + container + "(";

	for (Iterator i = formalTypes.iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    s += ((JifTypeSystem)ts).unlabel(t).toString();

	    if (i.hasNext()) {
	        s += ", ";
	    }
	}

	s += ")";

	return s;
    }
    
    public String signature() {
	String s = container + " " + startLabel +" (";

        for (Iterator i = formalTypes.iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            s += t.toString();

            if (i.hasNext()) {
                s += ",";
            }
        }

        s += ") : " + returnLabel;

        return s;
    }

}
