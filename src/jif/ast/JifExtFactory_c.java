package jif.ast;

import jif.extension.JifArrayAccessAssignExt;
import jif.extension.JifArrayAccessExt;
import jif.extension.JifArrayInitExt;
import jif.extension.JifBinaryExt;
import jif.extension.JifBlockExt;
import jif.extension.JifBranchExt;
import jif.extension.JifCallExt;
import jif.extension.JifCaseExt;
import jif.extension.JifCastExt;
import jif.extension.JifCheckedEndorseStmtExt;
import jif.extension.JifClassBodyExt;
import jif.extension.JifClassDeclExt;
import jif.extension.JifConditionalExt;
import jif.extension.JifConstructorCallExt;
import jif.extension.JifConstructorDeclExt;
import jif.extension.JifDeclassifyExprExt;
import jif.extension.JifDeclassifyStmtExt;
import jif.extension.JifDoExt;
import jif.extension.JifEmptyExt;
import jif.extension.JifEndorseExprExt;
import jif.extension.JifEndorseStmtExt;
import jif.extension.JifEvalExt;
import jif.extension.JifExprExt;
import jif.extension.JifFieldAssignExt;
import jif.extension.JifFieldDeclExt_c;
import jif.extension.JifFieldExt;
import jif.extension.JifForExt;
import jif.extension.JifFormalExt;
import jif.extension.JifIfExt;
import jif.extension.JifInitializerExt;
import jif.extension.JifInstanceofExt;
import jif.extension.JifLabelExprExt;
import jif.extension.JifLabeledExt;
import jif.extension.JifLiteralExt;
import jif.extension.JifLocalAssignExt;
import jif.extension.JifLocalClassDeclExt;
import jif.extension.JifLocalDeclExt;
import jif.extension.JifLocalExt;
import jif.extension.JifMethodDeclExt;
import jif.extension.JifNewArrayExt;
import jif.extension.JifNewExt;
import jif.extension.JifPrincipalExprExt;
import jif.extension.JifPrincipalNodeExt;
import jif.extension.JifReturnExt;
import jif.extension.JifSourceFileExt;
import jif.extension.JifSpecialExt;
import jif.extension.JifSwitchExt;
import jif.extension.JifSynchronizedExt;
import jif.extension.JifThrowExt;
import jif.extension.JifTryExt;
import jif.extension.JifUnaryExt;
import jif.extension.JifWhileExt;
import jif.translate.ArrayAccessAssignToJavaExt_c;
import jif.translate.ArrayAccessToJavaExt_c;
import jif.translate.ArrayInitToJavaExt_c;
import jif.translate.BinaryToJavaExt_c;
import jif.translate.BlockToJavaExt_c;
import jif.translate.BranchToJavaExt_c;
import jif.translate.CallToJavaExt_c;
import jif.translate.CannotToJavaExt_c;
import jif.translate.CanonicalLabelNodeToJavaExt_c;
import jif.translate.CanonicalPrincipalNodeToJavaExt_c;
import jif.translate.CanonicalTypeNodeToJavaExt_c;
import jif.translate.CaseToJavaExt_c;
import jif.translate.CastToJavaExt_c;
import jif.translate.CatchToJavaExt_c;
import jif.translate.ClassBodyToJavaExt_c;
import jif.translate.ClassDeclToJavaExt_c;
import jif.translate.ConditionalToJavaExt_c;
import jif.translate.ConstructorCallToJavaExt_c;
import jif.translate.ConstructorDeclToJavaExt_c;
import jif.translate.DoToJavaExt_c;
import jif.translate.DowngradeExprToJavaExt_c;
import jif.translate.DowngradeStmtToJavaExt_c;
import jif.translate.EmptyToJavaExt_c;
import jif.translate.EvalToJavaExt_c;
import jif.translate.FieldAssignToJavaExt_c;
import jif.translate.FieldDeclToJavaExt_c;
import jif.translate.FieldToJavaExt_c;
import jif.translate.ForToJavaExt_c;
import jif.translate.FormalToJavaExt_c;
import jif.translate.IdToJavaExt_c;
import jif.translate.IfToJavaExt_c;
import jif.translate.ImportToJavaExt_c;
import jif.translate.InitializerToJavaExt_c;
import jif.translate.InstanceOfToJavaExt_c;
import jif.translate.LabelExprToJavaExt_c;
import jif.translate.LabeledToJavaExt_c;
import jif.translate.LitToJavaExt_c;
import jif.translate.LocalAssignToJavaExt_c;
import jif.translate.LocalClassDeclToJavaExt_c;
import jif.translate.LocalDeclToJavaExt_c;
import jif.translate.LocalToJavaExt_c;
import jif.translate.MethodDeclToJavaExt_c;
import jif.translate.NewArrayToJavaExt_c;
import jif.translate.NewLabelToJavaExt_c;
import jif.translate.NewToJavaExt_c;
import jif.translate.PackageNodeToJavaExt_c;
import jif.translate.PrincipalExprToJavaExt_c;
import jif.translate.ReturnToJavaExt_c;
import jif.translate.SourceFileToJavaExt_c;
import jif.translate.SpecialToJavaExt_c;
import jif.translate.SwitchBlockToJavaExt_c;
import jif.translate.SwitchToJavaExt_c;
import jif.translate.SynchronizedToJavaExt_c;
import jif.translate.ThrowToJavaExt_c;
import jif.translate.TryToJavaExt_c;
import jif.translate.UnaryToJavaExt_c;
import jif.translate.WhileToJavaExt_c;
import polyglot.ast.Ext;
import polyglot.ast.ExtFactory;

