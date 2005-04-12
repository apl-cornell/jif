package jif.ast;

import java.util.Collections;
import java.util.List;

import jif.types.Assertion;
import jif.types.ParamInstance;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ext.jl.ast.NodeFactory_c;
import polyglot.types.Flags;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifNodeFactory</code> interface. 
 */
public class JifNodeFactory_c extends NodeFactory_c implements JifNodeFactory
{
    public JifNodeFactory_c() {
        this(new JifExtFactory_c());
    }
    protected JifNodeFactory_c(ExtFactory extFact) {
        super(extFact, new JifDelFactory_c());
    }

    /**
     * Get the JifExtFactory_c instance.
     */
    protected JifExtFactory_c jifExtFactory() {
        if (jifExtFact == null) {
            jifExtFact = (JifExtFactory_c)findExtFactInstance(JifExtFactory_c.class);
        }            
        return jifExtFact;
    }
    /** cache the result of looking for the JifExtFactory */
    JifExtFactory_c jifExtFact = null;

    public Disamb disamb() {
        return new JifDisamb_c();
    }

    public InstTypeNode InstTypeNode(Position pos, TypeNode type, List params) {
        InstTypeNode n = new InstTypeNode_c(pos, type, params);
        n = (InstTypeNode)n.ext(jifExtFactory().extInstTypeNode());
        n = (InstTypeNode)n.del(delFactory().delTypeNode());
        return n;
    }

    public LabeledTypeNode LabeledTypeNode(Position pos, TypeNode type, LabelNode label) {
        LabeledTypeNode n = new LabeledTypeNode_c(pos, type, label);
        n = (LabeledTypeNode)n.ext(jifExtFactory().extLabeledTypeNode());
        n = (LabeledTypeNode)n.del(delFactory().delTypeNode());
        return n;
    }

    public AmbNewArray AmbNewArray(Position pos, TypeNode baseType, String name, List dims) {
        AmbNewArray n = new AmbNewArray_c(pos, baseType, name, dims);
        n = (AmbNewArray)n.ext(jifExtFactory().extAmbNewArray());
        n = (AmbNewArray)n.del(delFactory().delAmbExpr());
        return n;
    }

    public AmbParamTypeOrAccess AmbParamTypeOrAccess(Position pos, Receiver base, String name) {
        AmbParamTypeOrAccess n = new AmbParamTypeOrAccess_c(pos, base,
                                                            name);
        n = (AmbParamTypeOrAccess)n.ext(jifExtFactory().extAmbParamTypeOrAccess());
        n = (AmbParamTypeOrAccess)n.del(delFactory().delAmbReceiver());
        return n;
    }

    public JoinLabelNode JoinLabelNode(Position pos, List components) {
        JoinLabelNode n = new JoinLabelNode_c(pos, components);
        n = (JoinLabelNode)n.ext(jifExtFactory().extJoinLabelNode());
        n = (JoinLabelNode)n.del(delFactory().delNode());
        return n;
    }

    public PolicyLabelNode PolicyLabelNode(Position pos, PrincipalNode owner, List readers) {
        PolicyLabelNode n = new PolicyLabelNode_c(pos, owner,
                                                  readers);
        n = (PolicyLabelNode)n.ext(jifExtFactory().extPolicyLabelNode());
        n = (PolicyLabelNode)n.del(delFactory().delNode());
        return n;
    }

    public AmbDynamicLabelNode AmbDynamicLabelNode(Position pos, Expr expr) {
        AmbDynamicLabelNode n = new AmbDynamicLabelNode_c(pos, expr);
        n = (AmbDynamicLabelNode)n.ext(jifExtFactory().extAmbDynamicLabelNode());
        n = (AmbDynamicLabelNode)n.del(delFactory().delNode());
        return n;
    }

