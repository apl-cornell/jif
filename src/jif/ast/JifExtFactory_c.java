package jif.ast;

import jif.extension.*;
import jif.translate.*;
import polyglot.ast.Ext;
import polyglot.ext.jl.ast.AbstractExtFactory_c;

/**
 * This class provides is Jif's Extension factory, creating the appropriate
 * Ext objects as required.
 */
public class JifExtFactory_c extends AbstractExtFactory_c
{

    public JifExtFactory_c() {
        super();
    }

    protected Ext extNodeImpl() {
        return new Jif_c(new ToJavaExt_c());
    }

    protected Ext extExprImpl() {
        return new Jif_c(new ExprToJavaExt_c());
    }

    /**
     * This method returns a vanilla Jif extensions (Jif_c) with a
     * CannotToJavaExt_c for the ToJavaExt.
     */
    protected Ext extCannotToJavaImpl() {
        return new Jif_c(new CannotToJavaExt_c());
    }

    protected Ext extAmbExprImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extAmbPrefixImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extAmbQualifierNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extAmbReceiverImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extAmbTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extArrayAccessImpl() {
        return new JifArrayAccessExt(new ExprToJavaExt_c());
    }

    protected Ext extArrayInitImpl() {
        return new JifArrayInitExt(new ExprToJavaExt_c());
    }

    protected Ext extLocalAssignImpl() {
        return new JifLocalAssignExt(new ExprToJavaExt_c());
    }
    protected Ext extFieldAssignImpl() {
        return new JifFieldAssignExt(new ExprToJavaExt_c());
    }
    protected Ext extArrayAccessAssignImpl() {
        return new JifArrayAccessAssignExt(new ExprToJavaExt_c());
    }

    protected Ext extBinaryImpl() {
        return new JifBinaryExt(new ExprToJavaExt_c());
    }

    protected Ext extBlockImpl() {
        return new JifBlockExt(new ToJavaExt_c());
    }

    protected Ext extSwitchBlockImpl() {
        return new JifBlockExt(new ToJavaExt_c());
    }

    protected Ext extBranchImpl() {
        return new JifBranchExt(new ToJavaExt_c());
    }

    protected Ext extCallImpl() {
        return new JifCallExt(new CallToJavaExt_c());
    }

    protected Ext extCaseImpl() {
        return new JifCaseExt(new CaseToJavaExt_c());
    }

    protected Ext extCastImpl() {
        return new JifCastExt(new CastToJavaExt_c());
    }

    protected Ext extClassBodyImpl() {
        return new JifClassBodyExt(new ClassBodyToJavaExt_c());
    }

    protected Ext extClassDeclImpl() {
        return new JifClassDeclExt(new ClassDeclToJavaExt_c());
    }

    protected Ext extConditionalImpl() {
        return new JifConditionalExt(new ToJavaExt_c());
    }

    protected Ext extConstructorCallImpl() {
        return new JifConstructorCallExt(new ConstructorCallToJavaExt_c());
    }

    protected Ext extConstructorDeclImpl() {
        return new JifConstructorDeclExt(new ConstructorDeclToJavaExt_c());
    }

    protected Ext extFieldDeclImpl() {
        return new JifFieldDeclExt_c(new FieldDeclToJavaExt_c());
    }

    protected Ext extDoImpl() {
            return new JifDoExt(new ToJavaExt_c());
    }

    protected Ext extEmptyImpl() {
        return new JifEmptyExt(new ToJavaExt_c());
    }

    protected Ext extEvalImpl() {
        return new JifEvalExt(new ToJavaExt_c());
    }

    protected Ext extFieldImpl() {
        return new JifFieldExt(new FieldToJavaExt_c());
    }

    protected Ext extForImpl() {
            return new JifForExt(new ToJavaExt_c());
    }

    protected Ext extFormalImpl() {
        return new JifFormalExt(new FormalToJavaExt_c());
    }

    protected Ext extIfImpl() {
        return new JifIfExt(new ToJavaExt_c());
    }

    protected Ext extInitializerImpl() {
        return new JifInitializerExt(new InitializerToJavaExt_c());
    }

    protected Ext extInstanceofImpl() {
            return new JifInstanceofExt(new InstanceOfToJavaExt_c());
    }

    protected Ext extLabeledImpl() {
        return new JifLabeledExt(new ToJavaExt_c());
    }

