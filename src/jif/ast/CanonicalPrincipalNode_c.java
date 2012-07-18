package jif.ast;

import java.util.List;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>CanonicalPrincipal</code> interface. 
 */
public class CanonicalPrincipalNode_c extends PrincipalNode_c implements CanonicalPrincipalNode
{
    public CanonicalPrincipalNode_c(Position pos, Principal principal) {
	super(pos);
	this.principal = principal;
    }
    
    @Override
    public boolean isDisambiguated() {
        return true;
    }    

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        LabelTypeCheckUtil ltcu = ((JifTypeSystem)tc.typeSystem()).labelTypeCheckUtil(); 
        ltcu.typeCheckPrincipal(tc, principal);        
        return super.typeCheck(tc);
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(principal.toString());
    }
    
    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    /**
     * Visit this term in evaluation order.
     */
    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }
    @Override
    public Term firstChild() {
        return null;
    }
}
