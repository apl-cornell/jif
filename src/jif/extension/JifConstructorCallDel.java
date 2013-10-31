package jif.extension;

import jif.types.JifContext;
import polyglot.ast.Expr;
import polyglot.ast.New;
import polyglot.ast.Special;
import polyglot.types.Context;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>ConstructorCall</code> node. 
 * 
 *  @see polyglot.ast.ConstructorCall
 */
public class JifConstructorCallDel extends JifDel_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    /**
     * This flag records whether the target of a field access is never
     * null. This flag is by default false, but may be set to true by the
     * dataflow analysis performed by jif.visit.NotNullChecker
     */
    private boolean isQualNeverNull = false;

    /**
     * Since the CFG may visit a node more than once, we need to take the
     * OR of all values set.
     */
    private boolean qualNeverNullAlreadySet = false;

    /**
     * This flag records if an NPE is fatal due to fail-on-exception.
     */
    private boolean isNPEfatal = false;

    public void setQualifierIsNeverNull(boolean neverNull) {
        if (!qualNeverNullAlreadySet) {
            isQualNeverNull = neverNull;
        } else {
            isQualNeverNull = isQualNeverNull && neverNull;
        }
        qualNeverNullAlreadySet = true;
    }

    public boolean qualIsNeverNull() {
        Expr r = ((New) node()).qualifier();
        return (r instanceof Special || isNPEfatal || isQualNeverNull);
    }

    /* (non-Javadoc)
     * @see polyglot.ast.NodeOps#enterScope(polyglot.types.Context)
     */
    @Override
    public Context enterScope(Context c) {
        return ((JifContext) c).pushConstructorCall();
    }
}
