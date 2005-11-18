package jif.extension;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;

/** The Jif delegate the <code>ProcedureDecl</code> node. 
 * 
 *  @see  polyglot.ast.ProcedureDecl
 */
public class JifProcedureDeclDel extends JifJL_c
{
    public JifProcedureDeclDel() { }


    // add the formals to the context before visiting the formals
    public Context enterScope(Context c) {
        c = super.enterScope(c);
        ProcedureDecl pd = (ProcedureDecl) node();
        for (Iterator i = pd.formals().iterator(); i.hasNext(); ) {
            Formal f = (Formal) i.next();
            c.addVariable(f.localInstance());
        }

        return c;
    }
}
