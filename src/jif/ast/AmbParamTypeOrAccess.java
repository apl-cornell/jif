package jif.ast;

import polyglot.ast.Ambiguous;
import polyglot.ast.Receiver;

/** An ambiguous parameter type or array access. It has the form
 *  <code>receiver[name]</code>. 
 */
public interface AmbParamTypeOrAccess extends Receiver, Ambiguous
{
    /** Gets the prefix. */
    Receiver prefix();
    
    /** Gets the name. */
    String name();
}
