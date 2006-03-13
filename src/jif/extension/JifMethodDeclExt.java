package jif.extension;

import java.util.Iterator;

import jif.ast.JifMethodDecl;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;

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

        // check covariance of labels
        checkCovariance(mi, lc);
        
        // check that the labels in the method signature conform to the
        // restrictions of the superclass and/or interface method declaration.
        overrideMethodLabelCheck(lc, mi);

	JifTypeSystem ts = lc.jifTypeSystem();
      	JifContext A = lc.jifContext();
	A = (JifContext) mn.enterScope(A);
        lc = lc.context(A);

        // let the label checker know that we are about to enter a method decl
        lc.enteringMethod(mi);

        // First, check the arguments, adjusting the context.
	Label Li = checkEnforceSignature(mi, lc);

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
	    X = X.N(A.currentCodePCBound());
	}

	mn = (JifMethodDecl) X(mn.body(body), X);

        // let the label checker know that we have left the method
        mn = lc.leavingMethod(mn);
    
	return mn;
    }

    /**
     * This method checks that covariant labels are not used in contravariant
     * positions.
     * @throws SemanticDetailedException
     *
     */
    protected void checkCovariance(JifMethodInstance mi, LabelChecker lc) throws SemanticDetailedException {
        if (mi.flags().isStatic()) {
            // static methods are ok, since they do not override other methods.
            return;
        }
        ProcedureDecl mn = (ProcedureDecl) node();
        Position declPosition = mn.position();

        // check pc bound
        Label Li = mi.pcBound();
        if (Li.isCovariant()) {
            throw new SemanticDetailedException("The pc bound of a method " +
                    "can not be the covariant label " + Li + ".",
             "The pc bound of a method " +
                    "can not be the covariant label " + Li + ". " +
                "Otherwise, information may be leaked by casting the " +
                "low-parameter class to a high-parameter class, and masking " +
                "the low side-effects that invoking the method may cause.",
                        declPosition);
        }
        
        // check arguments
        JifTypeSystem ts = lc.jifTypeSystem();
        Iterator types = mi.formalTypes().iterator();

        int index = 0;
        while (types.hasNext()) {
            Type tj = (Type) types.next();

            // This is the declared label of the parameter.
            Label argBj = ((ArgLabel)ts.labelOfType(tj)).upperBound();
            if (argBj.isCovariant()) {
                String name = ((Formal)mn.formals().get(index)).name();
                throw new SemanticDetailedException("The method " +
                        "argument " + name + 
                        " can not be labeled with the covariant label " + argBj + ".",
            "The method argument " + name + 
                        " can not be labeled with the covariant label " + argBj + ". " +
                    "Otherwise, information may be leaked by casting the " +
                    "low-parameter class to a high-parameter class, and calling " +
                    "the method with a high security parameter, which the " +
                    "method regards as low security information.",
                            argBj.position());
            }

            index++;
        }

    }
    
    /**
     * Check that this method instance <mi> conforms to the signatures of any
     * methods in the superclasses or interfaces that it is overriding.
     * 
     * In particular, argument labels and pc bounds are contravariant,
     * return labels, return value labels and labels on exception types are 
     * covariant.
     */
    protected void overrideMethodLabelCheck(LabelChecker lc, final JifMethodInstance mi) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();
        for (Iterator iter = mi.implemented().iterator(); iter.hasNext(); ) {
            final JifMethodInstance mj = (JifMethodInstance) iter.next();
            
            if (! ts.isAccessible(mj, lc.context())) {
                continue;
            }            
            CallHelper.OverrideHelper(mj, mi, lc).checkOverride(lc);
        }
    }

}
