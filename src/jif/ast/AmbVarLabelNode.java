package jif.ast;

import polyglot.ast.Ambiguous;

/** An ambiguous variable label node. 
 */
public interface AmbVarLabelNode extends LabelNode, Ambiguous {
    String name();
}
