package jif.ast;

import java.util.List;

import jif.types.Assertion;
import jif.types.JifContext;
import polyglot.ast.ClassDecl;

/** An immutable representation of the Jif class declaration.
 *  It extends the Java class declaration with the label/principal parameters
 *  and the authority constraint.
 */
public interface JifClassDecl extends ClassDecl {
    List<ParamDecl> params();

    JifClassDecl params(List<ParamDecl> params);

    List<PrincipalNode> authority();

    JifClassDecl authority(List<PrincipalNode> authority);

    List<ConstraintNode<Assertion>> constraints();

    JifClassDecl constraints(List<ConstraintNode<Assertion>> constraints);

    JifClassDecl type(polyglot.types.Type type);

    JifContext addParamsToContext(JifContext A);

    JifContext addAuthorityToContext(JifContext A);
}
