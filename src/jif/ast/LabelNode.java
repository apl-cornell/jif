package jif.ast;

import jif.types.label.Label;

/** A placeholder in the AST for a Jif label.
 */
public interface LabelNode extends ActsForParamNode<Label>, LabelComponentNode {
    Label label();

    LabelNode label(Label L);
}
