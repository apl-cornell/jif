package jif.types;

import java.util.Iterator;

import jif.ast.JifProcedureDecl;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import polyglot.ast.*;
import polyglot.types.Type;
import polyglot.util.Position;

public class VarSignature implements DefaultSignature
{
    JifTypeSystem ts;

    public VarSignature(JifTypeSystem ts) {
	this.ts = ts;
    }
    
    public Label defaultStartLabel(Position pos, String methodName) {
	return ts.freshLabelVariable(pos, methodName, 
                 "start label for the method " + methodName);
    }

    public Label defaultArgBound(Formal f) {
        String argName = f.name();
	return ts.freshLabelVariable(f.position(), argName,
                    "upper bound for the formal argument " + argName);
    }

    public Label defaultReturnLabel(ProcedureDecl pd) {
	Label Lr = ts.bottomLabel();
	
	for (Iterator i = pd.throwTypes().iterator(); i.hasNext(); ) {
	    TypeNode tn = (TypeNode) i.next();
	    Label excLabel = ts.labelOfType(tn.type(), ts.bottomLabel());
	    Lr = Lr.join(excLabel);	
	}	

	return Lr;
    }

    public Label defaultReturnValueLabel(ProcedureDecl pd) {
	JifProcedureDecl jpd = (JifProcedureDecl) pd;

	Label Lrv;
        if (jpd.returnLabel() != null) 
            Lrv = jpd.returnLabel().label();
        else
            Lrv = defaultReturnLabel(pd);
        
        
        JifProcedureInstance pi = (JifProcedureInstance)pd.procedureInstance();
        JifTypeSystem jts = (JifTypeSystem)pi.typeSystem();

        for (Iterator i = pi.formalArgLabels().iterator(); i.hasNext(); ) {
            ArgLabel a = (ArgLabel)i.next();
            Lrv = Lrv.join(a);
        }
        
	return Lrv;	
    }

    public Label defaultFieldLabel(FieldDecl fd) {
        return ts.bottomLabel();
    }
}
