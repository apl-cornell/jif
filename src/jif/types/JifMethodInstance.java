package jif.types;

import polyglot.types.*;
import java.util.*;

import jif.types.label.Label;

/** Jif method instance. A wrapper of all the type information related to
 *  a method. 
 */
public interface JifMethodInstance extends MethodInstance, JifProcedureInstance
{
    Label returnValueLabel();
}
