package jif.ast;

import jif.types.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import java.util.*;

/** This class is the root of all the classes that may be parameters,
 *  including label node classes and principal node classes. 
 */
public interface ParamNode extends Node
{
    Param parameter();
}
