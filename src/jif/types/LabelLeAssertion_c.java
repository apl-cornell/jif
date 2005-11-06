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
    
    public LabelLeAssertion lhs(Label lhs) {
        LabelLeAssertion_c n = (LabelLeAssertion_c) copy();
        n.lhs = lhs;
        return n;
    }
    
    public LabelLeAssertion rhs(Label rhs) {
        LabelLeAssertion_c n = (LabelLeAssertion_c) copy();
        n.rhs = rhs;
        return n;
    }

    public boolean isCanonical() {
	return true;
    }
}