    protected Ext extLitImpl() {
        return new JifLiteralExt(new ToJavaExt_c());
    }

    protected Ext extLocalImpl() {
        return new JifLocalExt(new LocalToJavaExt_c());
    }


    protected Ext extLocalDeclImpl() {
        return new JifLocalDeclExt(new LocalDeclToJavaExt_c());
    }

    protected Ext extMethodDeclImpl() {
        return new JifMethodDeclExt(new MethodDeclToJavaExt_c());
    }

    protected Ext extNewImpl() {
        return new JifNewExt(new NewToJavaExt_c());
    }

    protected Ext extNewArrayImpl() {
        return new JifNewArrayExt(new ExprToJavaExt_c());
    }

    protected Ext extReturnImpl() {
        return new JifReturnExt(new ReturnToJavaExt_c());
    }

    protected Ext extSourceFileImpl() {
        return new JifSourceFileExt(new SourceFileToJavaExt_c());
    }

    protected Ext extSpecialImpl() {
        return new JifSpecialExt(new ExprToJavaExt_c());
    }

    protected Ext extSwitchImpl() {
        return new JifSwitchExt(new ToJavaExt_c());
    }

    protected Ext extSynchronizedImpl() {
        return new JifSynchronizedExt(new ToJavaExt_c());
    }

    protected Ext extThrowImpl() {
        return new JifThrowExt(new ToJavaExt_c());
    }

    protected Ext extTryImpl() {
            return new JifTryExt(new ToJavaExt_c());
    }

    protected Ext extArrayTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extCanonicalTypeNodeImpl() {
        return new Jif_c(new CanonicalTypeNodeToJavaExt_c());
    }

    protected Ext extPackageNodeImpl() {
        return new Jif_c(new PackageNodeToJavaExt_c());
    }

    protected Ext extUnaryImpl() {
        return new JifUnaryExt(new ExprToJavaExt_c());
    }

    protected Ext extWhileImpl() {
        return new JifWhileExt(new ToJavaExt_c());
    }

    //----------------------------------------------------------------
    // Jif-specific nodes
    //-----------------------------------------------------------------

    public final Ext extInstTypeNode() {
        Ext e = extInstTypeNodeImpl();
        return postExtInstTypeNode(e);
    }

    public final Ext extLabeledTypeNode() {
        Ext e = extLabeledTypeNodeImpl();
        return postExtLabeledTypeNode(e);
    }

    public final Ext extAmbNewArray() {
        Ext e = extAmbNewArrayImpl();
        return postExtAmbNewArray(e);
    }

    public final Ext extAmbParamTypeOrAccess() {
        Ext e = extAmbParamTypeOrAccessImpl();
        return postExtAmbParamTypeOrAccess(e);
    }

    public final Ext extJoinLabelNode() {
        Ext e = extJoinLabelNodeImpl();
        return postExtJoinLabelNode(e);
    }

    public final Ext extMeetLabelNode() {
        Ext e = extMeetLabelNodeImpl();
        return postExtMeetLabelNode(e);
    }

    public final Ext extPolicyNode() {
        Ext e = extPolicyNodeImpl();
        return postExtPolicyNode(e);
    }

    public final Ext extAmbDynamicLabelNode() {
        Ext e = extAmbDynamicLabelNodeImpl();
        return postExtAmbDynamicLabelNode(e);
    }

    public final Ext extAmbVarLabelNode() {
        Ext e = extAmbVarLabelNodeImpl();
        return postExtAmbVarLabelNode(e);
    }

    public final Ext extAmbThisLabelNode() {
        Ext e = extAmbThisLabelNodeImpl();
        return postExtAmbThisLabelNode(e);
    }

    public final Ext extCanonicalLabelNode() {
        Ext e = extCanonicalLabelNodeImpl();
        return postExtCanonicalLabelNode(e);
    }

    public final Ext extParamNode() {
        Ext e = extParamNodeImpl();
        return postExtParamNode(e);
    }

    public final Ext extLabelNode() {
        Ext e = extLabelNodeImpl();
        return postExtLabelNode(e);
    }

    public final Ext extPrincipalNode() {
        Ext e = extPrincipalNodeImpl();
        return postExtPrincipalNode(e);
    }

