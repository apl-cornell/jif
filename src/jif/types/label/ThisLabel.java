package jif.types.label;

import jif.types.JifClassType;


/** The label derived from a label paramter. 
 */
public interface ThisLabel extends ParamLabel
{
    JifClassType classType();
}
