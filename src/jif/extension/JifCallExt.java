package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>Call</code> node. 
 * 
 *  @see polyglot.ext.jl.ast.Call_c
 */
public class JifCallExt extends Jif_c
{
    public JifCallExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
	Call me = (Call) node();

	JifContext A = lc.jifContext();
        A = (JifContext) me.enterScope(A);
	JifTypeSystem ts = lc.jifTypeSystem();

	JifMethodInstance mi = (JifMethodInstance)me.methodInstance();
	ReferenceType rt = mi.container();

	Receiver target = (Receiver) lc.context(A).labelCheck(me.target());

	A = (JifContext) A.pushBlock();
        
        boolean npExc = false;
        Label excPath = null;

	Label objLabel = null;
	
	if (target instanceof Expr) {
	    Expr e = (Expr) target;

	    if (e.type() == null) 
                throw new InternalCompilerError("Type of " + e + " is null", e.position());
                
	    PathMap Xs = X(target);
	    A.setPc(Xs.N());

            if (! (target instanceof Special)) {
                if (!((JifCallDel)node().del()).targetIsNeverNull()) {
                    // the target may be null		    
                    npExc = true;
                    excPath = (Label) Xs.N().copy();
                }
		objLabel = Xs.NV();
		A.setPc(Xs.NV());
            }
	    else {
		objLabel = ((JifClassType) lc.context().currentClass()).thisLabel();
	    }
	}

        CallHelper helper = new CallHelper(rt, objLabel, mi, me.arguments(), node().position());
	helper.checkCall(lc.context(A));
        A = (JifContext) A.pop();

	//subst arguments of inst_type
	if (helper.returnType() != me.type()) {
	    me = (Call) me.type(helper.returnType());
	}
        
        if (npExc) {
            // a null pointer exception may be thrown
            return X(me.target(target).arguments(helper.labelCheckedArgs()), 
		    helper.X().exc(excPath, ts.NullPointerException()));
        }
	
	return X(me.target(target).arguments(helper.labelCheckedArgs()), helper.X());
    }
}
