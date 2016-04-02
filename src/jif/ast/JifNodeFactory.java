package jif.ast;

import java.util.List;

import jif.types.Assertion;
import jif.types.ParamInstance;
import jif.types.label.Label;
import jif.types.label.Policy;
import jif.types.principal.Principal;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.If;
import polyglot.ast.Javadoc;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.util.Position;

/** The node factory of the Jif extension.
 */
public interface JifNodeFactory extends NodeFactory {
    InstTypeNode InstTypeNode(Position pos, TypeNode type,
            List<ParamNode> params);

    LabeledTypeNode LabeledTypeNode(Position pos, TypeNode type,
            LabelNode label);

    AmbNewArray AmbNewArray(Position pos, TypeNode base, Object expr,
            List<Expr> dims, int addDims);

    AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos, Receiver base,
            Object expr);

    JoinLabelNode JoinLabelNode(Position pos,
            List<LabelComponentNode> components);

    MeetLabelNode MeetLabelNode(Position pos,
            List<LabelComponentNode> components);

    PolicyNode ReaderPolicyNode(Position pos, PrincipalNode owner,
            List<PrincipalNode> readers);

    PolicyNode WriterPolicyNode(Position pos, PrincipalNode owner,
            List<PrincipalNode> writers);

    PolicyNode PolicyNode(Position pos, Policy pol);

    AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, Expr expr);

    AmbVarLabelNode AmbVarLabelNode(Position pos, Id name);

    AmbThisLabelNode AmbThisLabelNode(Position pos);

    AmbProviderLabelNode AmbProviderLabelNode(Position pos, TypeNode typeNode);

    CanonicalLabelNode CanonicalLabelNode(Position pos, Label label);

    AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr);

    AmbPrincipalNode AmbPrincipalNode(Position pos, Id name);

    AmbPrincipalNode AmbConjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right);

    AmbPrincipalNode AmbDisjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right);

    CanonicalPrincipalNode CanonicalPrincipalNode(Position pos,
            Principal principal);

    JifClassDecl JifClassDecl(Position pos, Flags flags, Id name,
            List<ParamDecl> params, TypeNode superClass,
            List<TypeNode> interfaces, List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body,
            Javadoc javadoc);

    JifMethodDecl JifMethodDecl(Position pos, Flags flags, TypeNode returnType,
            Id name, LabelNode startLabel, List<Formal> arguments,
            LabelNode endLabel, List<TypeNode> exceptions,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc);

    JifConstructorDecl JifConstructorDecl(Position pos, Flags flags, Id name,
            LabelNode startLabel, LabelNode returnLabel, List<Formal> arguments,
            List<TypeNode> exceptions,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc);

    AmbParam AmbParam(Position pos, Id name);

    AmbParam AmbParam(Position pos, Id name, ParamInstance pi);

    AmbExprParam AmbParam(Position pos, Expr expr, ParamInstance expectedPI);

    ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, Id name);

    CanonicalConstraintNode CanonicalConstraintNode(Position pos,
            Assertion constraint);

    AuthConstraintNode AuthConstraintNode(Position pos,
            List<PrincipalNode> principals);

    CallerConstraintNode CallerConstraintNode(Position pos,
            List<PrincipalNode> principals);

    AutoEndorseConstraintNode AutoEndorseConstraintNode(Position pos,
            LabelNode endorseTo);

    PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(
            Position pos, PrincipalNode actor, PrincipalNode granter);

    PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(
            Position pos, PrincipalNode actor, PrincipalNode granter,
            boolean isEquiv);

    LabelActsForPrincipalConstraintNode LabelActsForPrincipalConstraintNode(
            Position pos, LabelNode actor, PrincipalNode granter);

    LabelActsForLabelConstraintNode LabelActsForLabelConstraintNode(
            Position pos, LabelNode actor, LabelNode granter);

    LabelLeAssertionNode LabelLeAssertionNode(Position pos, LabelNode actor,
            LabelNode granter, boolean isEquiv);

    LabelExpr LabelExpr(Position pos, Label l);

    DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound,
            LabelNode label, Stmt body);

    DeclassifyStmt DeclassifyStmt(Position pos, LabelNode label, Stmt body);

    DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode bound,
            LabelNode label);

    DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode label);

    EndorseStmt EndorseStmt(Position pos, LabelNode bound, LabelNode label,
            Stmt body);

    EndorseStmt EndorseStmt(Position pos, LabelNode label, Stmt body);

    CheckedEndorseStmt CheckedEndorseStmt(Position pos, Expr e, LabelNode bound,
            LabelNode label, If body);

    EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode bound,
            LabelNode label);

    EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode label);

    NewLabel NewLabel(Position pos, LabelNode label);

    PrincipalExpr PrincipalExpr(Position pos, PrincipalNode principal);

    TypeNode ConstArrayTypeNode(Position position, TypeNode node);

    Prologue Prologue(Position pos, List<Stmt> stmts);
}
