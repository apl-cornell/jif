package jif.ast;

import polyglot.ast.*;
import java.util.*;

import jif.types.Assertion;

/** An immutable representation of the Jif method declaration.
 *  It extends the Java method declaration with the start label,
 *  the return label, and various constraints, including the authority
 *  constraint, the caller constraint, and the acts-for constraint. 
 */
public interface JifMethodDecl extends JifProcedureDecl, MethodDecl {
    JifMethodDecl startLabel(LabelNode startLabel);
    
    JifMethodDecl returnLabel(LabelNode returnLabel);

    JifMethodDecl constraints(List<ConstraintNode<Assertion>> constraints);
}
