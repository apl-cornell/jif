package jif.ast;

import java.util.List;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>CanonicalLabelNode</code> interface.
 */
public class CanonicalLabelNode_c extends LabelNode_c implements CanonicalLabelNode
{
    public CanonicalLabelNode_c(Position pos, Label label) {
	super(pos, label);
    }
    
    @Override
    public boolean isDisambiguated() {
        return true;
    }    
    
    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        if (!this.label().isCanonical()) {
            // label should be canonical by the time we start typechecking.
            throw new InternalCompilerError(this.label() + " is not canonical.");
        }
        LabelTypeCheckUtil ltcu = ((JifTypeSystem)tc.typeSystem()).labelTypeCheckUtil(); 
        ltcu.typeCheckLabel(tc, label());        
        return super.typeCheck(tc);
    }
    
    @Override
    public Term firstChild() {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Term> acceptCFG(CFGBuilder v, List succs) {
        return succs;
    }
}
