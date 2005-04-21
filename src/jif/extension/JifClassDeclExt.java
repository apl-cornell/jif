package jif.extension;

import java.util.Iterator;

import jif.ast.JifClassDecl;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassMember;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

/** The extension of the <code>JifClassDecl</code> node. 
 * 
 *  @see jif.ast.JifClassDecl
 */
public class JifClassDeclExt extends Jif_c {
    public JifClassDeclExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
	JifClassDecl n = (JifClassDecl) node();

	JifTypeSystem jts = (JifTypeSystem) lc.typeSystem();
	JifContext A = lc.jifContext();
        A = (JifContext)A.pushClass(n.type(), n.type());
        A = n.addParamsToContext(A);
        A = n.addAuthorityToContext(A);

        A.setEntryPC(jts.bottomLabel());

	JifParsedPolyType ct = (JifParsedPolyType) n.type();

	// Check the authority of the class against the superclass.
	if (ct.superType() instanceof JifClassType) {
            JifClassType superType = (JifClassType) ct.superType();

	    for (Iterator i = superType.authority().iterator(); i.hasNext(); ) {
		Principal pl = (Principal) i.next();

		boolean sat = false;

		for (Iterator j = ct.authority().iterator(); j.hasNext(); ) {
		    Principal pp = (Principal) j.next();

		    if (A.actsFor(pp, pl)) {
			sat = true;
			break;
		    }
		}

		if (! sat) {
		    throw new SemanticException(
			"Unsatisfied authority constraint on class \"" +
			ct + "\".");
		}
	    }
	}

        A = (JifContext) n.enterScope(A);
	
        LabelChecker newLC = lc.context(A);
        
        // let the label checker know that we are about to enter a class body
        newLC.enteringClassBody();
        
        // label check the body
        ClassBody body = (ClassBody) newLC.labelCheck(n.body());

        // let the label checker know that we have left the class body
        n = newLC.leavingClassBody((JifClassDecl)n.body(body));
        return n;
    }
}
