package jif.extension;

import java.util.List;

import jif.ast.JifProcedureDecl;
import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.Assertion;
import jif.types.AuthConstraint;
import jif.types.CallerConstraint;
import jif.types.JifProcedureInstance;
import jif.types.principal.Principal;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.ProcedureDeclOps;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

/** The Jif delegate the <code>ProcedureDecl</code> node. 
 * 
 *  @see  polyglot.ast.ProcedureDecl
 */
public class JifProcedureDeclDel extends JifDel_c implements ProcedureDeclOps {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifProcedureDeclDel() {
    }

    // add the formals to the context before visiting the formals
    @Override
    public Context enterScope(Context c) {
        c = super.enterScope(c);
        addFormalsToScope(c);
        return c;
    }

    protected void addFormalsToScope(Context c) {
        ProcedureDecl pd = (ProcedureDecl) node();
        for (Formal f : pd.formals()) {
            c.addVariable(f.localInstance());
        }
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifProcedureDecl pd = (JifProcedureDecl) super.typeCheck(tc);
        JifProcedureInstance jpi =
                (JifProcedureInstance) pd.procedureInstance();
        for (Assertion a : jpi.constraints()) {
            if (a instanceof AuthConstraint) {
                AuthConstraint ac = (AuthConstraint) a;
                ensureNotTopPrincipal(ac.principals(), a.position());
            } else if (a instanceof CallerConstraint) {
                CallerConstraint cc = (CallerConstraint) a;
                ensureNotTopPrincipal(cc.principals(), a.position());
            } else if (a instanceof ActsForConstraint) {
                @SuppressWarnings("unchecked")
                ActsForConstraint<ActsForParam, ActsForParam> afc =
                        (ActsForConstraint<ActsForParam, ActsForParam>) a;

                ActsForParam actor = afc.actor();
                ActsForParam granter = afc.granter();

                if (actor instanceof Principal) {
                    ensureNotTopPrincipal((Principal) actor, a.position());
                }

                if (granter instanceof Principal) {
                    ensureNotTopPrincipal((Principal) granter, a.position());
                }
            }
        }
        return pd;
    }

    protected void ensureNotTopPrincipal(List<Principal> principals,
            Position pos) throws SemanticException {
        for (Principal p : principals) {
            ensureNotTopPrincipal(p, pos);
        }
    }

    protected void ensureNotTopPrincipal(Principal p, Position pos)
            throws SemanticException {
        if (p.isTopPrincipal()) {
            throw new SemanticException("The top principal " + p
                    + " cannot appear in a constraint.", pos);
        }
    }

    @Override
    public void prettyPrintHeader(Flags flags, CodeWriter w, PrettyPrinter tr) {
        // XXX Should refactor to separate Del functionality out of JifClassDecl.
        ((ProcedureDeclOps) node()).prettyPrintHeader(flags, w, tr);
    }

}
