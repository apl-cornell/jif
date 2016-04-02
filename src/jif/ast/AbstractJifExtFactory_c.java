package jif.ast;

import polyglot.ast.AbstractExtFactory_c;
import polyglot.ast.Ext;
import polyglot.ast.ExtFactory;

/**
 * This class provides is Jif's Extension factory, creating the appropriate
 * Ext objects as required.
 */
public class AbstractJifExtFactory_c extends AbstractExtFactory_c
        implements JifExtFactory {
    public AbstractJifExtFactory_c() {
        super();
    }

    public AbstractJifExtFactory_c(ExtFactory nextExtFactory) {
        super(nextExtFactory);
    }

    @Override
    public final Ext extInstTypeNode() {
        Ext e = extInstTypeNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extInstTypeNode();
            e = composeExts(e, e2);
        }
        return postExtInstTypeNode(e);
    }

    @Override
    public final Ext extLabeledTypeNode() {
        Ext e = extLabeledTypeNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extLabeledTypeNode();
            e = composeExts(e, e2);
        }
        return postExtLabeledTypeNode(e);
    }

    @Override
    public final Ext extAmbNewArray() {
        Ext e = extAmbNewArrayImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbNewArray();
            e = composeExts(e, e2);
        }
        return postExtAmbNewArray(e);
    }

    @Override
    public final Ext extAmbParamTypeOrAccess() {
        Ext e = extAmbParamTypeOrAccessImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbParamTypeOrAccess();
            e = composeExts(e, e2);
        }
        return postExtAmbParamTypeOrAccess(e);
    }

    @Override
    public final Ext extJoinLabelNode() {
        Ext e = extJoinLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extJoinLabelNode();
            e = composeExts(e, e2);
        }
        return postExtJoinLabelNode(e);
    }

    @Override
    public final Ext extMeetLabelNode() {
        Ext e = extMeetLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extMeetLabelNode();
            e = composeExts(e, e2);
        }
        return postExtMeetLabelNode(e);
    }

    @Override
    public final Ext extPolicyNode() {
        Ext e = extPolicyNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extPolicyNode();
            e = composeExts(e, e2);
        }
        return postExtPolicyNode(e);
    }

    @Override
    public final Ext extAmbDynamicLabelNode() {
        Ext e = extAmbDynamicLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbDynamicLabelNode();
            e = composeExts(e, e2);
        }
        return postExtAmbDynamicLabelNode(e);
    }

    @Override
    public final Ext extAmbVarLabelNode() {
        Ext e = extAmbVarLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbVarLabelNode();
            e = composeExts(e, e2);
        }
        return postExtAmbVarLabelNode(e);
    }

    @Override
    public final Ext extAmbThisLabelNode() {
        Ext e = extAmbThisLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbThisLabelNode();
            e = composeExts(e, e2);
        }
        return postExtAmbThisLabelNode(e);
    }

    @Override
    public Ext extAmbProviderLabelNode() {
        Ext e = extAmbProviderLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbProviderLabelNode();
            e = composeExts(e, e2);
        }
        return postExtAmbProviderLabelNode(e);
    }

    @Override
    public final Ext extCanonicalLabelNode() {
        Ext e = extCanonicalLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extCanonicalLabelNode();
            e = composeExts(e, e2);
        }
        return postExtCanonicalLabelNode(e);
    }

    @Override
    public final Ext extParamNode() {
        Ext e = extParamNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extParamNode();
            e = composeExts(e, e2);
        }
        return postExtParamNode(e);
    }

    @Override
    public final Ext extLabelNode() {
        Ext e = extLabelNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extLabelNode();
            e = composeExts(e, e2);
        }
        return postExtLabelNode(e);
    }

    @Override
    public final Ext extPrincipalNode() {
        Ext e = extPrincipalNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extPrincipalNode();
            e = composeExts(e, e2);
        }
        return postExtPrincipalNode(e);
    }

    @Override
    public final Ext extAmbPrincipalNode() {
        Ext e = extAmbPrincipalNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbPrincipalNode();
            e = composeExts(e, e2);
        }
        return postExtAmbPrincipalNode(e);
    }

    @Override
    public final Ext extCanonicalPrincipalNode() {
        Ext e = extCanonicalPrincipalNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extCanonicalPrincipalNode();
            e = composeExts(e, e2);
        }
        return postExtCanonicalPrincipalNode(e);
    }

    @Override
    public final Ext extAmbParam() {
        Ext e = extAmbParamImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAmbParam();
            e = composeExts(e, e2);
        }
        return postExtAmbParam(e);
    }

    @Override
    public final Ext extParamDecl() {
        Ext e = extParamDeclImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extParamDecl();
            e = composeExts(e, e2);
        }
        return postExtParamDecl(e);
    }

    @Override
    public final Ext extConstraintNode() {
        Ext e = extConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtConstraintNode(e);
    }

    @Override
    public final Ext extCanonicalConstraintNode() {
        Ext e = extCanonicalConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extCanonicalConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtCanonicalConstraintNode(e);
    }

    @Override
    public final Ext extAuthConstraintNode() {
        Ext e = extAuthConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAuthConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtAuthConstraintNode(e);
    }

    @Override
    public final Ext extAutoEndorseConstraintNode() {
        Ext e = extAutoEndorseConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extAutoEndorseConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtAutoEndorseConstraintNode(e);
    }

    @Override
    public final Ext extCallerConstraintNode() {
        Ext e = extCallerConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extCallerConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtCallerConstraintNode(e);
    }

    @Override
    public final Ext extActsForConstraintNode() {
        Ext e = extActsForConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extActsForConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtActsForConstraintNode(e);
    }

    @Override
    public final Ext extLabelActsForPrincipalConstraintNode() {
        Ext e = extLabelActsForPrincipalConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF)
                    .extLabelActsForPrincipalConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtLabelActsForPrincipalConstraintNode(e);
    }

    @Override
    public final Ext extLabelActsForLabelConstraintNode() {
        Ext e = extLabelActsForLabelConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF)
                    .extLabelActsForLabelConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtLabelActsForLabelConstraintNode(e);
    }

    @Override
    public final Ext extPrincipalActsForPrincipalConstraintNode() {
        Ext e = extPrincipalActsForPrincipalConstraintNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF)
                    .extPrincipalActsForPrincipalConstraintNode();
            e = composeExts(e, e2);
        }
        return postExtPrincipalActsForPrincipalConstraintNode(e);
    }

    @Override
    public final Ext extLabelLeAssertionNode() {
        Ext e = extLabelLeAssertionNodeImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extLabelLeAssertionNode();
            e = composeExts(e, e2);
        }
        return postExtLabelLeAssertionNode(e);
    }

    @Override
    public final Ext extDeclassifyStmt() {
        Ext e = extDeclassifyStmtImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifStmtExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extDeclassifyStmt();
            e = composeExts(e, e2);
        }
        return postExtDeclassifyStmt(e);
    }

    @Override
    public final Ext extDeclassifyExpr() {
        Ext e = extDeclassifyExprImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extDeclassifyExpr();
            e = composeExts(e, e2);
        }
        return postExtDeclassifyExpr(e);
    }

    @Override
    public final Ext extEndorseStmt() {
        Ext e = extEndorseStmtImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifStmtExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extEndorseStmt();
            e = composeExts(e, e2);
        }

        return postExtEndorseStmt(e);
    }

    @Override
    public final Ext extCheckedEndorseStmt() {
        Ext e = extCheckedEndorseStmtImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifStmtExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extCheckedEndorseStmt();
            e = composeExts(e, e2);
        }

        return postExtEndorseStmt(e);
    }

    @Override
    public final Ext extEndorseExpr() {
        Ext e = extEndorseExprImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extEndorseExpr();
            e = composeExts(e, e2);
        }
        return postExtEndorseExpr(e);
    }

    @Override
    public final Ext extNewLabel() {
        Ext e = extNewLabelImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extNewLabel();
            e = composeExts(e, e2);
        }
        return postExtNewLabel(e);
    }

    @Override
    public final Ext extLabelExpr() {
        Ext e = extLabelExprImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extLabelExpr();
            e = composeExts(e, e2);
        }
        return postExtLabelExpr(e);
    }

    @Override
    public final Ext extPrincipalExpr() {
        Ext e = extPrincipalExprImpl();

        ExtFactory nextEF = nextExtFactory();
        if (nextEF instanceof JifExtFactory) {
            Ext e2 = ((JifExtFactory) nextEF).extPrincipalExpr();
            e = composeExts(e, e2);
        }
        return postExtPrincipalExpr(e);
    }

    //----------------------------------------------------------------
    // Jif-specific nodes Impls
    //-----------------------------------------------------------------
    protected Ext extInstTypeNodeImpl() {
        return extTypeNodeImpl();
    }

    protected Ext extLabeledTypeNodeImpl() {
        return extTypeNodeImpl();
    }

    protected Ext extAmbNewArrayImpl() {
        return extExprImpl();
    }

    protected Ext extAmbParamTypeOrAccessImpl() {
        return extNodeImpl();
    }

    protected Ext extJoinLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extMeetLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extPolicyNodeImpl() {
        return extNode();
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

    protected Ext extAmbProviderLabelNodeImpl() {
        return extLabelNode();
    }

    protected Ext extCanonicalLabelNodeImpl() {
        return extNodeImpl();
    }

    protected Ext extParamNodeImpl() {
        return extNodeImpl();
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
        return extNodeImpl();
    }

    protected Ext extAmbParamImpl() {
        return extParamNode();
    }

    protected Ext extParamDeclImpl() {
        return extNodeImpl();
    }

    protected Ext extConstraintNodeImpl() {
        return extNodeImpl();
    }

    protected Ext extCanonicalConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extAuthConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extAutoEndorseConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extCallerConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extActsForConstraintNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extLabelActsForPrincipalConstraintNodeImpl() {
        return extActsForConstraintNode();
    }

    protected Ext extLabelActsForLabelConstraintNodeImpl() {
        return extActsForConstraintNode();
    }

    protected Ext extPrincipalActsForPrincipalConstraintNodeImpl() {
        return extActsForConstraintNode();
    }

    protected Ext extLabelLeAssertionNodeImpl() {
        return extConstraintNode();
    }

    protected Ext extDeclassifyStmtImpl() {
        return extStmtImpl();
    }

    protected Ext extDeclassifyExprImpl() {
        return extExprImpl();
    }

    protected Ext extEndorseStmtImpl() {
        return extStmtImpl();
    }

    protected Ext extCheckedEndorseStmtImpl() {
        return extEndorseStmtImpl();
    }

    protected Ext extEndorseExprImpl() {
        return extExprImpl();
    }

    protected Ext extNewLabelImpl() {
        return extLabelExprImpl();
    }

    protected Ext extLabelExprImpl() {
        return extExprImpl();
    }

    protected Ext extPrincipalExprImpl() {
        return extExprImpl();
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

    protected Ext postExtAmbProviderLabelNode(Ext e) {
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

    protected Ext postExtAutoEndorseConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtCallerConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtActsForConstraintNode(Ext e) {
        return postExtConstraintNode(e);
    }

    protected Ext postExtLabelActsForPrincipalConstraintNode(Ext e) {
        return postExtActsForConstraintNode(e);
    }

    protected Ext postExtLabelActsForLabelConstraintNode(Ext e) {
        return postExtActsForConstraintNode(e);
    }

    protected Ext postExtPrincipalActsForPrincipalConstraintNode(Ext e) {
        return postExtActsForConstraintNode(e);
    }

    protected Ext postExtLabelLeAssertionNode(Ext e) {
        return postExtConstraintNode(e);
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
