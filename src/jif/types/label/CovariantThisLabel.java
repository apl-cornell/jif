package jif.types.label;

import jif.types.JifClassType;

/** Covariant label. 
 */
public interface CovariantThisLabel extends CovariantParamLabel
{
    JifClassType classType();
}
