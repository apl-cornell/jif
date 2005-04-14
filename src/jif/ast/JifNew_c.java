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
        n = (JifNew_c) n.type(t);
	
	return n;
    }
}
