package jif.extension;

import java.util.List;

import jif.ast.JifClassDecl;
import jif.ast.JifClassDecl_c;
import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import polyglot.ast.ClassDeclOps;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.types.ConstructorInstance;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.SerialVersionUID;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

/** The delegate of the <code>JifClassDecl</code> node.
 *
 *  @see jif.ast.JifClassDecl
 */
public class JifClassDeclDel extends JifDel_c implements ClassDeclOps {
    private static final long serialVersionUID = SerialVersionUID.generate();

    /**
     * @see polyglot.ast.JLDel_c#typeCheck(polyglot.visit.TypeChecker)
     */
    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifClassDecl cd = (JifClassDecl) this.node();
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();

        // check that there are not two static methods called "main"
        MethodInstance staticMain = null;
        List<? extends MethodInstance> mains = cd.type().methodsNamed("main");

        for (MethodInstance mi : mains) {
            if (mi.flags().isStatic()) {
                if (staticMain != null) {
                    // this is the second static method named main.
                    // we don't like this.
                    throw new SemanticDetailedException(
                            "Only one static "
                                    + "method named \"main\" allowed per class.",
                            "Two main methods can be used to invoke a Jif "
                                    + "program: public static main(String[]), or public "
                                    + "static main(principal, String[]). Any class may "
                                    + "have at most one static method named \"main\".",
                            mi.position());
                }

                staticMain = mi;
            }
        }

        // check that if this class extends Throwable, then it does not have
        // any parameters.
        if (cd.type().isSubtype(ts.Throwable())) {
            JifParsedPolyType jppt = (JifParsedPolyType) cd.type();
            if (jppt.params().size() > 0) {
                throw new SemanticDetailedException(
                        "Subclasses of "
                                + "java.lang.Throwable can not have parameters.",
                        "Subclasses of java.lang.Throwable can not have any parameters, "
                                + "since Jif does not currently support catch blocks for "
                                + "parameterized subclasses of Throwable.",
                        jppt.position());
            }
        }

        return super.typeCheck(tc);
    }

    @Override
    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
        // XXX Should refactor to separate Del functionality out of JifClassDecl.
        ((JifClassDecl_c) node()).prettyPrintHeader(w, tr);
    }

    @Override
    public void prettyPrintFooter(CodeWriter w, PrettyPrinter tr) {
        // XXX Should refactor to separate Del functionality out of JifClassDecl.
        ((JifClassDecl_c) node()).prettyPrintFooter(w, tr);
    }

    @Override
    public Node addDefaultConstructor(TypeSystem ts, NodeFactory nf,
            ConstructorInstance defaultConstructorInstance)
                    throws SemanticException {
        // XXX Should refactor to separate Del functionality out of JifClassDecl.
        return ((JifClassDecl_c) node()).addDefaultConstructor(ts, nf,
                defaultConstructorInstance);
    }

}
