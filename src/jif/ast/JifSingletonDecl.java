package jif.ast;

import java.util.List;

import jif.types.Assertion;
import jif.types.JifContext;
import polyglot.ast.ClassDecl;

/** An immutable representation of the Jif singleton declaration.
 *  It inherits the behavior of the JifClassDecl but may not be
 *  instantiated. Rather, all uses of the singleton will access the
 *  same instance, a static field of the class.
 */
public interface JifSingletonDecl extends JifClassDecl {
    List<ParamDecl> params();
    JifClassDecl params(List<ParamDecl> params);

    List<PrincipalNode> authority();
    JifClassDecl authority(List<PrincipalNode> authority);
    
    List<ConstraintNode<Assertion>> constraints();

    JifClassDecl constraints(List<ConstraintNode<Assertion>> constraints);

    JifClassDecl type(polyglot.types.Type type);
    
    JifContext addParamsToContext(JifContext A);
    JifContext  addAuthorityToContext(JifContext A);    
}
