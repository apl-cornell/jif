package jif.extension;

import jif.ast.*;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** The Jif extension of the <code>Field</code> node. 
 * 
 *  @see polyglot.ast.Field
 */
public class JifFieldExt extends Jif_c
{
    public JifFieldExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckIncrement(LabelChecker lc) throws SemanticException
    {
	JifNodeFactory nf = new JifNodeFactory_c();
	Field fe = (Field) node();
	Position pos = fe.position();
	FieldAssign fae = nf.FieldAssign(pos, fe, Assign.ADD_ASSIGN, 
		              nf.IntLit(pos, IntLit.INT, 1));


	fae = (FieldAssign)((JifFieldAssignExt) fae.ext()).labelCheck(lc);

	return fae.left();
    }

    /*public Node labelCheckIncrement(LabelChecker lc) throws SemanticException
    {
        JifContext A = lc.jifContext();
	JifTypeSystem ts = lc.jifTypeSystem();

	Field fe = (Field) node();

	Type npe = ts.NullPointerException();
	Type are = ts.ArithmeticException();

	if (fe.target() instanceof TypeNode) {
	    // static field
	    
	}

	Expr target = (Expr) lc.context(A).labelCheck(fe.target());
	PathMap Xe = X(target);
	PathMap Xp;

	if (((JifFieldDel)node().del()).targetIsNeverNull()) {
            // target is never null, so no NPE can be thrown
	    Xp = Xe;
        }
	else {
	    Xp = Xe.exc(Xe.NV(), npe);
	}

	PathMap Xr = ts.pathMap();
	Xr = Xr.N(Xp.N());
	Xr = Xr.NV(Xp.N());

	PathMap X = Xe.join(Xr);

	if (target instanceof Special) 
	    X = Xr;

	// Must be done after visiting target to get PC right.
	FieldInstance fi = fe.fieldInstance();
	Label L = ts.labelOfField(fi, A.pc());
	//FIXME: consider the case of "special" target.
	JifContext A1 = A.enterObj(targetType(ts, A, target), X(target).NV());
	L = A1.instantiate(L);

	X = Xe.set(Path.NV, Xe.NV().join(L));
		
	if (target instanceof Special && lc.checkingInits()) {
	    Label Lr = lc.constructorReturnLabel();
	    Label Li = A.entryPC();
            if (Lr != null) 
		L = L.join(Lr);
	    if (Li != null)
		L = L.join(Li);
	}	
	
	lc.constrainLE(X.NV(), L, fe.position(), 
		       "Invalid increment: NV of the field container or PC is "
		       + "more restrictive than the label of field " 
		       + fi.name() + ".");

	return X(fe, X);
    }*/

    /** label check the field access.
     *  Refer to Andrew's thesis, Figure 4.18
     */
    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifContext A = lc.jifContext();
	JifTypeSystem ts = lc.jifTypeSystem();

	Field fe = (Field) node();

	Receiver target = checkTarget(lc, fe);
	PathMap Xe = X(target);
	
	if (! ((JifFieldDel)node().del()).targetIsNeverNull()) {
            // null pointer exception may be thrown. 
	    Type npe = ts.NullPointerException();
	    Xe = Xe.exc(Xe.NV(), npe);
	}

	// Must be done after visiting target to get PC right.
	
	//Hack: get the field instance from the type system, because "fi" may 
	//be a different instance and contain outdated data.
	FieldInstance fi = ts.findField(target.type().toReference(), fe.name(), lc.context());
	
	Label L = ts.labelOfField(fi, A.pc());

	if (target instanceof Expr) {
	    Label objLabel = X(target).NV();
	    
	    JifContext A1 = A.objectTypeAndLabel(targetType(ts, A, target, fe), objLabel);
	    L = A1.instantiate(L, true);

	    Type ft = A1.instantiate(fe.type(), true);
	    
	    if (ft != fe.type())
		fe = (Field)fe.type(ft);
	}

	PathMap X = Xe.set(Path.NV, L.join(Xe.NV()));
	
	return X(fe, X);
    }
    
    static protected Receiver checkTarget(LabelChecker lc, Field fe) 
        throws SemanticException
    {
        JifTypeSystem ts = lc.jifTypeSystem();

        if (! (fe.target() instanceof Expr)) {
            // TODO: support static fields
            return (Receiver) X(fe.target(), ts.pathMap());
        }		

        Expr target = (Expr) lc.labelCheck(fe.target());
        return target;
    }

    static protected ReferenceType targetType(JifTypeSystem ts, JifContext A, 
            Receiver target, Field fe) 
    {
        String name = fe.name();
        ReferenceType rt = A.currentClass();
        if (target instanceof Special) {
            Special st = (Special) target;
            if (st.kind() == Special.SUPER) 
                rt = (ReferenceType) A.currentClass().superType();
            else {
                /*boolean found = false;
                do {
                    for (Iterator i = rt.fields().iterator(); i.hasNext(); ) {
                        FieldInstance fi = (FieldInstance) i.next();
                        if (name.equals(fi.name())) {
                            found = true;
                            break;
                        }
                    }
                    if (found) 
                        break;

                    rt = (ReferenceType) rt.superType();
                } while (rt != null);*/
                try {
                    FieldInstance fi = ts.findField(rt, name);
                    rt = fi.container();
                }
                catch (SemanticException x) {
                    throw new InternalCompilerError("Cannot find the field "
                            + name + " in " + rt);
                }
            }
        }
        else {
            rt = (ReferenceType) ts.unlabel(target.type());
        }

        return rt;
    }

    
}
