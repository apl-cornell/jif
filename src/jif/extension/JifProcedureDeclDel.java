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
    public Context enterScope(Context c, NodeVisitor v) {
        c = super.enterScope(c, v);
        ProcedureDecl pd = (ProcedureDecl) node();
        for (Iterator i = pd.formals().iterator(); i.hasNext(); ) {
            Formal f = (Formal) i.next();
            c.addVariable(f.localInstance());
        }

        return c;
    }

    /**
     * Check exceptions thrown by the call. We override the normal method in
     * order to prevent updating the node (and thus overriding the labeled throw
     * types), which is done in {@link polyglot.ext.jl.ast.Term_c Term_c}.
     */
    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
        TypeSystem ts = ec.typeSystem();

        List throwTypes = null;

        if (node() instanceof MethodDecl) {
            MethodDecl md = (MethodDecl)node();
            throwTypes = md.throwTypes();
        } else {
            throwTypes = ((ConstructorDecl)node()).throwTypes();
        }

        SubtypeSet s = ec.throwsSet();

        for (Iterator i = s.iterator(); i.hasNext();) {
            Type t = (Type)i.next();

            boolean throwDeclared = false;

            if (!t.isUncheckedException()) {
                for (Iterator j = throwTypes.iterator(); j.hasNext();) {
                    TypeNode tn = (TypeNode)j.next();
                    Type tj = tn.type();

                    if (ts.isSubtype(t, tj)) {
                        throwDeclared = true;
                        break;
                    }
                }

                if (!throwDeclared) {
                    ec.throwsSet().clear();
                    Position pos = ec.exceptionPosition(t);
                    throw new SemanticException("The exception \"" + t + 
                        "\" must either be caught or declared to be thrown.",
                        pos==null?node().position():pos);
                }
            }
        }

        ec.throwsSet().clear();

        return node();
    }
}
