package jif.ast;

import jif.extension.JifArrayAccessAssignDel;
import jif.extension.JifArrayAccessDel;
import jif.extension.JifArrayInitDel;
import jif.extension.JifAssignDel;
import jif.extension.JifBinaryDel;
import jif.extension.JifCallDel;
import jif.extension.JifCastDel;
import jif.extension.JifCatchDel;
import jif.extension.JifClassDeclDel;
import jif.extension.JifConstructorCallDel;
import jif.extension.JifFieldAssignDel;
import jif.extension.JifFieldDeclDel;
import jif.extension.JifFieldDel;
import jif.extension.JifFormalDel;
import jif.extension.JifIfDel;
import jif.extension.JifInitializerDel;
import jif.extension.JifInstanceOfDel;
import jif.extension.JifLocalDeclDel;
import jif.extension.JifMethodDeclDel;
import jif.extension.JifNewArrayDel;
import jif.extension.JifNewDel;
import jif.extension.JifProcedureDeclDel;
import jif.extension.JifThrowDel;
import jif.extension.JifTypeNodeDel;
import polyglot.ast.AbstractDelFactory_c;
import polyglot.ast.JL;

/**
 * Constructs Jif delegates
 **/
public class JifDelFactory_c extends AbstractDelFactory_c implements
        JifDelFactory {

    protected JifDelFactory_c() {
        super();
    }

    @Override
    protected JL delAssignImpl() {
        return new JifAssignDel();
    }

    @Override
    protected JL delArrayAccessImpl() {
        return new JifArrayAccessDel();
    }

    @Override
    protected JL delArrayAccessAssignImpl() {
        return new JifArrayAccessAssignDel();
    }

    @Override
    protected JL delArrayInitImpl() {
        return new JifArrayInitDel();
    }

    @Override
    protected JL delBinaryImpl() {
        return new JifBinaryDel();
    }

    @Override
    protected JL delCallImpl() {
        return new JifCallDel();
    }

    @Override
    protected JL delCastImpl() {
        return new JifCastDel();
    }

    @Override
    protected JL delCatchImpl() {
        return new JifCatchDel();
    }

    @Override
    protected JL delClassDeclImpl() {
        return new JifClassDeclDel();
    }

    @Override
    protected JL delFieldDeclImpl() {
        return new JifFieldDeclDel();
    }

    @Override
    protected JL delFieldImpl() {
        return new JifFieldDel();
    }

    @Override
    protected JL delFieldAssignImpl() {
        return new JifFieldAssignDel();
    }

    @Override
    protected JL delFormalImpl() {
        return new JifFormalDel();
    }

    @Override
    protected JL delIfImpl() {
        return new JifIfDel();
    }

    @Override
    protected JL delInitializerImpl() {
        return new JifInitializerDel();
    }

    @Override
    protected JL delInstanceofImpl() {
        return new JifInstanceOfDel();
    }

    @Override
    protected JL delLocalDeclImpl() {
        return new JifLocalDeclDel();
    }

    @Override
    protected JL delNewImpl() {
        return new JifNewDel();
    }

    @Override
    protected JL delNewArrayImpl() {
        return new JifNewArrayDel();
    }

    @Override
    protected JL delThrowImpl() {
        return new JifThrowDel();
    }

    @Override
    protected JL delTypeNodeImpl() {
        return new JifTypeNodeDel();
    }

    @Override
    protected JL delConstructorCallImpl() {
        return new JifConstructorCallDel();
    }

    @Override
    protected JL delMethodDeclImpl() {
        return new JifMethodDeclDel();
    }

    @Override
    protected JL delConstructorDeclImpl() {
        return new JifProcedureDeclDel();
    }

    @Override
    public final JL delAmbNewArray() {
        JL e = delAmbNewArrayImpl();

        if (nextDelFactory() != null
                && nextDelFactory() instanceof JifDelFactory) {
            JL e2 = ((JifDelFactory) nextDelFactory()).delAmbNewArray();
            e = composeDels(e, e2);
        }

        return postDelAmbNewArray(e);
    }

    @Override
    public final JL delLabelExpr() {
        JL e = delLabelExprImpl();

        if (nextDelFactory() != null
                && nextDelFactory() instanceof JifDelFactory) {
            JL e2 = ((JifDelFactory) nextDelFactory()).delLabelExpr();
            e = composeDels(e, e2);
        }
        return postDelLabelExpr(e);
    }

    @Override
    public final JL delNewLabel() {
        JL e = delNewLabelImpl();

        if (nextDelFactory() != null
                && nextDelFactory() instanceof JifDelFactory) {
            JL e2 = ((JifDelFactory) nextDelFactory()).delLabelExpr();
            e = composeDels(e, e2);
        }
        return postDelNewLabel(e);
    }

    protected JL delAmbNewArrayImpl() {
        return delAmbExprImpl();
    }

    protected JL postDelAmbNewArray(JL e) {
        return postDelAmbExpr(e);
    }

    protected JL delLabelExprImpl() {
        return delExprImpl();
    }

    protected JL postDelLabelExpr(JL e) {
        return postDelExpr(e);
    }

    protected JL delNewLabelImpl() {
        return delLabelExprImpl();
    }

    protected JL postDelNewLabel(JL e) {
        return postDelLabelExpr(e);
    }

}
