package jif.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import java.util.*;

/** An ambiguous parameter.
 */
public interface AmbParam extends ParamNode, Ambiguous {
    String name();
}
