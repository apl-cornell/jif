package jif.ast;

import java.util.List;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.JifContext;
import jif.types.principal.Principal;
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
    
    List<ConstraintNode<ActsForConstraint<ActsForParam, Principal>>> constraints();
    JifClassDecl constraints(
            List<ConstraintNode<ActsForConstraint<ActsForParam, Principal>>> constraints);

    JifClassDecl type(polyglot.types.Type type);
    
    JifContext addParamsToContext(JifContext A);
    JifContext  addAuthorityToContext(JifContext A);    
}
