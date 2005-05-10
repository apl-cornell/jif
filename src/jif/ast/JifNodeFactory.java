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
    AmbNewArray AmbNewArray(Position pos, TypeNode base, Object expr, List dims);
    AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos, Receiver base, Object expr);
    JoinLabelNode JoinLabelNode(Position pos, List components);
    PolicyLabelNode PolicyLabelNode(Position pos, PrincipalNode owner, List readers);
    AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, Expr expr);
    AmbVarLabelNode AmbVarLabelNode(Position pos, String name);
    AmbThisLabelNode AmbThisLabelNode(Position pos);
    CanonicalLabelNode CanonicalLabelNode(Position pos, Label label);
    AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr);
    AmbPrincipalNode AmbPrincipalNode(Position pos, String name);
    CanonicalPrincipalNode CanonicalPrincipalNode(Position pos, Principal principal);
    JifClassDecl JifClassDecl(Position pos, Flags flags, String name, List params, TypeNode superClass, List interfaces, List authority, ClassBody body);
    JifMethodDecl JifMethodDecl(Position pos, Flags flags, TypeNode returnType, String name, LabelNode startLabel, List arguments, LabelNode endLabel, List exceptions, List constraints, Block body);
    JifConstructorDecl JifConstructorDecl(Position pos, Flags flags, String name, LabelNode startLabel, LabelNode returnLabel, List arguments, List exceptions, List constraints, Block body);
    AmbParam AmbParam(Position pos, String name);
    AmbParam AmbParam(Position pos, String name, ParamInstance pi);
    AmbExprParam AmbParam(Position pos, Expr expr, ParamInstance expectedPI);
    ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, String name);
    CanonicalConstraintNode CanonicalConstraintNode(Position pos, Assertion constraint);
    AuthConstraintNode AuthConstraintNode(Position pos, List principals);
    CallerConstraintNode CallerConstraintNode(Position pos, List principals);
    ActsForConstraintNode ActsForConstraintNode(Position pos, PrincipalNode actor, PrincipalNode granter);
    ActsFor ActsFor(Position pos, Principal actor, Principal granter, Stmt consequent, Stmt alternative);
    ActsFor ActsFor(Position pos, PrincipalNode actor, PrincipalNode granter, Stmt consequent, Stmt alternative);
    LabelIf LabelIf(Position pos, LabelExpr lhs, LabelExpr rhs, Stmt consequent, Stmt alternative);
    LabelExpr LabelExpr(Position pos, Label l);
    DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound, LabelNode label, Stmt body);
    DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode bound, LabelNode label);
    NewLabel NewLabel(Position pos, LabelNode label);
}
