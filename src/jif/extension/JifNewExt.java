package jif.extension;

import java.util.ArrayList;
import java.util.List;

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

	List throwTypes = new ArrayList(noe.del().throwTypes(ts));
    
	ClassType ct = (ClassType) ts.unlabel(noe.type());

	constructorChecker.checkConstructorAuthority(ct, A, noe.position());

	Label newLabel = ts.freshLabelVariable(noe.position(), "new" + ct.name(),
                            "label of the reference to the newly created " +
                            ct.name() + " object, at " + noe.position());

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
	CallHelper helper = new CallHelper(newLabel, ct, 
                                           (JifProcedureInstance)noe.constructorInstance(), 
                                           noe.arguments(),
		                           node().position());

	helper.checkCall(lc, throwTypes);

	PathMap retX = helper.X();
	PathMap X = retX.NV(lc.upperBound(retX.NV(), newLabel));

	checkThrowTypes(throwTypes);
	return X(noe.arguments(helper.labelCheckedArgs()), X);
    }
}
