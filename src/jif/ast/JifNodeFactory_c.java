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
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Catch;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.DelFactory;
import polyglot.ast.Disamb;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.ExtFactory;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.If;
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
        CanonicalTypeNode n =
                JifCanonicalTypeNode(pos, type, null, extFactory());
        n = del(n, delFactory().delCanonicalTypeNode());
        return n;
    }

    protected JifCanonicalTypeNode JifCanonicalTypeNode(Position pos,
            Type type, Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory())
            ext = composeExts(ext, ef.extCanonicalTypeNode());
        return new JifCanonicalTypeNode_c(pos, type, ext);
    }

    @Override
    public InstTypeNode InstTypeNode(Position pos, TypeNode type,
            List<ParamNode> params) {
        InstTypeNode n = InstTypeNode(pos, type, params, null, extFactory());
        n = del(n, delFactory().delTypeNode());
        return n;
    }

    protected final InstTypeNode InstTypeNode(Position pos, TypeNode type,
            List<ParamNode> params, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extInstTypeNode());
        return new InstTypeNode_c(pos, type, params, ext);
    }

    @Override
    public LabeledTypeNode LabeledTypeNode(Position pos, TypeNode type,
            LabelNode label) {
        LabeledTypeNode n =
                LabeledTypeNode(pos, type, label, null, extFactory());
        n = del(n, delFactory().delTypeNode());
        return n;
    }

    protected final LabeledTypeNode LabeledTypeNode(Position pos,
            TypeNode type, LabelNode label, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extLabeledTypeNode());
        return new LabeledTypeNode_c(pos, type, label, ext);
    }

    @Override
    public AmbNewArray AmbNewArray(Position pos, TypeNode baseType,
            Object expr, List<Expr> dims, int addDims) {
        AmbNewArray n =
                AmbNewArray(pos, baseType, expr, dims, addDims, null,
                        extFactory());
        n = del(n, delFactory().delAmbExpr());
        return n;
    }

    protected final AmbNewArray AmbNewArray(Position pos, TypeNode baseType,
            Object expr, List<Expr> dims, int addDims, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extAmbNewArray());
        return new AmbNewArray_c(pos, baseType, expr, dims, addDims, ext);
    }

    @Override
    public AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos,
            Receiver base, Object expr) {
        AmbParamTypeOrAccess n =
                AmbParamTypeOrAccess(pos, base, expr, null, extFactory());
        n = del(n, delFactory().delAmbReceiver());
        return n;
    }

    protected final AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos,
            Receiver base, Object expr, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbParamTypeOrAccess());
        return new AmbParamTypeOrAccess_c(pos, base, expr, ext);
    }

    @Override
    public JoinLabelNode JoinLabelNode(Position pos,
            List<LabelComponentNode> components) {
        JoinLabelNode n = JoinLabelNode(pos, components, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final JoinLabelNode JoinLabelNode(Position pos,
            List<LabelComponentNode> components, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extJoinLabelNode());
        return new JoinLabelNode_c(pos, components, ext);
    }

    @Override
    public MeetLabelNode MeetLabelNode(Position pos,
            List<LabelComponentNode> components) {
        MeetLabelNode n = MeetLabelNode(pos, components, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final MeetLabelNode MeetLabelNode(Position pos,
            List<LabelComponentNode> components, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extMeetLabelNode());
        return new MeetLabelNode_c(pos, components, ext);
    }

    @Override
    public PolicyNode ReaderPolicyNode(Position pos, PrincipalNode owner,
            List<PrincipalNode> readers) {
        PolicyNode n =
                ReaderPolicyNode(pos, owner, readers, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final PolicyNode ReaderPolicyNode(Position pos,
            PrincipalNode owner, List<PrincipalNode> readers, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extPolicyNode());
        return new ReaderPolicyNode_c(pos, owner, readers, ext);
    }

    @Override
    public PolicyNode WriterPolicyNode(Position pos, PrincipalNode owner,
            List<PrincipalNode> writers) {
        PolicyNode n =
                WriterPolicyNode(pos, owner, writers, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final PolicyNode WriterPolicyNode(Position pos,
            PrincipalNode owner, List<PrincipalNode> writers, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extPolicyNode());
        return new WriterPolicyNode_c(pos, owner, writers, ext);
    }

    @Override
    public PolicyNode PolicyNode(Position pos, Policy policy) {
        PolicyNode n = PolicyNode(pos, policy, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final PolicyNode PolicyNode(Position pos, Policy policy, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extPolicyNode());
        return new PolicyNode_c(pos, policy, ext);
    }

    @Override
    public AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, Expr expr) {
        AmbDynamicLabelNode n =
                AmbDynamicLabelNode(pos, expr, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AmbDynamicLabelNode AmbDynamicLabelNode(Position pos,
            Expr expr, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbDynamicLabelNode());
        return new AmbDynamicLabelNode_c(pos, expr, ext);
    }

    @Override
    public AmbVarLabelNode AmbVarLabelNode(Position pos, Id name) {
        AmbVarLabelNode n = AmbVarLabelNode(pos, name, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AmbVarLabelNode AmbVarLabelNode(Position pos, Id name,
            Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbVarLabelNode());
        return new AmbVarLabelNode_c(pos, name, ext);
    }

    @Override
    public AmbThisLabelNode AmbThisLabelNode(Position pos) {
        AmbThisLabelNode n = AmbThisLabelNode(pos, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AmbThisLabelNode AmbThisLabelNode(Position pos, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbThisLabelNode());
        return new AmbThisLabelNode_c(pos, ext);
    }

    @Override
    public AmbProviderLabelNode AmbProviderLabelNode(Position pos,
            TypeNode typeNode) {
        AmbProviderLabelNode n =
                AmbProviderLabelNode(pos, typeNode, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AmbProviderLabelNode AmbProviderLabelNode(Position pos,
            TypeNode typeNode, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbProviderLabelNode());
        return new AmbProviderLabelNode_c(pos, typeNode, ext);
    }

    @Override
    public CanonicalLabelNode CanonicalLabelNode(Position pos, Label label) {
        CanonicalLabelNode n =
                CanonicalLabelNode(pos, label, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final CanonicalLabelNode CanonicalLabelNode(Position pos,
            Label label, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extCanonicalLabelNode());
        return new CanonicalLabelNode_c(pos, label, ext);
    }

    @Override
    public AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr) {
        AmbPrincipalNode n = AmbPrincipalNode(pos, expr, null, extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr,
            Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbPrincipalNode());
        return new AmbPrincipalNode_c(pos, expr, ext);
    }

    @Override
    public AmbPrincipalNode AmbPrincipalNode(Position pos, Id name) {
        AmbPrincipalNode n = AmbPrincipalNode(pos, name, null, extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final AmbPrincipalNode AmbPrincipalNode(Position pos, Id name,
            Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbPrincipalNode());
        return new AmbPrincipalNode_c(pos, name, ext);
    }

    @Override
    public AmbPrincipalNode AmbConjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right) {
        AmbPrincipalNode n =
                AmbJunctivePrincipalNode(pos, left, right, true, null,
                        extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final AmbPrincipalNode AmbJunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right, boolean isConjunction,
            Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAmbPrincipalNode());
        return new AmbJunctivePrincipalNode_c(pos, left, right, true, ext);
    }

    @Override
    public AmbPrincipalNode AmbDisjunctivePrincipalNode(Position pos,
            PrincipalNode left, PrincipalNode right) {
        AmbPrincipalNode n =
                AmbJunctivePrincipalNode(pos, left, right, false, null,
                        extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    @Override
    public CanonicalPrincipalNode CanonicalPrincipalNode(Position pos,
            Principal principal) {
        CanonicalPrincipalNode n =
                CanonicalPrincipalNode(pos, principal, null, extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final CanonicalPrincipalNode CanonicalPrincipalNode(Position pos,
            Principal principal, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef)
                                        .extCanonicalPrincipalNode());
        return new CanonicalPrincipalNode_c(pos, principal, ext);
    }

    @Override
    public ArrayAccessAssign ArrayAccessAssign(Position pos, ArrayAccess left,
            polyglot.ast.Assign.Operator op, Expr right) {
        ArrayAccessAssign n =
                JifArrayAccessAssign(pos, left, op, right, null, extFactory());
        n = del(n, delFactory().delArrayAccessAssign());
        return n;
    }

    protected final ArrayAccessAssign JifArrayAccessAssign(Position pos,
            ArrayAccess left, polyglot.ast.Assign.Operator op, Expr right,
            Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory())
            ext = composeExts(ext, ef.extArrayAccessAssign());
        return new JifArrayAccessAssign_c(pos, left, op, right, ext);
    }

    @Override
    public ClassDecl ClassDecl(Position pos, Flags flags, Id name,
            TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
        return JifClassDecl(pos, flags, name,
                Collections.<ParamDecl> emptyList(), superClass, interfaces,
                Collections.<PrincipalNode> emptyList(),
                Collections.<ConstraintNode<Assertion>> emptyList(), body);
    }

    @Override
    public JifClassDecl JifClassDecl(Position pos, Flags flags, Id name,
            List<ParamDecl> params, TypeNode superClass,
            List<TypeNode> interfaces, List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body) {
        JifClassDecl n =
                JifClassDecl(pos, flags, name, params, superClass, interfaces,
                        authority, constraints, body, null, extFactory());
        n = del(n, delFactory().delClassDecl());
        return n;
    }

    protected final JifClassDecl JifClassDecl(Position pos, Flags flags,
            Id name, List<ParamDecl> params, TypeNode superClass,
            List<TypeNode> interfaces, List<PrincipalNode> authority,
            List<ConstraintNode<Assertion>> constraints, ClassBody body,
            Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory)
            ext = composeExts(ext, ef.extClassDecl());
        return new JifClassDecl_c(pos, flags, name, params, superClass,
                interfaces, authority, constraints, body, ext);
    }

    @Override
    public MethodDecl MethodDecl(Position pos, Flags flags,
            TypeNode returnType, Id name, List<Formal> formals,
            List<TypeNode> throwTypes, Block body) {
        return JifMethodDecl(pos, flags, returnType, name, null, formals, null,
                throwTypes,
                Collections.<ConstraintNode<Assertion>> emptyList(), body);
    }

    @Override
    public JifMethodDecl JifMethodDecl(Position pos, Flags flags,
            TypeNode returnType, Id name, LabelNode startLabel,
            List<Formal> formals, LabelNode endLabel,
            List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body) {
        JifMethodDecl n =
                JifMethodDecl(pos, flags, returnType, name, startLabel,
                        formals, endLabel, throwTypes, constraints, body, null,
                        extFactory());
        n = del(n, delFactory().delMethodDecl());
        return n;
    }

    protected final JifMethodDecl JifMethodDecl(Position pos, Flags flags,
            TypeNode returnType, Id name, LabelNode startLabel,
            List<Formal> formals, LabelNode endLabel,
            List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body, Ext ext,
            ExtFactory extFactory) {
        for (ExtFactory ef : extFactory)
            ext = composeExts(ext, ef.extMethodDecl());
        return new JifMethodDecl_c(pos, flags, returnType, name, startLabel,
                formals, endLabel, throwTypes, constraints, body, ext);
    }

    @Override
    public ConstructorDecl ConstructorDecl(Position pos, Flags flags, Id name,
            List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        return JifConstructorDecl(pos, flags, name, null, null, formals,
                throwTypes,
                Collections.<ConstraintNode<Assertion>> emptyList(), body);
    }

    @Override
    public JifConstructorDecl JifConstructorDecl(Position pos, Flags flags,
            Id name, LabelNode startLabel, LabelNode returnLabel,
            List<Formal> formals, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body) {
        JifConstructorDecl n =
                JifConstructorDecl(pos, flags, name, startLabel, returnLabel,
                        formals, throwTypes, constraints, body, null,
                        extFactory());
        n = del(n, delFactory().delConstructorDecl());
        return n;
    }

    protected final JifConstructorDecl JifConstructorDecl(Position pos,
            Flags flags, Id name, LabelNode startLabel, LabelNode returnLabel,
            List<Formal> formals, List<TypeNode> throwTypes,
            List<ConstraintNode<Assertion>> constraints, Block body, Ext ext,
            ExtFactory extFactory) {
        for (ExtFactory ef : extFactory)
            ext = composeExts(ext, ef.extConstructorDecl());
        return new JifConstructorDecl_c(pos, flags, name, startLabel,
                returnLabel, formals, throwTypes, constraints, body, ext);
    }

    @Override
    public New New(Position pos, Expr outer, TypeNode objectType,
            List<Expr> args, ClassBody body) {
        New n = JifNew(pos, outer, objectType, args, body, null, extFactory());
        n = del(n, delFactory().delNew());
        return n;
    }

    protected final New JifNew(Position pos, Expr outer, TypeNode objectType,
            List<Expr> args, ClassBody body, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extNew());
        return new JifNew_c(pos, outer, objectType, args, body, ext);
    }

    @Override
    public AmbParam AmbParam(Position pos, Id name) {
        return AmbParam(pos, name, null);
    }

    @Override
    public AmbParam AmbParam(Position pos, Id name, ParamInstance pi) {
        AmbParam n = AmbParam(pos, name, pi, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AmbParam AmbParam(Position pos, Id name, ParamInstance pi,
            Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extAmbParam());
        return new AmbParam_c(pos, name, pi, ext);
    }

    @Override
    public AmbExprParam AmbParam(Position pos, Expr expr,
            ParamInstance expectedPI) {
        AmbExprParam n =
                AmbExprParam(pos, expr, expectedPI, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AmbExprParam AmbExprParam(Position pos, Expr expr,
            ParamInstance expectedPI, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extAmbParam());
        return new AmbExprParam_c(pos, expr, expectedPI, ext);
    }

    @Override
    public ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, Id name) {
        ParamDecl n = ParamDecl(pos, kind, name, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind,
            Id name, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extParamDecl());
        return new ParamDecl_c(pos, kind, name, ext);
    }

    @Override
    public CanonicalConstraintNode CanonicalConstraintNode(Position pos,
            Assertion constraint) {
        CanonicalConstraintNode n =
                CanonicalConstraintNode(pos, constraint, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final CanonicalConstraintNode CanonicalConstraintNode(
            Position pos, Assertion constraint, Ext ext, ExtFactory extFactory) {
        if (!constraint.isCanonical()) {
            throw new InternalCompilerError(constraint + " is not canonical.");
        }

        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef)
                                        .extCanonicalConstraintNode());
        return new CanonicalConstraintNode_c(pos, constraint, ext);
    }

    @Override
    public AuthConstraintNode AuthConstraintNode(Position pos,
            List<PrincipalNode> principals) {
        AuthConstraintNode n =
                AuthConstraintNode(pos, principals, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AuthConstraintNode AuthConstraintNode(Position pos,
            List<PrincipalNode> principals, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extAuthConstraintNode());
        return new AuthConstraintNode_c(pos, principals, ext);
    }

    @Override
    public AutoEndorseConstraintNode AutoEndorseConstraintNode(Position pos,
            LabelNode endorseTo) {
        AutoEndorseConstraintNode n =
                AutoEndorseConstraintNode(pos, endorseTo, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final AutoEndorseConstraintNode AutoEndorseConstraintNode(
            Position pos, LabelNode endorseTo, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef)
                                        .extAutoEndorseConstraintNode());
        return new AutoEndorseConstraintNode_c(pos, endorseTo, ext);
    }

    @Override
    public CallerConstraintNode CallerConstraintNode(Position pos,
            List<PrincipalNode> principals) {
        CallerConstraintNode n =
                CallerConstraintNode(pos, principals, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final CallerConstraintNode CallerConstraintNode(Position pos,
            List<PrincipalNode> principals, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extCallerConstraintNode());
        return new CallerConstraintNode_c(pos, principals, ext);
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
                PrincipalActsForPrincipalConstraintNode(pos, actor, granter,
                        isEquiv, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final PrincipalActsForPrincipalConstraintNode PrincipalActsForPrincipalConstraintNode(
            Position pos, PrincipalNode actor, PrincipalNode granter,
            boolean isEquiv, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(
                                ext,
                                ((JifExtFactory) ef)
                                        .extPrincipalActsForPrincipalConstraintNode());
        return new PrincipalActsForPrincipalConstraintNode_c(pos, actor,
                granter, isEquiv, ext);
    }

    @Override
    public LabelActsForPrincipalConstraintNode LabelActsForPrincipalConstraintNode(
            Position pos, LabelNode actor, PrincipalNode granter) {
        LabelActsForPrincipalConstraintNode n =
                LabelActsForPrincipalConstraintNode(pos, actor, granter, null,
                        extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final LabelActsForPrincipalConstraintNode LabelActsForPrincipalConstraintNode(
            Position pos, LabelNode actor, PrincipalNode granter, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(
                                ext,
                                ((JifExtFactory) ef)
                                        .extLabelActsForPrincipalConstraintNode());
        return new LabelActsForPrincipalConstraintNode_c(pos, actor, granter,
                ext);
    }

    @Override
    public LabelActsForLabelConstraintNode LabelActsForLabelConstraintNode(
            Position pos, LabelNode actor, LabelNode granter) {
        LabelActsForLabelConstraintNode n =
                LabelActsForLabelConstraintNode(pos, actor, granter, null,
                        extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final LabelActsForLabelConstraintNode LabelActsForLabelConstraintNode(
            Position pos, LabelNode actor, LabelNode granter, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef)
                                        .extLabelActsForLabelConstraintNode());
        return new LabelActsForLabelConstraintNode_c(pos, actor, granter, ext);
    }

    @Override
    public LabelLeAssertionNode LabelLeAssertionNode(Position pos,
            LabelNode lhs, LabelNode rhs, boolean isEquiv) {
        LabelLeAssertionNode n =
                LabelLeAssertionNode(pos, lhs, rhs, isEquiv, null, extFactory());
        n = del(n, delFactory().delNode());
        return n;
    }

    protected final LabelLeAssertionNode LabelLeAssertionNode(Position pos,
            LabelNode lhs, LabelNode rhs, boolean isEquiv, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extLabelLeAssertionNode());
        return new LabelLeAssertionNode_c(pos, lhs, rhs, isEquiv, ext);
    }

    @Override
    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound,
            LabelNode label, Stmt body) {
        DeclassifyStmt n =
                DeclassifyStmt(pos, bound, label, body, null, extFactory());
        n = del(n, delFactory().delStmt());
        return n;
    }

    protected final DeclassifyStmt DeclassifyStmt(Position pos,
            LabelNode bound, LabelNode label, Stmt body, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extDeclassifyStmt());
        return new DeclassifyStmt_c(pos, bound, label, body, ext);
    }

    @Override
    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode label,
            Stmt body) {
        return DeclassifyStmt(pos, null, label, body);
    }

    @Override
    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr,
            LabelNode bound, LabelNode label) {
        DeclassifyExpr n =
                DeclassifyExpr(pos, expr, bound, label, null, extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final DeclassifyExpr DeclassifyExpr(Position pos, Expr expr,
            LabelNode bound, LabelNode label, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extDeclassifyExpr());
        return new DeclassifyExpr_c(pos, expr, bound, label, ext);
    }

    @Override
    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr,
            LabelNode label) {
        return DeclassifyExpr(pos, expr, null, label);
    }

    @Override
    public EndorseStmt EndorseStmt(Position pos, LabelNode bound,
            LabelNode label, Stmt body) {
        EndorseStmt n =
                EndorseStmt(pos, bound, label, body, null, extFactory());
        n = del(n, delFactory().delStmt());
        return n;
    }

    protected final EndorseStmt EndorseStmt(Position pos, LabelNode bound,
            LabelNode label, Stmt body, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extEndorseStmt());
        return new EndorseStmt_c(pos, bound, label, body, ext);
    }

    @Override
    public EndorseStmt EndorseStmt(Position pos, LabelNode label, Stmt body) {
        return EndorseStmt(pos, null, label, body);
    }

    @Override
    public CheckedEndorseStmt CheckedEndorseStmt(Position pos, Expr e,
            LabelNode bound, LabelNode label, If body) {
        CheckedEndorseStmt n =
                CheckedEndorseStmt(pos, e, bound, label, body, null,
                        extFactory());
        n = del(n, delFactory().delStmt());
        return n;
    }

    protected final CheckedEndorseStmt CheckedEndorseStmt(Position pos, Expr e,
            LabelNode bound, LabelNode label, If body, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext =
                        composeExts(ext,
                                ((JifExtFactory) ef).extCheckedEndorseStmt());
        return new CheckedEndorseStmt_c(pos, e, bound, label, body, ext);
    }

    @Override
    public EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode label) {
        return EndorseExpr(pos, expr, null, label);
    }

    @Override
    public EndorseExpr EndorseExpr(Position pos, Expr expr, LabelNode bound,
            LabelNode label) {
        EndorseExpr n =
                EndorseExpr(pos, expr, bound, label, null, extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final EndorseExpr EndorseExpr(Position pos, Expr expr,
            LabelNode bound, LabelNode label, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extEndorseExpr());
        return new EndorseExpr_c(pos, expr, bound, label, ext);
    }

    @Override
    public LabelExpr LabelExpr(Position pos, Label l) {
        LabelNode ln = CanonicalLabelNode(pos, l);
        return LabelExpr(pos, ln);
    }

    public LabelExpr LabelExpr(Position pos, LabelNode node) {
        LabelExpr n = LabelExpr(pos, node, null, extFactory());
        n = del(n, ((JifDelFactory) delFactory()).delLabelExpr());
        return n;
    }

    protected final LabelExpr LabelExpr(Position pos, LabelNode node, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extLabelExpr());
        return new LabelExpr_c(pos, node, ext);
    }

    @Override
    public NewLabel NewLabel(Position pos, LabelNode label) {
        NewLabel n = NewLabel(pos, label, null, extFactory());
        n = del(n, ((JifDelFactory) delFactory()).delNewLabel());
        return n;
    }

    protected final NewLabel NewLabel(Position pos, LabelNode label, Ext ext,
            ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extNewLabel());
        return new NewLabel_c(pos, label, ext);
    }

    @Override
    public PrincipalExpr PrincipalExpr(Position pos, PrincipalNode principal) {
        PrincipalExpr n = PrincipalExpr(pos, principal, null, extFactory());
        n = del(n, delFactory().delExpr());
        return n;
    }

    protected final PrincipalExpr PrincipalExpr(Position pos,
            PrincipalNode principal, Ext ext, ExtFactory extFactory) {
        // XXX TODO FIXME What's the new way of doing things?
        for (ExtFactory ef : extFactory)
            if (ef instanceof JifExtFactory)
                ext = composeExts(ext, ((JifExtFactory) ef).extPrincipalExpr());
        return new PrincipalExpr_c(pos, principal, ext);
    }

    @Override
    public Catch Catch(Position pos, Formal formal, Block body) {
        Catch n = JifCatch(pos, formal, body, null, extFactory());
        n = del(n, delFactory().delCatch());
        return n;
    }

    protected final Catch JifCatch(Position pos, Formal formal, Block body,
            Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory())
            ext = composeExts(ext, ef.extCatch());
        return new JifCatch_c(pos, formal, body, ext);
    }

    @Override
    public Formal Formal(Position pos, Flags flags, TypeNode type, Id name) {
        Formal n = JifFormal(pos, flags, type, name, null, extFactory());
        n = del(n, delFactory().delFormal());
        return n;
    }

    protected final Formal JifFormal(Position pos, Flags flags, TypeNode type,
            Id name, Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory())
            ext = composeExts(ext, ef.extFormal());
        return new JifFormal_c(pos, flags, type, name, ext);
    }

    @Override
    public Binary Binary(Position pos, Expr left, Operator op, Expr right) {
        Binary n = JifBinary(pos, left, op, right, null, extFactory());
        n = del(n, delFactory().delBinary());
        return n;
    }

    protected final Binary JifBinary(Position pos, Expr left, Operator op,
            Expr right, Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory())
            ext = composeExts(ext, ef.extBinary());
        return new JifBinary_c(pos, left, op, right, ext);
    }

    @Override
    public TypeNode ConstArrayTypeNode(Position pos, TypeNode base) {
        TypeNode n = ConstArrayTypeNode(pos, base, null, extFactory());
        n = del(n, delFactory().delArrayTypeNode());
        return n;
    }

    protected final ConstArrayTypeNode ConstArrayTypeNode(Position pos,
            TypeNode base, Ext ext, ExtFactory extFactory) {
        for (ExtFactory ef : extFactory)
            ext = composeExts(ext, ef.extArrayTypeNode());
        return new ConstArrayTypeNode_c(pos, base, ext);
    }

    @Override
    public Prologue Prologue(Position pos, List<Stmt> stmts) {
        Prologue n = Prologue(pos, stmts, null, extFactory());
        n = del(n, delFactory().delBlock());
        return n;
    }

    protected final Prologue Prologue(Position pos, List<Stmt> stmts, Ext ext,
            ExtFactory extFactory) {
        for (ExtFactory ef : extFactory)
            ext = composeExts(ext, ef.extBlock());
        return new Prologue_c(pos, stmts, ext);
    }
}
