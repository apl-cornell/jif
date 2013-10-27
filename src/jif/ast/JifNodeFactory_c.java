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
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.DelFactory;
import polyglot.ast.Disamb;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.If;
import polyglot.ast.LocalClassDecl;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.NodeFactory_c;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifNodeFactory</code> interface.
 */
public class JifNodeFactory_c extends NodeFactory_c implements JifNodeFactory
{
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
        return (JifExtFactory)this.extFactory();
    }


    @Override
    public Disamb disamb() {
        return new JifDisamb_c();
    }

    @Override
    public CanonicalTypeNode CanonicalTypeNode(Position pos, Type type) {
        CanonicalTypeNode n = new JifCanonicalTypeNode_c(pos, type);
        n = (CanonicalTypeNode)n.ext(extFactory().extCanonicalTypeNode());
        n = (CanonicalTypeNode)n.del(delFactory().delCanonicalTypeNode());
        return n;
    }
    @Override
    public InstTypeNode InstTypeNode(Position pos, TypeNode type, List<ParamNode> params) {
        InstTypeNode n = new InstTypeNode_c(pos, type, params);
        n = (InstTypeNode)n.ext(jifExtFactory().extInstTypeNode());
        n = (InstTypeNode)n.del(delFactory().delTypeNode());
        return n;
    }

    @Override
    public LabeledTypeNode LabeledTypeNode(Position pos, TypeNode type, LabelNode label) {
        LabeledTypeNode n = new LabeledTypeNode_c(pos, type, label);
        n = (LabeledTypeNode)n.ext(jifExtFactory().extLabeledTypeNode());
        n = (LabeledTypeNode)n.del(delFactory().delTypeNode());
        return n;
    }

    @Override
    public AmbNewArray AmbNewArray(Position pos, TypeNode baseType, Object expr, List<Expr> dims, int addDims) {
        AmbNewArray n = new AmbNewArray_c(pos, baseType, expr, dims, addDims);
        n = (AmbNewArray)n.ext(jifExtFactory().extAmbNewArray());
        n = (AmbNewArray)n.del(delFactory().delAmbExpr());
        return n;
    }

    @Override
    public AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos, Receiver base, Object expr) {
        AmbParamTypeOrAccess n = new AmbParamTypeOrAccess_c(pos, base,
                expr);
        n = (AmbParamTypeOrAccess)n.ext(jifExtFactory().extAmbParamTypeOrAccess());
        n = (AmbParamTypeOrAccess)n.del(delFactory().delAmbReceiver());
        return n;
    }

    @Override
    public JoinLabelNode JoinLabelNode(Position pos,
            List<LabelComponentNode> components) {
        JoinLabelNode n = new JoinLabelNode_c(pos, components);
        n = (JoinLabelNode)n.ext(jifExtFactory().extJoinLabelNode());
        n = (JoinLabelNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public MeetLabelNode MeetLabelNode(Position pos, List<LabelComponentNode> components) {
        MeetLabelNode n = new MeetLabelNode_c(pos, components);
        n = (MeetLabelNode)n.ext(jifExtFactory().extMeetLabelNode());
        n = (MeetLabelNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public PolicyNode ReaderPolicyNode(Position pos, PrincipalNode owner, List<PrincipalNode> readers) {
        PolicyNode n = new ReaderPolicyNode_c(pos, owner,
                readers);
        n = (PolicyNode)n.ext(jifExtFactory().extPolicyNode());
        n = (PolicyNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public PolicyNode WriterPolicyNode(Position pos, PrincipalNode owner, List<PrincipalNode> writers) {
        PolicyNode n = new WriterPolicyNode_c(pos, owner,
                writers);
        n = (PolicyNode)n.ext(jifExtFactory().extPolicyNode());
        n = (PolicyNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public PolicyNode PolicyNode(Position pos, Policy policy) {
        PolicyNode n = new PolicyNode_c(pos, policy);
        n = (PolicyNode)n.ext(jifExtFactory().extPolicyNode());
        n = (PolicyNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, Expr expr) {
        AmbDynamicLabelNode n = new AmbDynamicLabelNode_c(pos, expr);
        n = (AmbDynamicLabelNode)n.ext(jifExtFactory().extAmbDynamicLabelNode());
        n = (AmbDynamicLabelNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AmbVarLabelNode AmbVarLabelNode(Position pos, Id name) {
        AmbVarLabelNode n = new AmbVarLabelNode_c(pos, name);
        n = (AmbVarLabelNode)n.ext(jifExtFactory().extAmbVarLabelNode());
        n = (AmbVarLabelNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AmbThisLabelNode AmbThisLabelNode(Position pos) {
        AmbThisLabelNode n = new AmbThisLabelNode_c(pos);
        n = (AmbThisLabelNode)n.ext(jifExtFactory().extAmbThisLabelNode());
        n = (AmbThisLabelNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AmbProviderLabelNode AmbProviderLabelNode(Position pos,
            TypeNode typeNode) {
        AmbProviderLabelNode n = new AmbProviderLabelNode_c(pos, typeNode);
        n = (AmbProviderLabelNode) n.ext(jifExtFactory().extAmbProviderLabelNode());
        n = (AmbProviderLabelNode) n.del(delFactory().delNode());
        return n;
    }
    @Override
    public CanonicalLabelNode CanonicalLabelNode(Position pos, Label label) {
        CanonicalLabelNode n = new CanonicalLabelNode_c(pos, label);
        n = (CanonicalLabelNode)n.ext(jifExtFactory().extCanonicalLabelNode());
        n = (CanonicalLabelNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr) {
        AmbPrincipalNode n = new AmbPrincipalNode_c(pos, expr);
        n = (AmbPrincipalNode)n.ext(jifExtFactory().extAmbPrincipalNode());
        n = (AmbPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbPrincipalNode(Position pos, Id name) {
        AmbPrincipalNode n = new AmbPrincipalNode_c(pos, name);
        n = (AmbPrincipalNode)n.ext(jifExtFactory().extAmbPrincipalNode());
        n = (AmbPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbConjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right) {
        AmbPrincipalNode n = new AmbJunctivePrincipalNode_c(pos, left, right, true);
        n = (AmbPrincipalNode)n.ext(jifExtFactory().extAmbPrincipalNode());
        n = (AmbPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public AmbPrincipalNode AmbDisjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right) {
        AmbPrincipalNode n = new AmbJunctivePrincipalNode_c(pos, left, right, false);
        n = (AmbPrincipalNode)n.ext(jifExtFactory().extAmbPrincipalNode());
        n = (AmbPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public CanonicalPrincipalNode CanonicalPrincipalNode(Position pos, Principal principal) {
        CanonicalPrincipalNode n = new CanonicalPrincipalNode_c(pos,
                principal);
        n = (CanonicalPrincipalNode)n.ext(jifExtFactory().extCanonicalPrincipalNode());
        n = (CanonicalPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public ArrayAccessAssign ArrayAccessAssign(Position pos, ArrayAccess left, polyglot.ast.Assign.Operator op, Expr right) {
        ArrayAccessAssign n = new JifArrayAccessAssign_c(pos, left, op, right);
        n = (ArrayAccessAssign)n.ext(extFactory().extArrayAccessAssign());
        n = (ArrayAccessAssign)n.del(delFactory().delArrayAccessAssign());
        return n;
    }

    @Override
    public ClassDecl ClassDecl(Position pos, Flags flags, Id name,
            TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
        ClassDecl n = new JifClassDecl_c(pos, flags, name,
                Collections.<ParamDecl> emptyList(), superClass,
                interfaces, Collections.<PrincipalNode> emptyList(),
                Collections.<ConstraintNode<Assertion>> emptyList(),
                body);
        n = (ClassDecl)n.ext(extFactory().extClassDecl());
        n = (ClassDecl)n.del(delFactory().delClassDecl());
        return n;
    }

    @Override
    public LocalClassDecl LocalClassDecl(Position pos, ClassDecl decl) {
        throw new InternalCompilerError("Jif does not support inner classes.");
    }

    @Override
    public LocalDecl LocalDecl(Position pos, Flags flags, TypeNode type,
            Id name, Expr init) {
        LocalDecl n = new JifLocalDecl_c(pos, flags, type, name, init);
        n = (LocalDecl)n.ext(extFactory().extLocalDecl());
        n = (LocalDecl)n.del(delFactory().delLocalDecl());
        return n;
    }
    @Override
    public JifClassDecl JifClassDecl(
            Position pos,
            Flags flags,
            Id name,
            List<ParamDecl> params,
            TypeNode superClass,
            List<TypeNode> interfaces,
            List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints,
            ClassBody body) {
        JifClassDecl n = new JifClassDecl_c(pos, flags, name,
                params, superClass, interfaces, authority, constraints, body);
        n = (JifClassDecl)n.ext(extFactory().extClassDecl());
        n = (JifClassDecl)n.del(delFactory().delClassDecl());
        return n;
    }

    @Override
    public MethodDecl MethodDecl(Position pos, Flags flags,
            TypeNode returnType, Id name, List<Formal> formals,
            List<TypeNode> throwTypes, Block body) {
        MethodDecl n =
                new JifMethodDecl_c(pos, flags, returnType, name, null,
                        formals, null, throwTypes,
                        Collections.<ConstraintNode<Assertion>> emptyList(),
                        body);
        n = (MethodDecl)n.ext(extFactory().extMethodDecl());
        n = (MethodDecl)n.del(delFactory().delMethodDecl());
        return n;
    }

    @Override
    public JifMethodDecl JifMethodDecl(Position pos, Flags flags,
            TypeNode returnType, Id name, LabelNode startLabel,
            List<Formal> formals, LabelNode endLabel, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body) {
//      //add default return value label node
//if (! (returnType instanceof LabeledTypeNode) &&
//      (! (returnType instanceof CanonicalTypeNode) ||
//      !returnType.type().isVoid()) ) {
//      List comps = new LinkedList();
//      if (endLabel != null)
//      if (endLabel instanceof JoinLabelNode)
//      comps = ListUtil.copy(
//      ((JoinLabelNode)endLabel).components(), false);
//      else
//      comps.add(endLabel.copy());

//      for (Iterator iter = formals.iterator(); iter.hasNext(); ) {
//      Formal arg = (Formal) iter.next();
//      comps.add(AmbVarLabelNode(arg.position(), arg.name()));
//      }
//      LabelNode LrvNode = JoinLabelNode(returnType.position(), comps);
//      returnType = LabeledTypeNode(returnType.position(), returnType,
//      LrvNode);
//      }

        JifMethodDecl n = new JifMethodDecl_c(pos, flags,
                returnType, name, startLabel, formals, endLabel,
                throwTypes, constraints, body);

        n = (JifMethodDecl)n.ext(extFactory().extMethodDecl());
        n = (JifMethodDecl)n.del(delFactory().delMethodDecl());
        return n;
    }

    @Override
    public ConstructorCall ConstructorCall(Position pos,
            ConstructorCall.Kind kind, Expr outer, List<Expr> args) {
        if (outer != null) throw new InternalCompilerError("Jif does not support inner classes.");
        return super.ConstructorCall(pos, kind, outer, args);
    }

    @Override
    public ConstructorDecl ConstructorDecl(Position pos, Flags flags, Id name,
            List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        ConstructorDecl n =
                new JifConstructorDecl_c(pos, flags, name, null, null, formals,
                        throwTypes,
                        Collections.<ConstraintNode<Assertion>> emptyList(),
                        body);
        n = (ConstructorDecl)n.ext(extFactory().extConstructorDecl());
        n = (ConstructorDecl)n.del(delFactory().delConstructorDecl());
        return n;
    }

    @Override
    public JifConstructorDecl JifConstructorDecl(Position pos, Flags flags,
            Id name, LabelNode startLabel, LabelNode returnLabel,
            List<Formal> formals, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body) {
        JifConstructorDecl n = new JifConstructorDecl_c(pos, flags,
                name, startLabel, returnLabel, formals, throwTypes,
                constraints, body);
        n = (JifConstructorDecl)n.ext(extFactory().extConstructorDecl());
        n = (JifConstructorDecl)n.del(delFactory().delConstructorDecl());
        return n;
    }

    @Override
    public New New(Position pos, Expr outer, TypeNode objectType,
            List<Expr> args, ClassBody body) {
        if (body != null)
            throw new InternalCompilerError("Jif does not support inner classes.");
        if (outer != null)
            throw new InternalCompilerError("Jif does not support inner classes.");
        New n = new JifNew_c(pos, objectType, args, body);
        n = (New)n.ext(extFactory().extNew());
        n = (New)n.del(delFactory().delNew());
        return n;
    }

    @Override
    public Special Special(Position pos, Special.Kind kind, TypeNode outer) {
        if (outer != null) throw new InternalCompilerError("Jif does not support inner classes.");
        return super.Special(pos, kind, outer);
    }

    @Override
    public AmbParam AmbParam(Position pos, Id name) {
        return AmbParam(pos, name, null);
    }
    @Override
    public AmbParam AmbParam(Position pos, Id name, ParamInstance pi) {
        AmbParam n = new AmbParam_c(pos, name, pi);
        n = (AmbParam)n.ext(jifExtFactory().extAmbParam());
        n = (AmbParam)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AmbExprParam AmbParam(Position pos, Expr expr, ParamInstance expectedPI) {
        AmbExprParam n = new AmbExprParam_c(pos, expr, expectedPI);
        n = (AmbExprParam)n.ext(jifExtFactory().extAmbParam());
        n = (AmbExprParam)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, Id name) {
        ParamDecl n = new ParamDecl_c(pos, kind, name);
        n = (ParamDecl)n.ext(jifExtFactory().extParamDecl());
        n = (ParamDecl)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public CanonicalConstraintNode CanonicalConstraintNode(Position pos, Assertion constraint) {
        if (! constraint.isCanonical()) {
            throw new InternalCompilerError(constraint + " is not canonical.");
        }
        CanonicalConstraintNode n = new CanonicalConstraintNode_c(pos,
                constraint);
        n = (CanonicalConstraintNode)n.ext(jifExtFactory().extCanonicalConstraintNode());
        n = (CanonicalConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AuthConstraintNode AuthConstraintNode(Position pos, List<PrincipalNode> principals) {
        AuthConstraintNode n = new AuthConstraintNode_c(pos,
                principals);
        n = (AuthConstraintNode)n.ext(jifExtFactory().extAuthConstraintNode());
        n = (AuthConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public AutoEndorseConstraintNode AutoEndorseConstraintNode(Position pos, LabelNode endorseTo) {
        AutoEndorseConstraintNode n = new AutoEndorseConstraintNode_c(pos,
                endorseTo);
        n = (AutoEndorseConstraintNode)n.ext(jifExtFactory().extAutoEndorseConstraintNode());
        n = (AutoEndorseConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public CallerConstraintNode CallerConstraintNode(Position pos, List<PrincipalNode> principals) {
        CallerConstraintNode n = new CallerConstraintNode_c(pos,
                principals);
        n = (CallerConstraintNode)n.ext(jifExtFactory().extCallerConstraintNode());
        n = (CallerConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(Position pos, PrincipalNode actor, PrincipalNode granter) {
        return PrincipalActsForPrincipalConstraintNode(pos, actor, granter, false);
    }

    @Override
    public PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(Position pos, PrincipalNode actor, PrincipalNode granter, boolean isEquiv) {
        PrincipalActsForPrincipalConstraintNode n = new PrincipalActsForPrincipalConstraintNode_c(pos,
                actor, granter, isEquiv);
        n = (PrincipalActsForPrincipalConstraintNode)n.ext(jifExtFactory().extPrincipalActsForPrincipalConstraintNode());
        n = (PrincipalActsForPrincipalConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public LabelActsForPrincipalConstraintNode LabelActsForPrincipalConstraintNode(
            Position pos, LabelNode actor, PrincipalNode granter) {
        LabelActsForPrincipalConstraintNode n =
                new LabelActsForPrincipalConstraintNode_c(pos, actor, granter);
        n =
                (LabelActsForPrincipalConstraintNode) n.ext(jifExtFactory()
                        .extLabelActsForPrincipalConstraintNode());
        n = (LabelActsForPrincipalConstraintNode) n.del(delFactory().delNode());
        return n;
    }

    @Override
    public LabelActsForLabelConstraintNode LabelActsForLabelConstraintNode(
            Position pos, LabelNode actor, LabelNode granter) {
        LabelActsForLabelConstraintNode n =
                new LabelActsForLabelConstraintNode_c(pos, actor, granter);
        n =
                (LabelActsForLabelConstraintNode) n.ext(jifExtFactory()
                        .extLabelActsForPrincipalConstraintNode());
        n = (LabelActsForLabelConstraintNode) n.del(delFactory().delNode());
        return n;
    }

    @Override
    public LabelLeAssertionNode LabelLeAssertionNode(Position pos, LabelNode lhs, LabelNode rhs, boolean isEquiv) {
        LabelLeAssertionNode n = new LabelLeAssertionNode_c(pos,
                lhs, rhs, isEquiv);
        n = (LabelLeAssertionNode)n.ext(jifExtFactory().extLabelLeAssertionNode());
        n = (LabelLeAssertionNode)n.del(delFactory().delNode());
        return n;
    }

    @Override
    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound, LabelNode label, Stmt body) {
        DeclassifyStmt n = new DeclassifyStmt_c(pos, bound, label,
                body);
        n = (DeclassifyStmt)n.ext(jifExtFactory().extDeclassifyStmt());
        n = (DeclassifyStmt)n.del(delFactory().delStmt());
        return n;
    }

    @Override
    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode label, Stmt body) {
        DeclassifyStmt n = new DeclassifyStmt_c(pos, null, label, body);
        n = (DeclassifyStmt)n.ext(jifExtFactory().extDeclassifyStmt());
        n = (DeclassifyStmt)n.del(delFactory().delStmt());
        return n;
    }

    @Override
    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode bound,
            LabelNode label)
    {
        DeclassifyExpr n = new DeclassifyExpr_c(pos, expr, bound, label);
        n = (DeclassifyExpr)n.ext(jifExtFactory().extDeclassifyExpr());
        n = (DeclassifyExpr)n.del(delFactory().delExpr());
        return n;
    }
    @Override
    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode label)
    {
        DeclassifyExpr n = new DeclassifyExpr_c(pos, expr, null, label);
        n = (DeclassifyExpr)n.ext(jifExtFactory().extDeclassifyExpr());
        n = (DeclassifyExpr)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public EndorseStmt EndorseStmt(Position pos, LabelNode bound, LabelNode label, Stmt body) {
        EndorseStmt n = new EndorseStmt_c(pos, bound, label, body);
        n = (EndorseStmt)n.ext(jifExtFactory().extEndorseStmt());
        n = (EndorseStmt)n.del(delFactory().delStmt());
        return n;
    }

    @Override
    public EndorseStmt EndorseStmt(Position pos, LabelNode label, Stmt body) {
        EndorseStmt n = new EndorseStmt_c(pos, null, label, body);
        n = (EndorseStmt)n.ext(jifExtFactory().extEndorseStmt());
        n = (EndorseStmt)n.del(delFactory().delStmt());
        return n;
    }

    @Override
    public CheckedEndorseStmt CheckedEndorseStmt(Position pos, Expr e, LabelNode bound, LabelNode label, If body) {
        CheckedEndorseStmt n = new CheckedEndorseStmt_c(pos, e, bound, label, body);
        n = (CheckedEndorseStmt)n.ext(jifExtFactory().extCheckedEndorseStmt());
        n = (CheckedEndorseStmt)n.del(delFactory().delStmt());
        return n;
    }


    @Override
    public EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode label)
    {
        EndorseExpr n = new EndorseExpr_c(pos, expr, null, label);
        n = (EndorseExpr)n.ext(jifExtFactory().extEndorseExpr());
        n = (EndorseExpr)n.del(delFactory().delExpr());
        return n;
    }
    @Override
    public EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode bound,
            LabelNode label)
    {
        EndorseExpr n = new EndorseExpr_c(pos, expr, bound,
                label);
        n = (EndorseExpr)n.ext(jifExtFactory().extEndorseExpr());
        n = (EndorseExpr)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public LabelExpr LabelExpr(Position pos, Label l) {
        LabelNode ln = CanonicalLabelNode(pos, l);
        return LabelExpr(pos, ln);
    }

    public LabelExpr LabelExpr(Position pos, LabelNode node) {
        LabelExpr n = new LabelExpr_c(pos, node);
        n = (LabelExpr)n.ext(jifExtFactory().extLabelExpr());
        n = (LabelExpr)n.del(((JifDelFactory)delFactory()).delLabelExpr());
        return n;
    }

    @Override
    public NewLabel NewLabel(Position pos, LabelNode label) {
        NewLabel n = new NewLabel_c(pos, label);
        n = (NewLabel)n.ext(jifExtFactory().extNewLabel());
        n = (NewLabel)n.del(((JifDelFactory)delFactory()).delNewLabel());
        return n;
    }
    @Override
    public PrincipalExpr PrincipalExpr(Position pos, PrincipalNode principal) {
        PrincipalExpr n = new PrincipalExpr_c(pos, principal);
        n = (PrincipalExpr)n.ext(jifExtFactory().extPrincipalExpr());
        n = (PrincipalExpr)n.del(delFactory().delExpr());
        return n;
    }

    @Override
    public Call Call(Position pos, Receiver target, Id name, List<Expr> args) {
        Call n = new JifCall_c(pos, target, name, args);
        n = (Call)n.ext(extFactory().extCall());
        n = (Call)n.del(delFactory().delCall());
        return n;
    }

    @Override
    public Catch Catch(Position pos, Formal formal, Block body) {
        Catch n = new JifCatch_c(pos, formal, body);
        n = (Catch)n.ext(extFactory().extCatch());
        n = (Catch)n.del(delFactory().delCatch());
        return n;
    }

    @Override
    public Formal Formal(Position pos, Flags flags, TypeNode type, Id name) {
        Formal n = new JifFormal_c(pos, flags, type, name);
        n = (Formal)n.ext(extFactory().extFormal());
        n = (Formal)n.del(delFactory().delFormal());
        return n;
    }

    @Override
    public Binary Binary(Position pos, Expr left, Operator op, Expr right) {
        Binary n = new JifBinary_c(pos, left, op, right);
        n = (Binary)n.ext(extFactory().extBinary());
        n = (Binary)n.del(delFactory().delBinary());
        return n;
    }
    @Override
    public TypeNode ConstArrayTypeNode(Position pos, TypeNode base) {
        return new ConstArrayTypeNode_c(pos, base);
    }
    @Override
    public JifSingletonDecl JifSingletonDecl(
            Position pos,
            Flags flags,
            Id name,
            List<ParamDecl> params,
            TypeNode superClass,
            List<TypeNode> interfaces,
            List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints,
            ClassBody body) {
        return new JifSingletonDecl_c(pos, flags, name, params,
                superClass, interfaces, authority, constraints, body);
    }
    @Override
    public JifSingletonAccess JifSingletonAccess(Position pos, TypeNode objectType, List<Expr> args) {
        return new JifSingletonAccess_c(pos, objectType, args);
    }
}
