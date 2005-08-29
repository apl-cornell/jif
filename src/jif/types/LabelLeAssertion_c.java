package jif.types;

import jif.types.label.Label;
import polyglot.ext.jl.types.TypeObject_c;


public class LabelLeAssertion_c extends TypeObject_c implements LabelLeAssertion
{
    Label lhs;
    Label rhs;
    
    public LabelLeAssertion_c(JifTypeSystem ts, Label lhs, Label rhs) {
        super(ts);
	this.lhs = lhs;
	this.rhs = rhs;
    }

    public Label lhs() {
	return lhs;
    }

    public Label rhs() {
	return rhs;
    }
    
    public boolean isCanonical() {
	return true;
    }
}
