package jif.ast;

import polyglot.ast.*;
import jif.types.*;
import jif.types.label.Label;

/** A placeholder in the AST for a Jif label.
 */
public interface LabelNode extends ParamNode {
    Label label();
    LabelNode label(Label L);
}
