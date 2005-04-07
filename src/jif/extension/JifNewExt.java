package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>New</code> node. 
 * 
 *  @see polyglot.ast.New
 */
public class JifNewExt extends Jif_c 
{
    public JifNewExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected ConstructorChecker constructorChecker = new ConstructorChecker();
    protected SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException {
	New noe = (New) node();

	JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.jifContext();
	A = (JifContext) noe.enterScope(A);

	ClassType ct = (ClassType) ts.unlabel(noe.type());

	constructorChecker.checkConstructorAuthority(ct, A);

	Label newLabel = ts.freshLabelVariable(noe.position(), "new" + ct.name(),
                            "label of the reference to the newly created " +
                            ct.name() + " object, at " + noe.position());
	if (ct instanceof JifClassType) {
	    JifClassType jct = (JifClassType) ct;
	    if (jct.isInvariant()) {
              /*
                JifPolyType pt = (JifPolyType) ((JifSubstType)jct).base();
                ParamLabel thisL = (ParamLabel) pt.thisLabel();
                JifSubst subst = (JifSubst) ((JifSubstType)jct).subst();
                newLabel = (Label) subst.get(thisL.uid());
                */

                newLabel = jct.thisLabel();
	    }
	}

	if (ts.isLabeled(noe.type()) ) {
            // error messages for equality constraints aren't displayed, so no
            // need top define error messages.  
            lc.constrain(new LabelConstraint(new NamedLabel("new_label",
                                                            "label of the reference to the newly created " + ct.name(), 
                                                            newLabel), 
                                             LabelConstraint.EQUAL, 
                                             new NamedLabel("declared_label", 
                                                            "declared label of the newly created " + ct.name(), 
                                                            ts.labelOfType(noe.type())), 
                                             A.labelEnv(),
                                             noe.position()));
        }
	CallHelper helper = new CallHelper(ct, newLabel, 
                                           (JifProcedureInstance)noe.constructorInstance(), 
                                           noe.arguments(),
		                           node().position());

	helper.checkCall(lc);

	PathMap retX = helper.X();
	PathMap X = retX.NV(retX.NV().join(newLabel));

	return X(noe.arguments(helper.labelCheckedArgs()), X);
    }
}
