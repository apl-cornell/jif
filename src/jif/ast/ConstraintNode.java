package jif.ast;

import polyglot.ast.*;
import jif.types.*;

/** The root of various constraint nodes. 
 */
public interface ConstraintNode extends Node {
    Assertion constraint();
    ConstraintNode constraint(Assertion constraint);
}
