package jif.ast;

import java.util.List;

import jif.types.principal.Principal;
import polyglot.ast.Term;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implemenation of the <code>CanonicalPrincipal</code> interface. 
 */
public class CanonicalPrincipalNode_c extends PrincipalNode_c implements CanonicalPrincipalNode
{
    public CanonicalPrincipalNode_c(Position pos, Principal principal) {
	super(pos);
	this.principal = principal;
    }
    
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(principal.toString());
    }
    
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    /**
     * Visit this term in evaluation order.
     */
    public List acceptCFG(CFGBuilder v, List succs) {
        return succs;
    }
    public Term entry() {
        return this;    
    }
}