/**
 * This class provides is Jif's Extension factory, creating the appropriate
 * Ext objects as required.
 */
public class JifExtFactory_c extends AbstractJifExtFactory_c {
    public JifExtFactory_c() {
        super();
    }

    public JifExtFactory_c(ExtFactory nextExtFactory) {
        super(nextExtFactory);
    }

    @Override
    protected Ext extNodeImpl() {
        return new JifExt_c(new CannotToJavaExt_c());
    }

    @Override
    protected Ext extExprImpl() {
        return new JifExprExt(new CannotToJavaExt_c());
    }

    @Override
    protected Ext extIdImpl() {
        return new JifExt_c(new IdToJavaExt_c());
    }

    /**
     * This method returns a vanilla Jif extensions (Jif_c) with a
     * CannotToJavaExt_c for the ToJavaExt.
     */
    protected Ext extCannotToJavaImpl() {
        return new JifExt_c(new CannotToJavaExt_c());
    }

    @Override
    protected Ext extAmbExprImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extAmbPrefixImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extAmbQualifierNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extAmbReceiverImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extAmbTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extArrayAccessImpl() {
        return new JifArrayAccessExt(new ArrayAccessToJavaExt_c());
    }

    @Override
    protected Ext extArrayInitImpl() {
        return new JifArrayInitExt(new ArrayInitToJavaExt_c());
    }

    @Override
    protected Ext extLocalAssignImpl() {
        return new JifLocalAssignExt(new LocalAssignToJavaExt_c());
    }

    @Override
    protected Ext extFieldAssignImpl() {
        return new JifFieldAssignExt(new FieldAssignToJavaExt_c());
    }

    @Override
    protected Ext extArrayAccessAssignImpl() {
        return new JifArrayAccessAssignExt(new ArrayAccessAssignToJavaExt_c());
    }

    @Override
    protected Ext extBinaryImpl() {
        return new JifBinaryExt(new BinaryToJavaExt_c());
    }

    @Override
    protected Ext extBlockImpl() {
        return new JifBlockExt(new BlockToJavaExt_c());
    }

    @Override
    protected Ext extSwitchBlockImpl() {
        return new JifBlockExt(new SwitchBlockToJavaExt_c());
    }

    @Override
    protected Ext extBranchImpl() {
        return new JifBranchExt(new BranchToJavaExt_c());
    }

    @Override
    protected Ext extCallImpl() {
        return new JifCallExt(new CallToJavaExt_c());
    }

    @Override
    protected Ext extCaseImpl() {
        return new JifCaseExt(new CaseToJavaExt_c());
    }

    @Override
    protected Ext extCastImpl() {
        return new JifCastExt(new CastToJavaExt_c());
    }

    @Override
    protected Ext extCatchImpl() {
        return new JifExt_c(new CatchToJavaExt_c());
    }

    @Override
    protected Ext extClassBodyImpl() {
        return new JifClassBodyExt(new ClassBodyToJavaExt_c());
    }

    @Override
    protected Ext extClassDeclImpl() {
        return new JifClassDeclExt(new ClassDeclToJavaExt_c());
    }

    @Override
    protected Ext extConditionalImpl() {
        return new JifConditionalExt(new ConditionalToJavaExt_c());
    }

    @Override
    protected Ext extConstructorCallImpl() {
        return new JifConstructorCallExt(new ConstructorCallToJavaExt_c());
    }

    @Override
    protected Ext extConstructorDeclImpl() {
        return new JifConstructorDeclExt(new ConstructorDeclToJavaExt_c());
    }

    @Override
    protected Ext extFieldDeclImpl() {
        return new JifFieldDeclExt_c(new FieldDeclToJavaExt_c());
    }

    @Override
    protected Ext extDoImpl() {
        return new JifDoExt(new DoToJavaExt_c());
    }

    @Override
    protected Ext extEmptyImpl() {
        return new JifEmptyExt(new EmptyToJavaExt_c());
    }

    @Override
    protected Ext extEvalImpl() {
        return new JifEvalExt(new EvalToJavaExt_c());
    }

    @Override
    protected Ext extFieldImpl() {
        return new JifFieldExt(new FieldToJavaExt_c());
    }

    @Override
    protected Ext extForImpl() {
        return new JifForExt(new ForToJavaExt_c());
    }

