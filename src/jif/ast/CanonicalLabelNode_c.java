package jif.ast;

import jif.types.label.Label;
import polyglot.util.*;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implementation of the <code>CanonicalLabelNode</code> interface.
 */
public class CanonicalLabelNode_c extends LabelNode_c implements CanonicalLabelNode
{
    public CanonicalLabelNode_c(Position pos, Label label) {
	super(pos, label);
    }
}