    public AmbVarLabelNode AmbVarLabelNode(Position pos, String name) {
        AmbVarLabelNode n = new AmbVarLabelNode_c(pos, name);
        n = (AmbVarLabelNode)n.ext(jifExtFactory().extAmbVarLabelNode());
        n = (AmbVarLabelNode)n.del(delFactory().delNode());
        return n;
    }

    public AmbThisLabelNode AmbThisLabelNode(Position pos) {
        AmbThisLabelNode n = new AmbThisLabelNode_c(pos);
        n = (AmbThisLabelNode)n.ext(jifExtFactory().extAmbThisLabelNode());
        n = (AmbThisLabelNode)n.del(delFactory().delNode());
        return n;
    }

    public CanonicalLabelNode CanonicalLabelNode(Position pos, Label label) {
        if (! label.isCanonical()) {
            throw new InternalCompilerError(label + " is not canonical.");
        }
        CanonicalLabelNode n = new CanonicalLabelNode_c(pos, label);
        n = (CanonicalLabelNode)n.ext(jifExtFactory().extCanonicalLabelNode());
        n = (CanonicalLabelNode)n.del(delFactory().delNode());
        return n;
    }

    public AmbPrincipalNode AmbPrincipalNode(Position pos, Expr expr) {
        AmbPrincipalNode n = new AmbPrincipalNode_c(pos, expr);
        n = (AmbPrincipalNode)n.ext(jifExtFactory().extAmbPrincipalNode());
        n = (AmbPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    public AmbPrincipalNode AmbPrincipalNode(Position pos, String name) {
        AmbPrincipalNode n = new AmbPrincipalNode_c(pos, name);
        n = (AmbPrincipalNode)n.ext(jifExtFactory().extAmbPrincipalNode());
        n = (AmbPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    public CanonicalPrincipalNode CanonicalPrincipalNode(Position pos, Principal principal) {
        CanonicalPrincipalNode n = new CanonicalPrincipalNode_c(pos,
                                                                principal);
        n = (CanonicalPrincipalNode)n.ext(jifExtFactory().extCanonicalPrincipalNode());
        n = (CanonicalPrincipalNode)n.del(delFactory().delExpr());
        return n;
    }

    public ClassDecl ClassDecl(Position pos, Flags flags, String name, TypeNode superClass, List interfaces, ClassBody body) {
        ClassDecl n = new JifClassDecl_c(pos, flags, name,
                Collections.EMPTY_LIST, false, superClass, interfaces, 
                Collections.EMPTY_LIST, body);
        n = (ClassDecl)n.ext(extFactory().extClassDecl());
        n = (ClassDecl)n.del(delFactory().delClassDecl());
        return n;
    }

    public LocalClassDecl LocalClassDecl(Position pos, ClassDecl decl) {
        throw new InternalCompilerError("Jif does not support inner classes.");
    }

    public JifClassDecl JifClassDecl(Position pos, Flags flags, String name,
                                     List params, boolean invariant,
                                     TypeNode superClass, List interfaces,
                                     List authority, ClassBody body) {
        JifClassDecl n = new JifClassDecl_c(pos, flags, name,
                                            params, invariant, superClass, interfaces, authority, body);
        n = (JifClassDecl)n.ext(extFactory().extClassDecl());
        n = (JifClassDecl)n.del(delFactory().delClassDecl());
        return n;
    }

    public MethodDecl MethodDecl(Position pos, Flags flags, TypeNode returnType,
            String name, List formals, List throwTypes, Block body) {
        MethodDecl n = new JifMethodDecl_c(pos, flags,
                                   returnType, name, null, formals, null,
                                   throwTypes, Collections.EMPTY_LIST, body);
        n = (MethodDecl)n.ext(extFactory().extMethodDecl());
        n = (MethodDecl)n.del(delFactory().delMethodDecl());
        return n;
    }

    public JifMethodDecl JifMethodDecl(Position pos, Flags flags, 
                                       TypeNode returnType, String name, LabelNode startLabel, 
                                       List formals, LabelNode endLabel, List throwTypes, 
                                       List constraints, Block body) {
//        //add default return value label node
//        if (! (returnType instanceof LabeledTypeNode) &&
//            (! (returnType instanceof CanonicalTypeNode) ||
//             !returnType.type().isVoid()) ) {
//            List comps = new LinkedList();
//            if (endLabel != null)
//                if (endLabel instanceof JoinLabelNode) 
//                    comps = TypedList.copyAndCheck(
//                                                   ((JoinLabelNode)endLabel).components(), 
//                                                   LabelNode.class, false);
//                else	    
//                    comps.add(endLabel.copy());
//
//            for (Iterator iter = formals.iterator(); iter.hasNext(); ) {
//                Formal arg = (Formal) iter.next();
//                comps.add(AmbVarLabelNode(arg.position(), arg.name()));
//            }
//            LabelNode LrvNode = JoinLabelNode(returnType.position(), comps);
//            returnType = LabeledTypeNode(returnType.position(), returnType,
//                                         LrvNode);
//        }

        JifMethodDecl n = new JifMethodDecl_c(pos, flags, 
                                              returnType, name, startLabel, formals, endLabel, 
                                              throwTypes, constraints, body);

        n = (JifMethodDecl)n.ext(extFactory().extMethodDecl());
        n = (JifMethodDecl)n.del(delFactory().delMethodDecl());
        return n;
    }

    public ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, Expr outer, List args) {
        if (outer != null) throw new InternalCompilerError("Jif does not support inner classes.");
        return super.ConstructorCall(pos, kind, outer, args);
    }

    public ConstructorDecl ConstructorDecl(Position pos, Flags flags, String name, List formals, List throwTypes, Block body) {
        ConstructorDecl n = new JifConstructorDecl_c(pos, flags, name, null, null, formals,
                                        throwTypes, Collections.EMPTY_LIST,
                                        body);
        n = (ConstructorDecl)n.ext(extFactory().extConstructorDecl());
        n = (ConstructorDecl)n.del(delFactory().delConstructorDecl());
        return n;
    }

    public JifConstructorDecl JifConstructorDecl(Position pos, Flags flags, String name, LabelNode startLabel, LabelNode returnLabel, List formals, List throwTypes, List constraints, Block body) {
        JifConstructorDecl n = new JifConstructorDecl_c(pos, flags,
                                                        name, startLabel, returnLabel, formals, throwTypes,
                                                        constraints, body);
        n = (JifConstructorDecl)n.ext(extFactory().extConstructorDecl());
        n = (JifConstructorDecl)n.del(delFactory().delConstructorDecl());
        return n;
    }
    
    public New New(Position pos, Expr outer, TypeNode objectType, List args, ClassBody body) {
        if (body != null) 
            throw new InternalCompilerError("Jif does not support inner classes.");
        if (outer != null)
            throw new InternalCompilerError("Jif does not support inner classes.");
        New n = new JifNew_c(pos, objectType, args, body);
        n = (New)n.ext(extFactory().extNew());
        n = (New)n.del(delFactory().delNew());
        return n;
    }

    public Special Special(Position pos, Special.Kind kind, TypeNode outer) {
        if (outer != null) throw new InternalCompilerError("Jif does not support inner classes.");
        return super.Special(pos, kind, outer);
    }

    public AmbParam AmbParam(Position pos, String name) {
        AmbParam n = new AmbParam_c(pos, name);
        n = (AmbParam)n.ext(jifExtFactory().extAmbParam());
        n = (AmbParam)n.del(delFactory().delNode());
        return n;
    }

    public ParamDecl ParamDecl(Position pos, ParamInstance.Kind kind, String name) {
        ParamDecl n = new ParamDecl_c(pos, kind, name);
        n = (ParamDecl)n.ext(jifExtFactory().extParamDecl());
        n = (ParamDecl)n.del(delFactory().delNode());
        return n;
    }

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

    public AuthConstraintNode AuthConstraintNode(Position pos, List principals) {
        AuthConstraintNode n = new AuthConstraintNode_c(pos,
                                                        principals);
        n = (AuthConstraintNode)n.ext(jifExtFactory().extAuthConstraintNode());
        n = (AuthConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    public CallerConstraintNode CallerConstraintNode(Position pos, List principals) {
        CallerConstraintNode n = new CallerConstraintNode_c(pos,
                                                            principals);
        n = (CallerConstraintNode)n.ext(jifExtFactory().extCallerConstraintNode());
        n = (CallerConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    public ActsForConstraintNode ActsForConstraintNode(Position pos, PrincipalNode actor, PrincipalNode granter) {
        ActsForConstraintNode n = new ActsForConstraintNode_c(pos,
                                                              actor, granter);
        n = (ActsForConstraintNode)n.ext(jifExtFactory().extActsForConstraintNode());
        n = (ActsForConstraintNode)n.del(delFactory().delNode());
        return n;
    }

    public SwitchLabel SwitchLabel(Position pos, Expr expr, List cases) {
        SwitchLabel n = new SwitchLabel_c(pos, expr, cases);
        n = (SwitchLabel)n.ext(jifExtFactory().extSwitchLabel());
        n = (SwitchLabel)n.del(delFactory().delStmt());
        return n;
    }

    public LabelCase LabelCase(Position pos, Formal decl, LabelNode label, Stmt body) {
        LabelCase n = new LabelCase_c(pos, decl, label, body);
        n = (LabelCase)n.ext(jifExtFactory().extLabelCase());
        n = (LabelCase)n.del(delFactory().delStmt());
        return n;
    }

    public LabelCase LabelCase(Position pos, LabelNode label, Stmt body) {
        return LabelCase(pos, null, label, body);
    }

    public LabelCase LabelCase(Position pos, Stmt body) {
        return LabelCase(pos, null, null, body);
    }

    public ActsFor ActsFor(Position pos, PrincipalNode actor, PrincipalNode granter, Stmt consequent) {
        return ActsFor(pos, actor, granter, consequent, null);
    }

    public ActsFor ActsFor(Position pos, PrincipalNode actor, PrincipalNode granter, Stmt consequent, Stmt alternative) {
        ActsFor n = new ActsFor_c(pos, actor, granter, consequent,
                                  alternative);
        n = (ActsFor)n.ext(jifExtFactory().extActsFor());
        n = (ActsFor)n.del(delFactory().delStmt());
        return n;
    }

    public DeclassifyStmt DeclassifyStmt(Position pos, LabelNode bound, LabelNode label, Stmt body) {
        DeclassifyStmt n = new DeclassifyStmt_c(pos, bound, label,
                                                body);
        n = (DeclassifyStmt)n.ext(jifExtFactory().extDeclassifyStmt());
        n = (DeclassifyStmt)n.del(delFactory().delStmt());
        return n;
    }

    public DeclassifyExpr DeclassifyExpr(Position pos, Expr expr, LabelNode bound,
                                         LabelNode label) 
    {
        DeclassifyExpr n = new DeclassifyExpr_c(pos, expr, bound,
                                                label);
        n = (DeclassifyExpr)n.ext(jifExtFactory().extDeclassifyExpr());
        n = (DeclassifyExpr)n.del(delFactory().delExpr());
        return n;
    }

    public NewLabel NewLabel(Position pos, LabelNode label) {
        NewLabel n = new NewLabel_c(pos, label);
        n = (NewLabel)n.ext(jifExtFactory().extNewLabel());
        n = (NewLabel)n.del(delFactory().delExpr());
        return n;
    }
    
    public Catch Catch(Position pos, Formal formal, Block body) {
        Catch n = new JifCatch_c(pos, formal, body);
        n = (Catch)n.ext(extFactory().extCatch());
        n = (Catch)n.del(delFactory().delCatch());
        return n;
    }
}
