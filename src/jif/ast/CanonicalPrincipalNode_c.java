package jif.ast;

import java.util.List;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.label.AccessPath;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implemenation of the <code>CanonicalPrincipal</code> interface. 
 */
public class CanonicalPrincipalNode_c extends PrincipalNode_c implements CanonicalPrincipalNode
{
    public CanonicalPrincipalNode_c(Position pos, Principal principal) {
	super(pos);
	this.principal = principal;
    }
    
    public boolean isDisambiguated() {
        return true;
    }    

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        LabelTypeCheckUtil.typeCheckPrincipal(tc, principal);        
        return super.typeCheck(tc);
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
