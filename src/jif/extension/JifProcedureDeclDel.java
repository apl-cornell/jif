package jif.extension;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;
import polyglot.visit.ExceptionChecker;

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

    /** 
    * Check exceptions thrown by the call. 
    * XXX
    */
    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
        TypeSystem ts = ec.typeSystem();

        List throwTypes = null;
        String name;

        if (node() instanceof MethodDecl) {
            MethodDecl md = (MethodDecl)node();
            throwTypes = md.throwTypes();
            name = md.name();
        } else {
            throwTypes = ((ConstructorDecl)node()).throwTypes();
            name = "constructor";
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
