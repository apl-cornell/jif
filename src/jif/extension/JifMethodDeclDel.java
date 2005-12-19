package jif.extension;

import java.util.List;

import jif.ast.JifMethodDecl;
import jif.types.*;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.visit.TypeChecker;

/** The delegate of the <code>JifMethodDecl</code> node. 
 * 
 *  @see jif.ast.JifMethodDecl
 */
public class JifMethodDeclDel extends JifProcedureDeclDel {
    public JifMethodDeclDel() {
    }
    
    /**
     * @see polyglot.ext.jl.ast.JL_c#typeCheck(polyglot.visit.TypeChecker)
     */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifMethodDecl jmd = (JifMethodDecl)this.node();
        JifMethodInstance mi = (JifMethodInstance)jmd.methodInstance(); 
        if ("main".equals(mi.name()) && mi.flags().isStatic()) {
            // check that the class is not parameterized.
            JifClassType currClass = (JifClassType)tc.context().currentClass();
            if (currClass.actuals().size() > 0) {                
                throw new SemanticDetailedException(
                      "A parameterized class can not have a \"main\" method.",
                      "Parameterized classes cannot have a main method, as " +
                      "the invoker of the main method has no way to specify " +
                      "instantiations of the class parameters.",
                      mi.position());
            }
            // ensure the signature of mi is either main(String[]) or
            // main(principal, String[])
            boolean wrongSig = true;
            List formalTypes = mi.formalTypes();
            
            JifTypeSystem jts = (JifTypeSystem)tc.typeSystem();
            Type stringArray = jts.arrayOf(jts.String());
            if (formalTypes.size() == 1) {
                Type formal0 = jts.unlabel((Type)formalTypes.get(0));
                if (formal0.equals(stringArray)) {
                    // the main method signature is main(String[])
                    wrongSig = false;
                }
            }
            else if (formalTypes.size() == 2) {
                Type formal0 = jts.unlabel((Type)formalTypes.get(0));
                Type formal1 = jts.unlabel((Type)formalTypes.get(1));
                if (formal0.equals(jts.Principal()) && formal1.equals(stringArray)) {
                    // the main method signature is main(principal, String[])
                    wrongSig = false;
                }                
            }

            if (wrongSig) {
                // warn the user that there may be a potentially wrong 
                // signature
                ErrorQueue eq = tc.errorQueue();
                eq.enqueue(ErrorInfo.WARNING,
                          "The signature of an invocable main " +
                          "method in a Jif class should either be " +
                          "\"main(String[] args)\" or \"main(principal p, " +
                          "String[] args)\" where p will be the user " +
                          "invoking the main method. This method may have " +
                          "an incorrect signature.", 
                          mi.position());
            }
        }
        
        return super.typeCheck(tc);
    }

}
