package jif.extension;

import jif.ast.JifNodeFactory;
import jif.ast.JifUtil;
import jif.ast.LabelExpr;
import jif.ast.PrincipalExpr;
import jif.types.JifContext;
import jif.types.JifMethodInstance;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPath;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ast.Binary.Operator;
import polyglot.frontend.goals.Disambiguated;
import static polyglot.ast.Binary.GE;
import static polyglot.ast.Binary.LE;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.visit.SupertypeDisambiguator;
import polyglot.visit.TypeChecker;

public class JifBinaryDel extends JifJL_c
{
    // ambiguous operators
    public static final Binary.Operator EQUIV    = new Operator("equiv", Precedence.RELATIONAL);
    public static final Binary.Operator TRUST_GE = new Operator("≽",     Precedence.RELATIONAL);
    // also LE, GE
    
    // disambiguous operators
    public static final Operator RELABELS_TO     = new Operator("flowsto",    Precedence.RELATIONAL);
    public static final Operator ACTSFOR         = new Operator("actsfor",    Precedence.RELATIONAL);
    public static final Operator AUTHORIZES      = new Operator("authorizes", Precedence.RELATIONAL);
    public static final Operator ENFORCES        = new Operator("enforces",   Precedence.RELATIONAL);
    public static final Operator PRINCIPAL_EQUIV = new Operator("(principal) equiv", Precedence.RELATIONAL);
    public static final Operator LABEL_EQUIV     = new Operator("(label) equiv",     Precedence.RELATIONAL);

    public JifBinaryDel() { }

    /**
     * As a side-effect of typechecking, the various label and principal
     * comparisons are disambiguated (for example x ≽ y is translated to
     * x actsfor y or x enforces y.
     * 
     * @see #disambiguateRelations(JifTypeSystem)
     */
    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        Binary b = node();
        JifNodeFactory nf = (JifNodeFactory)tc.nodeFactory();
        JifTypeSystem  ts = (JifTypeSystem)tc.typeSystem();

        b = disambiguateRelations(ts);
        
        if (b.operator() == RELABELS_TO || b.operator() == LABEL_EQUIV) {
            LabelExpr lhs = checkLabelExpr(ts, nf, tc, b.left());
            LabelExpr rhs = checkLabelExpr(ts, nf, tc, b.right());

            return b.left(lhs).right(rhs).type(ts.Boolean());
        }

        if (b.operator() == ACTSFOR || b.operator() == PRINCIPAL_EQUIV) {
            checkPrincipalExpr(ts, nf, tc, b.left());
            checkPrincipalExpr(ts, nf, tc, b.right());
            
            return b.type(ts.Boolean());
        }
        
        if (b.operator() == AUTHORIZES) {
            LabelExpr lhs = checkLabelExpr(ts, nf, tc, node().left());
            checkPrincipalExpr(ts, nf, tc, node().right());
            
            return b.left(lhs).type(ts.Boolean());
        }
        
        if (b.operator() == ENFORCES) {
            checkPrincipalExpr(ts, nf, tc, node().left());
            LabelExpr rhs = checkLabelExpr(ts, nf, tc, node().right());

            return b.right(rhs).type(ts.Boolean());
        }
        
        // Note: at this point, b.node() must be equal to node() even though
        // b has been disambiguated, because disambiguation only modifies the
        // operator in one of the new cases handled by this method; normal java
        // operators are unchanged.
        
