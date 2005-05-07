package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Node;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>ConstructorCall</code> node. 
 * 
 *  @see polyglot.ast.ConstructorCall
 */
public class JifConstructorCallExt extends JifStmtExt_c
{
    public JifConstructorCallExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected ConstructorChecker constructorChecker = new ConstructorChecker();
    protected SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	ConstructorCall ccs = (ConstructorCall) node();

	JifContext A = lc.jifContext();
        A = (JifContext) ccs.del().enterScope(A);

	JifConstructorInstance ci = (JifConstructorInstance)ccs.constructorInstance();
	
	ClassType ct = ci.container().toClass();
	JifClassType jct = (JifClassType) A.currentClass();
	if (ccs.kind()==ConstructorCall.SUPER) {
	    ct = (ClassType) A.currentClass().superType();
	}
	if (ccs.qualifier() != null) {
	    throw new InternalCompilerError(
		"Qualified constructor calls are not supported in Jif.");
	}

	constructorChecker.checkConstructorAuthority(ct, A, ccs.position());

	CallHelper helper = new CallHelper(jct.thisLabel(), ct, ci, 
                                           ccs.arguments(), node().position());

	helper.checkCall(lc.context(A));

	return X(ccs.arguments(helper.labelCheckedArgs()), helper.X());
    }
}
