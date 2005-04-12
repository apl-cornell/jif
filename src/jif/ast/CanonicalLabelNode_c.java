package jif.ast;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifTypeSystem;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>CanonicalLabelNode</code> interface.
 */
public class CanonicalLabelNode_c extends LabelNode_c implements CanonicalLabelNode
{
    public CanonicalLabelNode_c(Position pos, Label label) {
	super(pos, label);
    }
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        LabelTypeCheckUtil.typeCheckLabel(tc, label());        
        return super.typeCheck(tc);
    }
}
