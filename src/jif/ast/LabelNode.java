package jif.ast;

import polyglot.ast.Expr;
import jif.types.label.Label;

/** A placeholder in the AST for a Jif label.
 */
public interface LabelNode extends ActsForParamNode<Label>, Expr, LabelComponentNode {
    Label label();
    LabelNode label(Label L);
}
