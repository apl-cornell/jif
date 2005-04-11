package jif.ast;

import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import java.util.*;

/** The node factory of the Jif extension. 
 */
public interface JifNodeFactory extends NodeFactory {
    InstTypeNode InstTypeNode(Position pos, TypeNode type, List params);
    LabeledTypeNode LabeledTypeNode(Position pos, TypeNode type, LabelNode label);
    AmbNewArray AmbNewArray(Position pos, TypeNode base, String name, List dims);
    AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos, Receiver base, String name);
    JoinLabelNode JoinLabelNode(Position pos, List components);
    PolicyLabelNode PolicyLabelNode(Position pos, PrincipalNode owner, List readers);
    AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, String name);
    AmbVarLabelNode AmbVarLabelNode(Position pos, String name);
    AmbThisLabelNode AmbThisLabelNode(Position pos);
    CanonicalLabelNode CanonicalLabelNode(Position pos, Label label);
    AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr);
    CanonicalPrincipalNode CanonicalPrincipalNode(Position pos, Principal principal);
    JifClassDecl JifClassDecl(Position pos, Flags flags, String name, List params, boolean inv, TypeNode superClass, List interfaces, List authority, ClassBody body);
    JifMethodDecl JifMethodDecl(Position pos, Flags flags, TypeNode returnType, String name, LabelNode startLabel, List arguments, LabelNode endLabel, List exceptions, List constraints, Block body);
    JifConstructorDecl JifConstructorDecl(Position pos, Flags flags, String name, LabelNode startLabel, LabelNode returnLabel, List arguments, List exceptions, List constraints, Block body);
    AmbParam AmbParam(Position pos, String name);
    ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, String name);
    CanonicalConstraintNode CanonicalConstraintNode(Position pos, Assertion constraint);
    AuthConstraintNode AuthConstraintNode(Position pos, List principals);
    CallerConstraintNode CallerConstraintNode(Position pos, List principals);
    ActsForConstraintNode ActsForConstraintNode(Position pos, PrincipalNode actor, PrincipalNode granter);
    SwitchLabel SwitchLabel(Position pos, Expr expr, List cases);
    LabelCase LabelCase(Position pos, Formal decl, LabelNode label, Stmt body);
    LabelCase LabelCase(Position pos, LabelNode label, Stmt body);
    LabelCase LabelCase(Position pos, Stmt body);
    ActsFor ActsFor(Position pos, PrincipalNode actor, PrincipalNode granter, Stmt consequent);
    ActsFor ActsFor(Position pos, PrincipalNode actor, PrincipalNode granter, Stmt consequent, Stmt alternative);
    DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound, LabelNode label, Stmt body);
    DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode bound, LabelNode label);
    NewLabel NewLabel(Position pos, LabelNode label);
}
