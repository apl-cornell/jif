package jif.types;

import polyglot.types.*;
import polyglot.util.*;
import jif.ast.*;
import jif.types.label.Label;
import polyglot.ext.jl.types.*;

/** A labeled type. 
 */
public interface LabeledType extends Type
{
    Type typePart();
    LabeledType typePart(Type type);

    Label labelPart();
    LabeledType labelPart(Label L);
}
