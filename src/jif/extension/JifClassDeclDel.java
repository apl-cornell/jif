package jif.extension;

import java.util.Iterator;
import java.util.List;

import jif.ast.JifClassDecl;
import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import polyglot.ast.Node;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.visit.TypeChecker;

/** The delegate of the <code>JifClassDecl</code> node.
 *
 *  @see jif.ast.JifClassDecl
 */
public class JifClassDeclDel extends JifJL_c {
    public JifClassDeclDel() {
    }

    /**
     * @see polyglot.ext.jl.ast.JL_c#typeCheck(polyglot.visit.TypeChecker)
     */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifClassDecl cd = (JifClassDecl)this.node();
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();


        // check that there are not two static methods called "main"
        MethodInstance staticMain = null;
        List mains = cd.type().methodsNamed("main");
        for (Iterator iter = mains.iterator(); iter.hasNext(); ) {
            MethodInstance mi = (MethodInstance)iter.next();
            if (mi.flags().isStatic()) {
                if (staticMain != null) {
                    // this is the second static method named main.
                    // we don't like this.
                    throw new SemanticException("Jif allows only one static " +
                        "method named \"main\" per class.", mi.position());
                }

                staticMain = mi;
            }
        }

        // check that if this class extends Throwable, then it does not have
        // any parameters.
        if (cd.type().isSubtype(ts.Throwable())) {
            JifParsedPolyType jppt = (JifParsedPolyType)cd.type();
            if (jppt.actuals().size() > 0) {
                throw new SemanticException("A subclass of " +
                    "java.lang.Throwable may not have any parameters.",
                    jppt.position());
            }
        }

        // check that if this is a Java class, then it does not have
        // any parameters.
        if (!ts.isJifClass(cd.type())) {
            JifParsedPolyType jppt = (JifParsedPolyType)cd.type();
            if (jppt.actuals().size() > 0) {
                throw new SemanticException("A Java class may not have any parameters.",
                    jppt.position());
            }
        }

        return super.typeCheck(tc);
    }

}