        return super.typeCheck(tc);
    }

    /**
     * Label and principal comparisons are translated to static method calls.
     * This  function returns the appropriate type information for the generated
     * calls.  Assumes that the operator is unambiguous.
     *
     * @param o
     *          an unambiguous operator.
     * @return
     *          type information for a static two-argument method, or null if
     *          the operator is not a label comparison.  
     */
    public static JifMethodInstance equivalentMethod(JifTypeSystem ts, Operator o) {
        // new unambiguous operators
        if (o == RELABELS_TO)
            return ts.relabelsToMethod();
        if (o == ACTSFOR)
            return ts.actsForMethod();
        if (o == AUTHORIZES)
            return ts.authorizesMethod();
        if (o == ENFORCES)
            return ts.enforcesMethod();
        if (o == PRINCIPAL_EQUIV)
            return ts.principalEquivMethod();
        if (o == LABEL_EQUIV)
            return ts.labelEquivMethod();
        
        // ambiguous operators
        if (o == EQUIV || o == TRUST_GE)
            throw new InternalCompilerError("Expected unambiguous operator");
        
        // java operators 
        return null;
    }
    
    /**
     * This uses type information to specify which version of an overloaded
     * operator (>=, <=, equiv, or ≽) is intended, and returns an updated node.
     * Assumes that <code>left()</code> and <code>right()</code> have types.  
     * 
     * @return
     *         a copy of node() having an unambiguous operator()
     * 
     * @throws SemanticException
     *         if the expression is invalid
     */
    protected Binary disambiguateRelations(JifTypeSystem ts) throws SemanticException {
        
        // the left (l) and right (r) types are either
        // a principal (p), a label (l), or neither (n).
        // thus for example, if l(eft) is a p(rincipal), then lp is true.
        
        boolean lp = ts.isImplicitCastValid(node().left().type(), ts.Principal());
        boolean ll = ts.isImplicitCastValid(node().left().type(), ts.Label());
        boolean ln = !lp && !ll;
        String left = lp ? "principal" :
                      ll ? "label"     :
                           "numeric expression";
        
        boolean rp = ts.isImplicitCastValid(node().right().type(), ts.Principal());
        boolean rl = ts.isImplicitCastValid(node().right().type(), ts.Label());
        boolean rn = !rp && !rl;
        String right = rp ? "principal" :
                       rl ? "label"     :
                            "numeric expression";
        
        Operator result = node().operator();
        
        if (node().operator() == GE) {
                 if (lp && rl) result = ENFORCES;        // p >= l
            else if (ll && rp) result = AUTHORIZES;      // l >= p
            else if (lp && rp) result = ACTSFOR;         // p >= p
            else if (ln && rn) result = GE;              // n >= n
            else throw new SemanticException(">= cannot be used to compare a " + left + " with a " + right);
        }
        
        else if (node().operator() == LE) {
                 if (ll && rl) result = RELABELS_TO;     // l <= l
            else if (ln && rn) result = LE;              // n <= n
            else throw new SemanticException("<= cannot be used to compare a " + left + " with a " + right);
        }
        
        else if (node().operator() == EQUIV) {
                 if (lp && rp) result = PRINCIPAL_EQUIV; // p equiv p
            else if (ll && rl) result = LABEL_EQUIV;     // l equiv l
            else throw new SemanticException("\"equiv\" can only be used to compare principals or labels");
        }
        
        else if (node().operator() == TRUST_GE) {
                 if (lp && rp) result = ACTSFOR;         // p ≽ p
            else if (ll && rp) result = AUTHORIZES;      // l ≽ p
            else if (lp && rl) result = ENFORCES;        // p ≽ l
            else throw new SemanticException("≽ can only be used to compare principals and labels");
        }
        
        return node().operator(result);
    }
    
    /**
     * Convert the given expression to a LabelExpr by wrapping it if necessary.
     *  
     * @throws SemanticException
     *          if the expression is not a valid label expression (i.e. a final
     *          access path or constant) 
     */
    protected LabelExpr checkLabelExpr(JifTypeSystem ts, JifNodeFactory nf, TypeChecker tc, Expr e) throws SemanticException {
        if (e instanceof LabelExpr) {
            return (LabelExpr) e;
        }
        else if (JifUtil.isFinalAccessExprOrConst(ts, e)) {
            Label l = JifUtil.exprToLabel(ts, e, (JifContext)tc.context());
            e = nf.LabelExpr(e.position(), l);
            return (LabelExpr) e.visit(tc);
        }
        else {
            throw new SemanticException(
                    "An expression used in a label test must be either a final access path, principal parameter or a constant principal",
                    e.position());
        }
    }
    
    /**
     * Checks that the given expression is a valid runtime-represented principal
     * expression.  Assumes e has a non-null canonical type.
     *
     * @throws SemanticException
     *          if the expression is not a valid runtime-representable principal
     *          expression (i.e. a final access path or constant)
     */
    protected void checkPrincipalExpr(JifTypeSystem ts, JifNodeFactory nf, TypeChecker tc, Expr e) throws SemanticException {

        if (e instanceof PrincipalExpr)
            return;
        
        if (e.type() == null)
            throw new InternalCompilerError(
                "Expected type-checked node.",
                e.position());
            
        if (!e.type().isCanonical())
            throw new InternalCompilerError(
                "Expected canonical type.",
                e.position());
        
        if (!ts.isImplicitCastValid(e.type(), ts.Principal()))
            throw new SemanticException(
                "Only principal expressions may be used in an actsfor check",
                e.position());

        if (!JifUtil.isFinalAccessExprOrConst(ts, e)) {
            // illegal dynamic principal. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap = JifUtil.exprToAccessPath(e, (JifContext)tc.context()); 
            ap.verify((JifContext)tc.context());

            // previous line should throw an exception, but throw this just to
            // be safe.
            throw new SemanticDetailedException(
                "Illegal dynamic principal.",
                "Only final access paths or principal expressions can be used as a dynamic principal. " +
                "A final access path is an expression starting with either \"this\" or a final " +
                "local variable \"v\", followed by zero or more final field accesses. That is, " +
                "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a " +
                "final local variables, and each field f1 to fn is a final field. A principal expression " +
                "is either a principal parameter, or an external principal.",
                e.position());                                        
        }

        Principal p = JifUtil.exprToPrincipal(ts, e, (JifContext)tc.context());
        if (!p.isRuntimeRepresentable()) {
            throw new SemanticDetailedException(
                "A principal used in an actsfor must be runtime-representable.",                    
                "Both principals used in an actsfor test must be " +
                "represented at runtime, since the actsfor test is a dynamic " +
                "test. The principal " + p + 
                " is not represented at runtime, and thus cannot be used " +
                "in an actsfor test.",
                e.position());
        }
        
        // success
        return;
    }

    @Override
    public Binary node() {
        return (Binary) super.node();
    }
}
