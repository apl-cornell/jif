package jif.extension;

import java.util.Iterator;
import java.util.List;

import jif.ast.JifMethodDecl;
import jif.ast.JifNodeFactory;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Block;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.main.Report;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>JifMethodDecl</code> node. 
 * 
 *  @see jif.ast.JifMethodDecl
 */
public class JifMethodDeclExt extends JifProcedureDeclExt_c
{
    public JifMethodDeclExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifMethodDecl mn = (JifMethodDecl) node();
        JifMethodInstance mi = (JifMethodInstance) mn.methodInstance();

        // check that the labels in the method signature conform to the
        // restrictions of the superclass and/or interface method declaration.
        overrideMethodLabelCheck(lc, mi);

	JifTypeSystem ts = lc.jifTypeSystem();
      	JifContext A = lc.jifContext();
	A = (JifContext) mn.enterScope(A);
        lc = lc.context(A);

	// First, check the arguments, adjusting the context.
	Label Li = checkArguments(mi, lc);

	Block body = null;
	PathMap X;

	if (! mn.flags().isAbstract() && ! mn.flags().isNative()) {
	    // Now, check the body of the method in the new context.

	    // Visit only the body, not the formal parameters.
	    body = (Block) lc.context(A).labelCheck(mn.body());
	    X = X(body);

	    if (Report.should_report(jif_verbose, 3))
		Report.report(3, "Body path labels = " + X);

	    addReturnConstraints(Li, X, mi, lc, mi.returnType());
	}
	else {
	    // for an abstract or native method, just set the 
	    // normal termination path to the entry PC of the
	    // method.
	    X = ts.pathMap();
	    X = X.N(A.entryPC());
	}

	mn = (JifMethodDecl) X(mn.body(body), X);

	return mn;
    }

    /**
     * Check that this method instance <mi> conforms to the signatures of any
     * methods in the superclasses or interfaces that it is overriding.
     * 
     * In particular, argument labels and start labels are contravariant,
     * return labels, return value labels and labels on exception types are 
     * covariant.
     */
    protected void overrideMethodLabelCheck(LabelChecker lc, final JifMethodInstance mi) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();

        MethodDecl md = (MethodDecl)this.node();
        for (Iterator iter = mi.implemented().iterator(); iter.hasNext(); ) {
            final JifMethodInstance mj = (JifMethodInstance) iter.next();
            
            if (! ts.isAccessible(mj, lc.context())) {
                continue;
            }            
            CallHelper.OverrideHelper(mj, mi, lc).checkOverride(lc);
        }
    }

}