    public final Ext extAmbPrincipalNode() {
        Ext e = extAmbPrincipalNodeImpl();
        return postExtAmbPrincipalNode(e);
    }


    public final Ext extCanonicalPrincipalNode() {
        Ext e = extCanonicalPrincipalNodeImpl();
        return postExtCanonicalPrincipalNode(e);
    }

    public final Ext extAmbParam() {
        Ext e = extAmbParamImpl();
        return postExtAmbParam(e);
    }

    public final Ext extParamDecl() {
        Ext e = extParamDeclImpl();
        return postExtParamDecl(e);
    }

    public final Ext extConstraintNode() {
        Ext e = extConstraintNodeImpl();
        return postExtConstraintNode(e);
    }

    public final Ext extCanonicalConstraintNode() {
        Ext e = extCanonicalConstraintNodeImpl();
        return postExtCanonicalConstraintNode(e);
    }

    public final Ext extAuthConstraintNode() {
        Ext e = extAuthConstraintNodeImpl();
        return postExtAuthConstraintNode(e);
    }

    public final Ext extCallerConstraintNode() {
        Ext e = extCallerConstraintNodeImpl();
        return postExtCallerConstraintNode(e);
    }

    public final Ext extActsForConstraintNode() {
        Ext e = extActsForConstraintNodeImpl();
        return postExtActsForConstraintNode(e);
    }

    public final Ext extLabelLeAssertionNode() {
        Ext e = extLabelLeAssertionNodeImpl();
        return postExtLabelLeAssertionNode(e);
    }

    public final Ext extActsFor() {
        Ext e = extActsForImpl();
        return postExtActsFor(e);
    }

    public final Ext extLabelIf() {
        Ext e = extLabelIfImpl();
        return postExtLabelIf(e);
    }

    public final Ext extDeclassifyStmt() {
        Ext e = extDeclassifyStmtImpl();
        return postExtDeclassifyStmt(e);
    }

    public final Ext extDeclassifyExpr() {
        Ext e = extDeclassifyExprImpl();
        return postExtDeclassifyExpr(e);
    }

    public final Ext extEndorseStmt() {
        Ext e = extEndorseStmtImpl();
        return postExtEndorseStmt(e);
    }

    public final Ext extEndorseExpr() {
        Ext e = extEndorseExprImpl();
        return postExtEndorseExpr(e);
    }

    public final Ext extNewLabel() {
        Ext e = extNewLabelImpl();
        return postExtNewLabel(e);
    }

    public final Ext extLabelExpr() {
        Ext e = extLabelExprImpl();
        return postExtLabelExpr(e);
    }

    public final Ext extPrincipalExpr() {
        Ext e = extPrincipalExprImpl();
        return postExtPrincipalExpr(e);
    }


    //----------------------------------------------------------------
    // Jif-specific nodes Impls
    //-----------------------------------------------------------------
    protected Ext extInstTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extLabeledTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extAmbNewArrayImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extAmbParamTypeOrAccessImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extJoinLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extMeetLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extPolicyNodeImpl() {
        return extLabelNode();
    }

    protected Ext extAmbDynamicLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extAmbVarLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extAmbThisLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extCanonicalLabelNodeImpl() {
        return new Jif_c(new CanonicalLabelNodeToJavaExt_c());
    }

    protected Ext extParamNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extLabelNodeImpl() {
        return extParamNode();
    }

    protected Ext extPrincipalNodeImpl() {
        return extParamNode();
    }

    protected Ext extAmbPrincipalNodeImpl() {
        return extPrincipalNode();
    }


    protected Ext extCanonicalPrincipalNodeImpl() {
        return new JifPrincipalNodeExt(new CanonicalPrincipalNodeToJavaExt_c());
    }

    protected Ext extAmbParamImpl() {
        return extParamNode();
    }

    protected Ext extParamDeclImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extConstraintNodeImpl() {
        return extCannotToJavaImpl();
    }

    protected Ext extCanonicalConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extAuthConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extCallerConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extActsForConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extLabelLeAssertionNodeImpl() {
        return extConstraintNode();
    }
    
    protected Ext extActsForImpl() {
        return new JifActsForExt(new ActsForToJavaExt_c());
    }

    protected Ext extLabelIfImpl() {
        return new JifLabelIfExt(new LabelIfToJavaExt_c());
    }


