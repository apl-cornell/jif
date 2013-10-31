package jif.ast;

import java.util.List;

import jif.types.Assertion;
import polyglot.ast.ProcedureDecl;

/** An immutable representation of the Jif procedure declaration.
 *  It extends the Java procedure declaration with the start label,
 *  the return label, and various constraints, including the authority
 *  constraint, the caller constraint, and the acts-for constraint. 
 */
public interface JifProcedureDecl extends ProcedureDecl {
    LabelNode startLabel();

    LabelNode returnLabel();

    List<ConstraintNode<Assertion>> constraints();
}
