package jif.types;

import polyglot.util.*;
import java.util.*;

/** The auto endorse constraint. 
 */
public interface AutoEndorseConstraint extends Assertion {
    List principals();
    AutoEndorseConstraint principals(List principals);
}
