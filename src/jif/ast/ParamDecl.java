package jif.ast;

import jif.types.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

import java.util.*;

/** The AST node representing a label/principal parameter declaration.
 */
public interface ParamDecl extends Node
{
    String name();
    ParamDecl name(String name);

    ParamInstance.Kind kind();
    ParamDecl kind(ParamInstance.Kind kind);

    ParamInstance paramInstance();
    ParamDecl paramInstance(ParamInstance pi);

    boolean isPrincipal();
    boolean isLabel();
    boolean isInvariantLabel();
    boolean isCovariantLabel();
}
