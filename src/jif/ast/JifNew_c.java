package jif.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.types.label.Label;

import java.util.*;

public class JifNew_c extends New_c implements New
{
    public JifNew_c(Position pos, TypeNode tn, List arguments, 
	    ClassBody body) {
	super(pos, null, tn, arguments, body);
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	JifTypeSystem jts = (JifTypeSystem) tc.typeSystem();
	JifNew_c n = (JifNew_c) super.typeCheck(tc);

	Type t = n.tn.type();

	if (t instanceof JifClassType && ! jts.isLabeled(t)) {
	    JifClassType ct = (JifClassType) t;
	    //HACK: adding a place holder
	    if (ct.isInvariant()) {
                // XXX SNC: I don't believe this code is ever called.
                //          If it were, then an exception should be thrown,
                //          as an invariant this label can only be a ParamLabel.
                Label L = jts.freshLabelVariable(n.position(), "new", 
                            "the label of the reference to the new object " +
                            "of type " + ct.fullName() + " instantiated at " +
                            n.position().toString());
                ct = ct.setInvariantThis(L);
	    }

            t = ct;
	}

        n = (JifNew_c) n.type(t);
	
	return n;
    }
}
