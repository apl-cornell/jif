package jif.ast;

import java.util.Collections;
import java.util.List;

import jif.types.Assertion;
import jif.types.ParamInstance;
import jif.types.label.Label;
import jif.types.label.Policy;
import jif.types.principal.Principal;
import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.Binary;
import polyglot.ast.Binary.Operator;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Catch;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.DelFactory;
import polyglot.ast.Disamb;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.If;
import polyglot.ast.Javadoc;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.NodeFactory_c;
import polyglot.ast.Receiver;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifNodeFactory</code> interface.
 */
public class JifNodeFactory_c extends NodeFactory_c implements JifNodeFactory {
    public JifNodeFactory_c() {
        this(new JifExtFactory_c());
    }

    protected JifNodeFactory_c(JifExtFactory extFact) {
        super(extFact, new JifDelFactory_c());
    }

    protected JifNodeFactory_c(JifExtFactory extFact, DelFactory delFact) {
        super(extFact, delFact);
    }

    protected JifExtFactory jifExtFactory() {
        return (JifExtFactory) this.extFactory();
    }

    @Override
    public Disamb disamb() {
        return new JifDisamb_c();
    }

    @Override
    public CanonicalTypeNode CanonicalTypeNode(Position pos, Type type) {
        CanonicalTypeNode n = new JifCanonicalTypeNode_c(pos, type);
        n = ext(n, extFactory().extCanonicalTypeNode());
        n = del(n, delFactory().delCanonicalTypeNode());
        return n;
    }

    @Override
    public InstTypeNode InstTypeNode(Position pos, TypeNode type,
            List<ParamNode> params) {
        InstTypeNode n = new InstTypeNode_c(pos, type, params);
        n = ext(n, jifExtFactory().extInstTypeNode());
        n = del(n, delFactory().delTypeNode());
        return n;
    }

    @Override
    public LabeledTypeNode LabeledTypeNode(Position pos, TypeNode type,
            LabelNode label) {
        LabeledTypeNode n = new LabeledTypeNode_c(pos, type, label);
        n = ext(n, jifExtFactory().extLabeledTypeNode());
        n = del(n, delFactory().delTypeNode());
        return n;
    }

    @Override
    public AmbNewArray AmbNewArray(Position pos, TypeNode baseType, Object expr,
            List<Expr> dims, int addDims) {
        AmbNewArray n = new AmbNewArray_c(pos, baseType, expr, dims, addDims);
        n = ext(n, jifExtFactory().extAmbNewArray());
        n = del(n, delFactory().delAmbExpr());
        return n;
    }

