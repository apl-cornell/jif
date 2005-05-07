package jif.ast;

import java.util.ArrayList;
import java.util.List;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import polyglot.ast.*;
import polyglot.ext.jl.ast.New_c;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

public class JifNew_c extends New_c implements New
{
    public JifNew_c(Position pos, TypeNode tn, List arguments, 
	    ClassBody body) {
	super(pos, null, tn, arguments, body);
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	JifNew_c n = (JifNew_c) super.typeCheck(tc);

	Type t = n.tn.type();
        LabelTypeCheckUtil.typeCheckType(tc, t);
        
        n = (JifNew_c) n.type(t);
	
	return n;
    }
    public List throwTypes(TypeSystem ts) {
        List ex = new ArrayList(super.throwTypes(ts));
        New n = (New)this.node();
        if (n.objectType().type() instanceof JifClassType) {
            ex.addAll(LabelTypeCheckUtil.throwTypes((JifClassType)n.objectType().type(), 
                                                    (JifTypeSystem)ts));
        }
        return ex;
    }
}
