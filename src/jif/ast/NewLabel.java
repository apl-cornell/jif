package jif.ast;

import jif.types.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import java.util.*;

/** An immutable representation of the Jif <code>new label</code> 
 *  statement. 
 */
public interface NewLabel extends Expr {
    LabelNode label();
    NewLabel label(LabelNode label);
}