    @Override
    public AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos,
            Receiver base, Object expr) {
        AmbParamTypeOrAccess n = new AmbParamTypeOrAccess_c(pos, base, expr);
        n = ext(n, jifExtFactory().extAmbParamTypeOrAccess());
        n = del(n, delFactory().delAmbReceiver());
        return n;
    }

    @Override
    public JoinLabelNode JoinLabelNode(Position pos,
            List<LabelComponentNode> components) {
        JoinLabelNode n = new JoinLabelNode_c(pos, components);
        n = ext(n, jifExtFactory().extJoinLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public MeetLabelNode MeetLabelNode(Position pos,
            List<LabelComponentNode> components) {
        MeetLabelNode n = new MeetLabelNode_c(pos, components);
        n = ext(n, jifExtFactory().extMeetLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public PolicyNode ReaderPolicyNode(Position pos, PrincipalNode owner,
            List<PrincipalNode> readers) {
        PolicyNode n = new ReaderPolicyNode_c(pos, owner, readers);
        n = ext(n, jifExtFactory().extPolicyNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public PolicyNode WriterPolicyNode(Position pos, PrincipalNode owner,
            List<PrincipalNode> writers) {
        PolicyNode n = new WriterPolicyNode_c(pos, owner, writers);
        n = ext(n, jifExtFactory().extPolicyNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public PolicyNode PolicyNode(Position pos, Policy policy) {
        PolicyNode n = new PolicyNode_c(pos, policy);
        n = ext(n, jifExtFactory().extPolicyNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, Expr expr) {
        AmbDynamicLabelNode n = new AmbDynamicLabelNode_c(pos, expr);
        n = ext(n, jifExtFactory().extAmbDynamicLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AmbVarLabelNode AmbVarLabelNode(Position pos, Id name) {
        AmbVarLabelNode n = new AmbVarLabelNode_c(pos, name);
        n = ext(n, jifExtFactory().extAmbVarLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AmbThisLabelNode AmbThisLabelNode(Position pos) {
        AmbThisLabelNode n = new AmbThisLabelNode_c(pos);
        n = ext(n, jifExtFactory().extAmbThisLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AmbProviderLabelNode AmbProviderLabelNode(Position pos,
            TypeNode typeNode) {
        AmbProviderLabelNode n = new AmbProviderLabelNode_c(pos, typeNode);
        n = ext(n, jifExtFactory().extAmbProviderLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public CanonicalLabelNode CanonicalLabelNode(Position pos, Label label) {
        CanonicalLabelNode n = new CanonicalLabelNode_c(pos, label);
        n = ext(n, jifExtFactory().extCanonicalLabelNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr) {
        AmbPrincipalNode n = new AmbPrincipalNode_c(pos, expr);
        n = ext(n, jifExtFactory().extAmbPrincipalNode());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbPrincipalNode(Position pos, Id name) {
        AmbPrincipalNode n = new AmbPrincipalNode_c(pos, name);
        n = ext(n, jifExtFactory().extAmbPrincipalNode());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbConjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right) {
        AmbPrincipalNode n =
                new AmbJunctivePrincipalNode_c(pos, left, right, true);
        n = ext(n, jifExtFactory().extAmbPrincipalNode());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbDisjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right) {
        AmbPrincipalNode n =
                new AmbJunctivePrincipalNode_c(pos, left, right, false);
        n = ext(n, jifExtFactory().extAmbPrincipalNode());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public CanonicalPrincipalNode CanonicalPrincipalNode(Position pos,
            Principal principal) {
        CanonicalPrincipalNode n = new CanonicalPrincipalNode_c(pos, principal);
        n = ext(n, jifExtFactory().extCanonicalPrincipalNode());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public ArrayAccessAssign ArrayAccessAssign(Position pos, ArrayAccess left,
            polyglot.ast.Assign.Operator op, Expr right) {
        ArrayAccessAssign n = new JifArrayAccessAssign_c(pos, left, op, right);
        n = ext(n, extFactory().extArrayAccessAssign());
        n = del(n, delFactory().delArrayAccessAssign());
        return n;
    }

    @Override
    public ClassDecl ClassDecl(Position pos, Flags flags, Id name,
            TypeNode superClass, List<TypeNode> interfaces, ClassBody body,
            Javadoc javadoc) {
        ClassDecl n = new JifClassDecl_c(pos, flags, name,
                Collections.<ParamDecl> emptyList(), superClass, interfaces,
                Collections.<PrincipalNode> emptyList(),
                Collections.<ConstraintNode<Assertion>> emptyList(), body,
                javadoc);
        n = ext(n, extFactory().extClassDecl());
        n = del(n, delFactory().delClassDecl());
        return n;
    }

    @Override
    public LocalDecl LocalDecl(Position pos, Flags flags, TypeNode type,
            Id name, Expr init) {
        LocalDecl n = new JifLocalDecl_c(pos, flags, type, name, init);
        n = ext(n, extFactory().extLocalDecl());
        n = del(n, delFactory().delLocalDecl());
        return n;
    }

    @Override
    public JifClassDecl JifClassDecl(Position pos, Flags flags, Id name,
            List<ParamDecl> params, TypeNode superClass,
            List<TypeNode> interfaces, List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body,
            Javadoc javadoc) {
        JifClassDecl n = new JifClassDecl_c(pos, flags, name, params,
                superClass, interfaces, authority, constraints, body, javadoc);
        n = ext(n, extFactory().extClassDecl());
        n = del(n, delFactory().delClassDecl());
        return n;
    }

    @Override
    public MethodDecl MethodDecl(Position pos, Flags flags, TypeNode returnType,
            Id name, List<Formal> formals, List<TypeNode> throwTypes,
            Block body, Javadoc javadoc) {
        MethodDecl n = new JifMethodDecl_c(pos, flags, returnType, name, null,
                formals, null, throwTypes,
                Collections.<ConstraintNode<Assertion>> emptyList(), body,
                javadoc);
        n = ext(n, extFactory().extMethodDecl());
        n = del(n, delFactory().delMethodDecl());
        return n;
    }

    @Override
    public JifMethodDecl JifMethodDecl(Position pos, Flags flags,
            TypeNode returnType, Id name, LabelNode startLabel,
            List<Formal> formals, LabelNode endLabel, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc) {
        JifMethodDecl n = new JifMethodDecl_c(pos, flags, returnType, name,
                startLabel, formals, endLabel, throwTypes, constraints, body,
                javadoc);

        n = ext(n, extFactory().extMethodDecl());
        n = del(n, delFactory().delMethodDecl());
        return n;
    }

    @Override
    public ConstructorDecl ConstructorDecl(Position pos, Flags flags, Id name,
            List<Formal> formals, List<TypeNode> throwTypes, Block body,
            Javadoc javadoc) {
        ConstructorDecl n = new JifConstructorDecl_c(pos, flags, name, null,
                null, formals, throwTypes,
                Collections.<ConstraintNode<Assertion>> emptyList(), body,
                javadoc);
        n = ext(n, extFactory().extConstructorDecl());
        n = del(n, delFactory().delConstructorDecl());
        return n;
    }

    @Override
    public JifConstructorDecl JifConstructorDecl(Position pos, Flags flags,
            Id name, LabelNode startLabel, LabelNode returnLabel,
            List<Formal> formals, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body,
            Javadoc javadoc) {
        JifConstructorDecl n = new JifConstructorDecl_c(pos, flags, name,
                startLabel, returnLabel, formals, throwTypes, constraints, body,
                javadoc);
        n = ext(n, extFactory().extConstructorDecl());
        n = del(n, delFactory().delConstructorDecl());
        return n;
    }

    @Override
    public New New(Position pos, Expr outer, TypeNode objectType,
            List<Expr> args, ClassBody body) {
        New n = new JifNew_c(pos, outer, objectType, args, body);
        n = ext(n, extFactory().extNew());
        n = del(n, delFactory().delNew());
        return n;
    }

    @Override
    public AmbParam AmbParam(Position pos, Id name) {
        return AmbParam(pos, name, null);
    }

    @Override
    public AmbParam AmbParam(Position pos, Id name, ParamInstance pi) {
        AmbParam n = new AmbParam_c(pos, name, pi);
        n = ext(n, jifExtFactory().extAmbParam());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AmbExprParam AmbParam(Position pos, Expr expr,
            ParamInstance expectedPI) {
        AmbExprParam n = new AmbExprParam_c(pos, expr, expectedPI);
        n = ext(n, jifExtFactory().extAmbParam());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, Id name) {
        ParamDecl n = new ParamDecl_c(pos, kind, name);
        n = ext(n, jifExtFactory().extParamDecl());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public CanonicalConstraintNode CanonicalConstraintNode(Position pos,
            Assertion constraint) {
        if (!constraint.isCanonical()) {
            throw new InternalCompilerError(constraint + " is not canonical.");
        }
        CanonicalConstraintNode n =
                new CanonicalConstraintNode_c(pos, constraint);
        n = ext(n, jifExtFactory().extCanonicalConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AuthConstraintNode AuthConstraintNode(Position pos,
            List<PrincipalNode> principals) {
        AuthConstraintNode n = new AuthConstraintNode_c(pos, principals);
        n = ext(n, jifExtFactory().extAuthConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public AutoEndorseConstraintNode AutoEndorseConstraintNode(Position pos,
            LabelNode endorseTo) {
        AutoEndorseConstraintNode n =
                new AutoEndorseConstraintNode_c(pos, endorseTo);
        n = ext(n, jifExtFactory().extAutoEndorseConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public CallerConstraintNode CallerConstraintNode(Position pos,
            List<PrincipalNode> principals) {
        CallerConstraintNode n = new CallerConstraintNode_c(pos, principals);
        n = ext(n, jifExtFactory().extCallerConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(
            Position pos, PrincipalNode actor, PrincipalNode granter) {
        return PrincipalActsForPrincipalConstraintNode(pos, actor, granter,
                false);
    }

    @Override
    public PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(
            Position pos, PrincipalNode actor, PrincipalNode granter,
            boolean isEquiv) {
        PrincipalActsForPrincipalConstraintNode n =
                new PrincipalActsForPrincipalConstraintNode_c(pos, actor,
                        granter, isEquiv);
        n = ext(n,
                jifExtFactory().extPrincipalActsForPrincipalConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public LabelActsForPrincipalConstraintNode LabelActsForPrincipalConstraintNode(
            Position pos, LabelNode actor, PrincipalNode granter) {
        LabelActsForPrincipalConstraintNode n =
                new LabelActsForPrincipalConstraintNode_c(pos, actor, granter);
        n = ext(n, jifExtFactory().extLabelActsForPrincipalConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public LabelActsForLabelConstraintNode LabelActsForLabelConstraintNode(
            Position pos, LabelNode actor, LabelNode granter) {
        LabelActsForLabelConstraintNode n =
                new LabelActsForLabelConstraintNode_c(pos, actor, granter);
        n = ext(n, jifExtFactory().extLabelActsForPrincipalConstraintNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public LabelLeAssertionNode LabelLeAssertionNode(Position pos,
            LabelNode lhs, LabelNode rhs, boolean isEquiv) {
        LabelLeAssertionNode n =
                new LabelLeAssertionNode_c(pos, lhs, rhs, isEquiv);
        n = ext(n, jifExtFactory().extLabelLeAssertionNode());
        n = del(n, delFactory().delNode());
        return n;
    }

    @Override
    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound,
            LabelNode label, Stmt body) {
        DeclassifyStmt n = new DeclassifyStmt_c(pos, bound, label, body);
        n = ext(n, jifExtFactory().extDeclassifyStmt());
        n = del(n, delFactory().delStmt());
        return n;
    }

    @Override
    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode label,
            Stmt body) {
        DeclassifyStmt n = new DeclassifyStmt_c(pos, null, label, body);
        n = ext(n, jifExtFactory().extDeclassifyStmt());
        n = del(n, delFactory().delStmt());
        return n;
    }

    @Override
    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr,
            LabelNode bound, LabelNode label) {
        DeclassifyExpr n = new DeclassifyExpr_c(pos, expr, bound, label);
        n = ext(n, jifExtFactory().extDeclassifyExpr());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr,
            LabelNode label) {
        DeclassifyExpr n = new DeclassifyExpr_c(pos, expr, null, label);
        n = ext(n, jifExtFactory().extDeclassifyExpr());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public EndorseStmt EndorseStmt(Position pos, LabelNode bound,
            LabelNode label, Stmt body) {
        EndorseStmt n = new EndorseStmt_c(pos, bound, label, body);
        n = ext(n, jifExtFactory().extEndorseStmt());
        n = del(n, delFactory().delStmt());
        return n;
    }

    @Override
    public EndorseStmt EndorseStmt(Position pos, LabelNode label, Stmt body) {
        EndorseStmt n = new EndorseStmt_c(pos, null, label, body);
        n = ext(n, jifExtFactory().extEndorseStmt());
        n = del(n, delFactory().delStmt());
        return n;
    }

    @Override
    public CheckedEndorseStmt CheckedEndorseStmt(Position pos, Expr e,
            LabelNode bound, LabelNode label, If body) {
        CheckedEndorseStmt n =
                new CheckedEndorseStmt_c(pos, e, bound, label, body);
        n = ext(n, jifExtFactory().extCheckedEndorseStmt());
        n = del(n, delFactory().delStmt());
        return n;
    }

    @Override
    public EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode label) {
        EndorseExpr n = new EndorseExpr_c(pos, expr, null, label);
        n = ext(n, jifExtFactory().extEndorseExpr());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode bound,
            LabelNode label) {
        EndorseExpr n = new EndorseExpr_c(pos, expr, bound, label);
        n = ext(n, jifExtFactory().extEndorseExpr());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public LabelExpr LabelExpr(Position pos, Label l) {
        LabelNode ln = CanonicalLabelNode(pos, l);
        return LabelExpr(pos, ln);
    }

    public LabelExpr LabelExpr(Position pos, LabelNode node) {
        LabelExpr n = new LabelExpr_c(pos, node);
        n = ext(n, jifExtFactory().extLabelExpr());
        n = del(n, ((JifDelFactory) delFactory()).delLabelExpr());
        return n;
    }

    @Override
    public NewLabel NewLabel(Position pos, LabelNode label) {
        NewLabel n = new NewLabel_c(pos, label);
        n = ext(n, jifExtFactory().extNewLabel());
        n = del(n, ((JifDelFactory) delFactory()).delNewLabel());
        return n;
    }

    @Override
    public PrincipalExpr PrincipalExpr(Position pos, PrincipalNode principal) {
        PrincipalExpr n = new PrincipalExpr_c(pos, principal);
        n = ext(n, jifExtFactory().extPrincipalExpr());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public Call Call(Position pos, Receiver target, Id name, List<Expr> args) {
        Call n = new JifCall_c(pos, target, name, args);
        n = ext(n, extFactory().extCall());
        n = del(n, delFactory().delCall());
        return n;
    }

    @Override
    public Catch Catch(Position pos, Formal formal, Block body) {
        Catch n = new JifCatch_c(pos, formal, body);
        n = ext(n, extFactory().extCatch());
        n = del(n, delFactory().delCatch());
        return n;
    }

    @Override
    public Formal Formal(Position pos, Flags flags, TypeNode type, Id name) {
        Formal n = new JifFormal_c(pos, flags, type, name);
        n = ext(n, extFactory().extFormal());
        n = del(n, delFactory().delFormal());
        return n;
    }

    @Override
    public Binary Binary(Position pos, Expr left, Operator op, Expr right) {
        Binary n = new JifBinary_c(pos, left, op, right);
        n = ext(n, extFactory().extBinary());
        n = del(n, delFactory().delBinary());
        return n;
    }

    @Override
    public TypeNode ConstArrayTypeNode(Position pos, TypeNode base) {
        return new ConstArrayTypeNode_c(pos, base);
    }

    @Override
    public Prologue Prologue(Position pos, List<Stmt> stmts) {
        Prologue n = new Prologue_c(pos, stmts);
        n = ext(n, extFactory().extBlock());
        n = del(n, delFactory().delBlock());
        return n;
    }
}