    @Override
    protected Ext extFormalImpl() {
        return new JifFormalExt(new FormalToJavaExt_c());
    }

    @Override
    protected Ext extIfImpl() {
        return new JifIfExt(new IfToJavaExt_c());
    }

    @Override
    protected Ext extImportImpl() {
        return new JifExt_c(new ImportToJavaExt_c());
    }

    @Override
    protected Ext extInitializerImpl() {
        return new JifInitializerExt(new InitializerToJavaExt_c());
    }

    @Override
    protected Ext extInstanceofImpl() {
        return new JifInstanceofExt(new InstanceOfToJavaExt_c());
    }

    @Override
    protected Ext extLabeledImpl() {
        return new JifLabeledExt(new LabeledToJavaExt_c());
    }

    @Override
    protected Ext extLitImpl() {
        return new JifLiteralExt(new LitToJavaExt_c());
    }

    @Override
    protected Ext extLocalImpl() {
        return new JifLocalExt(new LocalToJavaExt_c());
    }

    @Override
    protected Ext extLocalDeclImpl() {
        return new JifLocalDeclExt(new LocalDeclToJavaExt_c());
    }

    @Override
    protected Ext extMethodDeclImpl() {
        return new JifMethodDeclExt(new MethodDeclToJavaExt_c());
    }

    @Override
    protected Ext extNewImpl() {
        return new JifNewExt(new NewToJavaExt_c());
    }

    @Override
    protected Ext extNewArrayImpl() {
        return new JifNewArrayExt(new NewArrayToJavaExt_c());
    }

    @Override
    protected Ext extReturnImpl() {
        return new JifReturnExt(new ReturnToJavaExt_c());
    }

    @Override
    protected Ext extSourceFileImpl() {
        return new JifSourceFileExt(new SourceFileToJavaExt_c());
    }

    @Override
    protected Ext extSpecialImpl() {
        return new JifSpecialExt(new SpecialToJavaExt_c());
    }

    @Override
    protected Ext extSwitchImpl() {
        return new JifSwitchExt(new SwitchToJavaExt_c());
    }

    @Override
    protected Ext extSynchronizedImpl() {
        return new JifSynchronizedExt(new SynchronizedToJavaExt_c());
    }

    @Override
    protected Ext extThrowImpl() {
        return new JifThrowExt(new ThrowToJavaExt_c());
    }

    @Override
    protected Ext extTryImpl() {
        return new JifTryExt(new TryToJavaExt_c());
    }

    @Override
    protected Ext extArrayTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extCanonicalTypeNodeImpl() {
        return new JifExt_c(new CanonicalTypeNodeToJavaExt_c());
    }

    @Override
    protected Ext extPackageNodeImpl() {
        return new JifExt_c(new PackageNodeToJavaExt_c());
    }

    @Override
    protected Ext extUnaryImpl() {
        return new JifUnaryExt(new UnaryToJavaExt_c());
    }

    @Override
    protected Ext extWhileImpl() {
        return new JifWhileExt(new WhileToJavaExt_c());
    }

    @Override
    protected Ext extInstTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extLabeledTypeNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extAmbNewArrayImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extAmbParamTypeOrAccessImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extCanonicalLabelNodeImpl() {
        return new JifExt_c(new CanonicalLabelNodeToJavaExt_c());
    }

    @Override
    protected Ext extParamNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extCanonicalPrincipalNodeImpl() {
        return new JifPrincipalNodeExt(new CanonicalPrincipalNodeToJavaExt_c());
    }

    @Override
    protected Ext extParamDeclImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extConstraintNodeImpl() {
        return extCannotToJavaImpl();
    }

    @Override
    protected Ext extDeclassifyStmtImpl() {
        return new JifDeclassifyStmtExt(new DowngradeStmtToJavaExt_c());
    }

    @Override
    protected Ext extDeclassifyExprImpl() {
        return new JifDeclassifyExprExt(new DowngradeExprToJavaExt_c());
    }

    @Override
    protected Ext extEndorseStmtImpl() {
        return new JifEndorseStmtExt(new DowngradeStmtToJavaExt_c());
    }

    @Override
    protected Ext extCheckedEndorseStmtImpl() {
        return new JifCheckedEndorseStmtExt(new DowngradeStmtToJavaExt_c());
    }

    @Override
    protected Ext extEndorseExprImpl() {
        return new JifEndorseExprExt(new DowngradeExprToJavaExt_c());
    }

    @Override
    protected Ext extNewLabelImpl() {
        return new JifLabelExprExt(new NewLabelToJavaExt_c());
    }

    @Override
    protected Ext extLabelExprImpl() {
        return new JifLabelExprExt(new LabelExprToJavaExt_c());
    }

    @Override
    protected Ext extPrincipalExprImpl() {
        return new JifPrincipalExprExt(new PrincipalExprToJavaExt_c());
    }

    @Override
    protected Ext extLocalClassDeclImpl() {
        return new JifLocalClassDeclExt(new LocalClassDeclToJavaExt_c());
    }
}
