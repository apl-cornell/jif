package jif.ast;

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
        if (label() instanceof DynamicLabel) {
            DynamicLabel dl = (DynamicLabel)label();
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
            if (!ts.isLabel(dl.path().type())) {
                throw new SemanticException("The type of a dynamic label must be \"label\"", this.position());
            }
        }
        return super.typeCheck(tc);
    }
}