    protected Ext extDeclassifyStmtImpl() {
        return new JifDeclassifyStmtExt(new DowngradeStmtToJavaExt_c());
    }

    protected Ext extDeclassifyExprImpl() {
        return new JifDeclassifyExprExt(new DowngradeExprToJavaExt_c());
    }

    protected Ext extEndorseStmtImpl() {
        return new JifEndorseStmtExt(new DowngradeStmtToJavaExt_c());
    }

    protected Ext extEndorseExprImpl() {
        return new JifEndorseExprExt(new DowngradeExprToJavaExt_c());
    }

    protected Ext extNewLabelImpl() {
        return new JifLabelExprExt(new NewLabelToJavaExt_c());
    }
    protected Ext extLabelExprImpl() {
        return new JifLabelExprExt(new LabelExprToJavaExt_c());
    }
    protected Ext extPrincipalExprImpl() {
        return new JifPrincipalExprExt(new PrincipalExprToJavaExt_c());
    }

    //----------------------------------------------------------------
    // Jif-specific nodes Post methods
    //-----------------------------------------------------------------
    protected Ext postExtInstTypeNode(Ext e) {
        return postExtTypeNode(e);
    }

    protected Ext postExtLabeledTypeNode(Ext e) {
        return postExtTypeNode(e);
    }

    protected Ext postExtAmbNewArray(Ext e) {
        return postExtNode(e);
    }

    protected Ext postExtAmbParamTypeOrAccess(Ext e) {
        return postExtNode(e);
    }

    protected Ext postExtJoinLabelNode(Ext e) {
        return postExtLabelNode(e);
    }

    protected Ext postExtMeetLabelNode(Ext e) {
        return postExtLabelNode(e);
    }

    protected Ext postExtPolicyNode(Ext e) {
        return postExtNode(e);
    }

    protected Ext postExtAmbDynamicLabelNode(Ext e) {
        return postExtLabelNode(e);
    }

    protected Ext postExtAmbVarLabelNode(Ext e) {
        return postExtLabelNode(e);
    }

    protected Ext postExtAmbThisLabelNode(Ext e) {
        return postExtLabelNode(e);
    }

    protected Ext postExtCanonicalLabelNode(Ext e) {
        return postExtLabelNode(e);
    }

    protected Ext postExtParamNode(Ext e) {
        return postExtNode(e);
    }

    protected Ext postExtLabelNode(Ext e) {
        return postExtParamNode(e);
    }

    protected Ext postExtPrincipalNode(Ext e) {
        return postExtParamNode(e);
    }

    protected Ext postExtAmbPrincipalNode(Ext e) {
        return postExtPrincipalNode(e);
    }


    protected Ext postExtCanonicalPrincipalNode(Ext e) {
        return postExtPrincipalNode(e);
    }

    protected Ext postExtAmbParam(Ext e) {
        //return postExtAmbParam(e);
	return postExtNode(e);
    }

    protected Ext postExtParamDecl(Ext e) {
        return postExtNode(e);
    }

    protected Ext postExtConstraintNode(Ext e) {
        return postExtNode(e);
    }

    protected Ext postExtCanonicalConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtAuthConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtCallerConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtActsForConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtLabelLeAssertionNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtActsFor(Ext e) {
        return postExtStmt(e);
    }

    protected Ext postExtLabelIf(Ext e) {
        return postExtStmt(e);
    }

    protected Ext postExtDowngradeStmt(Ext e) {
        return postExtStmt(e);
    }

    protected Ext postExtDowngradeExpr(Ext e) {
        return postExtExpr(e);
    }

    protected Ext postExtDeclassifyStmt(Ext e) {
        return postExtDowngradeStmt(e);
    }

    protected Ext postExtDeclassifyExpr(Ext e) {
        return postExtDowngradeExpr(e);
    }

    protected Ext postExtEndorseStmt(Ext e) {
        return postExtDowngradeStmt(e);
    }

    protected Ext postExtEndorseExpr(Ext e) {
        return postExtDowngradeExpr(e);
    }

    protected Ext postExtNewLabel(Ext e) {
        return postExtLabelExpr(e);
    }

    protected Ext postExtLabelExpr(Ext e) {
        return postExtExpr(e);
    }
    protected Ext postExtPrincipalExpr(Ext e) {
        return postExtExpr(e);
    }
}
