package jif.extension;

import jif.types.JifContext;
import polyglot.types.Context;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>ConstructorCall</code> node. 
 * 
 *  @see polyglot.ast.ConstructorCall
 */
public class JifConstructorCallDel extends JifJL_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifConstructorCallDel() {
    }

    /* (non-Javadoc)
     * @see polyglot.ast.NodeOps#enterScope(polyglot.types.Context)
     */
    @Override
    public Context enterScope(Context c) {
        return ((JifContext) c).pushConstructorCall();
    }
}
