package jif.extension;

import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.types.*;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
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

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifTypeSystem jts = (JifTypeSystem)ar.typeSystem();
        
        LocalDecl n = (LocalDecl)node();
        LocalInstance li = n.localInstance();
        li.setFlags(n.flags());
        li.setName(n.name());
        li.setType(n.declType());

        return n;
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
