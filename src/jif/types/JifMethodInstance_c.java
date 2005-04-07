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
    protected List formalArgLabels;
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
	this.formalArgLabels = TypedList.copyAndCheck(formalArgLabels, 
	                                          Label.class, 
	                                          true);
    
    }
//    private Label replaceArgLabels(Label l) {
//        if (l == null) return l;
//        try {
//            return l.subst(new ArgLabelSubstitution(argLabels, false));
//        }
//        catch (SemanticException e) {
//            throw new InternalCompilerError("Unexpected SemanticException " +
//            "during label substitution: " + e.getMessage(), l.position());
//        }
//    }
//
//    private Type replaceArgLabelsInType(Type t) {
//	if (((JifTypeSystem)typeSystem()).isLabeled(t)) {
//	    LabeledType lt = (LabeledType)t;
//	    lt = lt.labelPart(replaceArgLabels(lt.labelPart()));
//	    lt = lt.typePart(replaceArgLabelsInType(lt.typePart()));
//
//	    return lt;
//	}
//	else if (t instanceof ArrayType) {
//	    ArrayType at = (ArrayType)t;
//	    at = at.base(replaceArgLabelsInType(at.base()));
//
//	    return at;
//	}
//
//	return t;
//    }
//
//    private List replaceArgLabelsInTypeList(List l) {
//	List m = new ArrayList(l.size());
//	for (Iterator i = l.iterator(); i.hasNext(); ) {
//	    Type t = (Type)i.next();
//	    m.add(replaceArgLabelsInType(t));
//	}
//
//	return m;
//    }

    public Label startLabel() {
	return startLabel;
    }

    public Label externalPC() {
	return startLabel;
    }

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

    
//    public ArgLabel getArgLabel(int i) {
//        return (ArgLabel)argLabels.get(i);
//    }
//
//    public List argLabels() {
//        return argLabels;
//    }
//
//    public List nonSignatureArgLabels() {
//        if (nonSignatureArgLabels == null) {
//            nonSignatureArgLabels = new ArrayList(argLabels.size());
//            for (int i = 0; i < argLabels.size(); i++) {
//                ArgLabel a = (ArgLabel)argLabels.get(i);
//                ArgLabel b = 
//                    ((JifTypeSystem)ts).argLabel(
//                        a.position(),
//                        a.uid(),
//                        a.index(),
//                        false,
//                        a.name());
//                nonSignatureArgLabels.add(i, b);
//            }
//        }
//        return nonSignatureArgLabels;
//    }
    public String debugString() {
	JifTypeSystem jts = (JifTypeSystem) ts;
	String s = "method " + flags.translate() + jts.unlabel(returnType) +
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
            sb.append(" : ");
            sb.append(returnLabel);
       }
       return sb.toString();

    }
}
