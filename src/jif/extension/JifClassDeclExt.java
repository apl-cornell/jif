package jif.extension;

import java.util.Iterator;
import java.util.List;

import jif.ast.JifClassDecl;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
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
import polyglot.types.*;
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

        // label check class conformance
	labelCheckClassConformance(ct,newLC);

	// label check the body
        ClassBody body = (ClassBody) newLC.labelCheck(n.body());

        // let the label checker know that we have left the class body
        n = newLC.leavingClassBody((JifClassDecl)n.body(body));
        return n;
    }

    private void labelCheckClassConformance(JifParsedPolyType ct, LabelChecker lc) throws SemanticException {
        if (ct.flags().isInterface() || ct.flags().isAbstract()) {
            // don't need to check interfaces or abstract classes
            return;
        }

        JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.context();

        // build up a list of superclasses and interfaces that ct
        // extends/implements that may contain abstract methods that
        // ct must define.
        List superInterfaces = ts.abstractSuperInterfaces(ct);

        // check each abstract method of the classes and interfaces in
        // superInterfaces
        for (Iterator i = superInterfaces.iterator(); i.hasNext(); ) {
            ReferenceType rt = (ReferenceType)i.next();
            for (Iterator j = rt.methods().iterator(); j.hasNext(); ) {
                JifMethodInstance mi = (JifMethodInstance)j.next();
                if (!mi.flags().isAbstract()) {
                    // the method isn't abstract, so ct doesn't have to
                    // implement it.
                    continue;
                }

                boolean implFound = false;
                ReferenceType curr = ct;
                while (curr != null && !implFound) {
                    List possible = curr.methods(mi.name(), mi.formalTypes());
                    for (Iterator k = possible.iterator(); k.hasNext(); ) {
                        JifMethodInstance mj = (JifMethodInstance)k.next();
                        if (!mj.flags().isAbstract() &&
                            ((ts.isAccessible(mi, ct) && ts.isAccessible(mj, ct)) ||
                                    ts.isAccessible(mi, mj.container().toClass()))) {
                            // The method mj may be a suitable implementation of mi.
                            // mj is not abstract, and either mj's container
                            // can access mi (thus mj can really override mi), or
                            // mi and mj are both accessible from ct (e.g.,
                            // mi is declared in an interface that ct implements,
                            // and mj is defined in a superclass of ct).

                            // If neither the method instance mj nor the method
                            // instance mi is declared in the class type ct, then
                            // we need to check that it has appropriate protections.
                            if (!ct.equals(mj.container()) && !ct.equals(mi.container())) {
                                try {
                                    // check that mj can override mi, which
                                    // includes access protection checks.
                                    if (mj.canOverrideImpl(mi, true)) {
                                        // passes the java checks, now perform the label checks
                                        JifMethodDeclExt.labelCheckOverride(mj, mi, lc);
                                    }
                                }
                                catch (SemanticException e) {
                                    // change the position of the semantic
                                    // exception to be the class that we
                                    // are checking.
                                    throw new SemanticException(e.getMessage(),
                                        ct.position());
                                }
                            }
                            else {
                                // the method implementation mj or mi was
                                // declared in ct. So other checks will take
                                // care of access issues
                            }
                            implFound = true;
                            break;
                        }
                    }

                    if (curr == mi.container()) {
                        // we've reached the definition of the abstract
                        // method. We don't want to look higher in the
                        // hierarchy; this is not an optimization, but is
                        // required for correctness.
                        break;
                    }

                    curr = curr.superType() ==  null ?
                           null : curr.superType().toReference();
                }


                // did we find a suitable implementation of the method mi?
                if (!implFound && !ct.flags().isAbstract()) {
                    throw new SemanticException(ct.fullName() + " should be " +
                            "declared abstract; it does not define " +
                            mi.signature() + ", which is declared in " +
                            rt.toClass().fullName(), ct.position());
                }
            }
        }
    }

}
