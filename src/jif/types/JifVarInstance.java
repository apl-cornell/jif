package jif.types;

import jif.types.label.Label;
import polyglot.types.*;

/** Jif variable instance. A shared super class of <code>JifLocalInstance</code>
 *  and <code>JifFieldInstance</code>
 */
public interface JifVarInstance extends VarInstance
{
    UID uid();
    Label label();
    void setLabel(Label L);
}
