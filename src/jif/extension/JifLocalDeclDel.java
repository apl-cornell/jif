package jif.extension;

import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

/** The delegate of the <code>JifMethodDecl</code> node. 
 * 
 *  @see jif.ast.JifMethodDecl
 */
public class JifLocalDeclDel extends JifJL_c {
    public JifLocalDeclDel() {
    }
    
    
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        LocalDecl n = (LocalDecl) super.buildTypes(tb);
        JifTypeSystem jts = (JifTypeSystem)tb.typeSystem();

        JifLocalInstance li = (JifLocalInstance)n.localInstance();
        li.setLabel(jts.freshLabelVariable(li.position(), li.name(), "label of the local variable " + li.name()));
        
        return n.localInstance(li);
    }

    /**
     * @see polyglot.ext.jl.ast.JL_c#typeCheck(polyglot.visit.TypeChecker)
     */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        LocalDecl ld = (LocalDecl)this.node();
        if (ld.flags().isFinal() && ld.init() == null) {
            throw new SemanticException("Final local variables must have " +
                "an initializing expression.", ld.position());
        }
        
        return super.typeCheck(tc);
    }

}
