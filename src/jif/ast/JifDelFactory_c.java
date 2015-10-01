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
import polyglot.ast.JLDel;

/**
 * Constructs Jif delegates
 **/
public class JifDelFactory_c extends AbstractDelFactory_c
        implements JifDelFactory {

    protected JifDelFactory_c() {
        super();
    }

    @Override
    protected JLDel delAssignImpl() {
        return new JifAssignDel();
    }

    @Override
    protected JLDel delArrayAccessImpl() {
        return new JifArrayAccessDel();
    }

    @Override
    protected JLDel delArrayAccessAssignImpl() {
        return new JifArrayAccessAssignDel();
    }

    @Override
    protected JLDel delArrayInitImpl() {
        return new JifArrayInitDel();
    }

    @Override
    protected JLDel delBinaryImpl() {
        return new JifBinaryDel();
    }

    @Override
    protected JLDel delCallImpl() {
        return new JifCallDel();
    }

    @Override
    protected JLDel delCastImpl() {
        return new JifCastDel();
    }

    @Override
    protected JLDel delCatchImpl() {
        return new JifCatchDel();
    }

    @Override
    protected JLDel delClassDeclImpl() {
        return new JifClassDeclDel();
    }

    @Override
    protected JLDel delFieldDeclImpl() {
        return new JifFieldDeclDel();
    }

    @Override
    protected JLDel delFieldImpl() {
        return new JifFieldDel();
    }

    @Override
    protected JLDel delFieldAssignImpl() {
        return new JifFieldAssignDel();
    }

    @Override
    protected JLDel delFormalImpl() {
        return new JifFormalDel();
    }

    @Override
    protected JLDel delIfImpl() {
        return new JifIfDel();
    }

    @Override
    protected JLDel delInitializerImpl() {
        return new JifInitializerDel();
    }

    @Override
    protected JLDel delInstanceofImpl() {
        return new JifInstanceOfDel();
    }

    @Override
    protected JLDel delLocalDeclImpl() {
        return new JifLocalDeclDel();
    }

    @Override
    protected JLDel delNewImpl() {
        return new JifNewDel();
    }

    @Override
    protected JLDel delNewArrayImpl() {
        return new JifNewArrayDel();
    }

    @Override
    protected JLDel delThrowImpl() {
        return new JifThrowDel();
    }

    @Override
    protected JLDel delTypeNodeImpl() {
        return new JifTypeNodeDel();
    }

    @Override
    protected JLDel delConstructorCallImpl() {
        return new JifConstructorCallDel();
    }

    @Override
    protected JLDel delMethodDeclImpl() {
        return new JifMethodDeclDel();
    }

    @Override
    protected JLDel delConstructorDeclImpl() {
        return new JifProcedureDeclDel();
    }

    @Override
    public final JLDel delAmbNewArray() {
        JLDel e = delAmbNewArrayImpl();

        if (nextDelFactory() != null
                && nextDelFactory() instanceof JifDelFactory) {
            JLDel e2 = ((JifDelFactory) nextDelFactory()).delAmbNewArray();
            e = composeDels(e, e2);
        }

        return postDelAmbNewArray(e);
    }

    @Override
    public final JLDel delLabelExpr() {
        JLDel e = delLabelExprImpl();

        if (nextDelFactory() != null
                && nextDelFactory() instanceof JifDelFactory) {
            JLDel e2 = ((JifDelFactory) nextDelFactory()).delLabelExpr();
            e = composeDels(e, e2);
        }
        return postDelLabelExpr(e);
    }

    @Override
    public final JLDel delNewLabel() {
        JLDel e = delNewLabelImpl();

        if (nextDelFactory() != null
                && nextDelFactory() instanceof JifDelFactory) {
            JLDel e2 = ((JifDelFactory) nextDelFactory()).delLabelExpr();
            e = composeDels(e, e2);
        }
        return postDelNewLabel(e);
    }

    protected JLDel delAmbNewArrayImpl() {
        return delAmbExprImpl();
    }

    protected JLDel postDelAmbNewArray(JLDel e) {
        return postDelAmbExpr(e);
    }

    protected JLDel delLabelExprImpl() {
        return delExprImpl();
    }

    protected JLDel postDelLabelExpr(JLDel e) {
        return postDelExpr(e);
    }

    protected JLDel delNewLabelImpl() {
        return delLabelExprImpl();
    }

    protected JLDel postDelNewLabel(JLDel e) {
        return postDelLabelExpr(e);
    }

}
