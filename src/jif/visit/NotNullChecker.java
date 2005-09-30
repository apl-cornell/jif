package jif.visit;

import java.util.*;

import jif.ast.*;
import jif.extension.*;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.PreciseClassChecker.AccessPath;
import jif.visit.PreciseClassChecker.AccessPathLocal;
import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.visit.*;

/**
 * Visitor which determines at which program points local variables and
 * final fields of this class cannot be
 * null, and thus field access and method calls to them cannot produce
 * NullPointerExceptions. This information is then stored in the appropriate
 * delegates. 
 */
public class NotNullChecker extends DataFlow
{
    public NotNullChecker(Job job, TypeSystem ts, NodeFactory nf) {
	super(job, ts, nf, true /* forward analysis */);
        EDGE_KEY_NPE = null; 
    }

    private FlowGraph.ExceptionEdgeKey EDGE_KEY_NPE;
    
    public NodeVisitor begin() {
        EDGE_KEY_NPE = new FlowGraph.ExceptionEdgeKey(typeSystem().NullPointerException());                
        return super.begin();
            
    }

    public Item createItem(FlowGraph graph, Term n) {
        return new DataFlowItem();
    }

    static class DataFlowItem extends Item {
        // contains objects of type VarInstance that are not null
        Set notNullAccessPaths;

        DataFlowItem() {
            notNullAccessPaths = new HashSet();
        }
        DataFlowItem(Set notNullAccessPaths) {
            this.notNullAccessPaths = notNullAccessPaths;
        }
        DataFlowItem(DataFlowItem d) {
            notNullAccessPaths = new HashSet(d.notNullAccessPaths);
        }
        static boolean exprIsNotNullStatic(Expr e) {
            // expression is not null if it is a "new" expression, "this"
            // or if it is a cast of a non-null expression.
            return (e instanceof New ) || 
		(e instanceof NewArray ) ||
		(e instanceof ArrayInit ) ||
		(e instanceof Special ) ||
		(e instanceof Lit && !(e instanceof NullLit)) ||
		(e instanceof Cast && exprIsNotNullStatic(((Cast)e).expr())) ||
		(e instanceof DeclassifyExpr && exprIsNotNullStatic(((DeclassifyExpr)e).expr()));
        }        
        boolean exprIsNotNull(Expr e) {
            // expression is not null if it is a "new" expression,
            // or if it is an accesspath that is contained in 
            // notNullVariables, or if it is a cast of a 
            // non-null expression.
            AccessPath ap = PreciseClassChecker.findAccessPathForExpr(e);
            return exprIsNotNullStatic(e) ||
                    (ap != null && notNullAccessPaths.contains(ap)) ||
                    (e instanceof Cast && exprIsNotNull(((Cast)e).expr())) ||
                    (e instanceof DeclassifyExpr && exprIsNotNull(((DeclassifyExpr)e).expr()));
        }        

        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return this.notNullAccessPaths == ((DataFlowItem)o).notNullAccessPaths || 
                       this.notNullAccessPaths.equals(((DataFlowItem)o).notNullAccessPaths);
            }
            return false;
        }
        public int hashCode() {
            return notNullAccessPaths.hashCode();
        }
        public String toString() {
            return "[nn access paths: " + notNullAccessPaths + "]";
        }
    }


    /**
     * Create an initial Item for the dataflow analysis. By default, the 
     * set of not null variables is empty.
     */
    protected Item createInitialItem(FlowGraph graph, Term node) {
        return new DataFlowItem();
    }

    protected Map flow(List inItems, List inItemKeys, FlowGraph graph, Term n, Set edgeKeys) {
        return this.flowToBooleanFlow(inItems, inItemKeys, graph, n, edgeKeys);
    }

    /**
     * If a local variable is initialized with a non-null expression, then
     * the variable is not null. If a local variable is assigned non-null
     * expression then the variable is not null; if a local variable is assigned
     * a possibly null expression, then the local variable is possibly null.
     */
    public Map flow(Item trueItem, Item falseItem, Item otherItem, FlowGraph graph, Term n, Set succEdgeKeys) {
        DataFlowItem dfIn = (DataFlowItem)safeConfluence(trueItem, FlowGraph.EDGE_KEY_TRUE, 
                                     falseItem, FlowGraph.EDGE_KEY_FALSE,
                                     otherItem, FlowGraph.EDGE_KEY_OTHER,
                                     n, graph);

        if (n instanceof LocalDecl) {
            LocalDecl x = (LocalDecl)n;
            if (dfIn.exprIsNotNull(x.init())) {                
                Set s = addNotNull(dfIn.notNullAccessPaths, new AccessPathLocal(x.localInstance()));
                DataFlowItem newItem = new DataFlowItem(s);
                return checkNPE(itemToMap(newItem, succEdgeKeys), n);
            }
        }
        else if (n instanceof Formal) {
            Formal f = (Formal)n;
            JifFormalDel d = (JifFormalDel)n.del();
            if (d.isCatchFormal()) {
                // f is a formal in a catch block (e.g., 
                // try {...} catch(Exception e) {...} )
                // and as such is never null
                Set s = addNotNull(dfIn.notNullAccessPaths, new AccessPathLocal(f.localInstance()));
                DataFlowItem newItem = new DataFlowItem(s);
                return checkNPE(itemToMap(newItem, succEdgeKeys), n);
            }
        }
        else if (n instanceof Instanceof) {
            Instanceof io = (Instanceof)n;
            AccessPath ap = PreciseClassChecker.findAccessPathForExpr(io.expr());
            if (ap != null) {                        
                // on the true branch of an instanceof, we know that
                // the local (or final field of this) is not null
                // e.g., if (o instanceof String) { /* o is not null */ }
                Set trueBranch = addNotNull(dfIn.notNullAccessPaths, ap);
                return itemsToMap(new DataFlowItem(trueBranch), 
                                  dfIn, dfIn, succEdgeKeys);
            }
        }
        else if (n instanceof Assign) {
            Assign x = (Assign)n; 
            AccessPath ap = PreciseClassChecker.findAccessPathForExpr(x.left());
            if (ap != null && x.operator() == Assign.ASSIGN) {
                Set s = killAccessPath(dfIn.notNullAccessPaths, ap);
                if (dfIn.exprIsNotNull(x.right())) {
                    s = addNotNull(s, ap);
                }
                DataFlowItem newItem = new DataFlowItem(s);
                return checkNPE(itemToMap(newItem, succEdgeKeys), n);
            }
        }
        else if (n instanceof Binary && 
                (Binary.EQ.equals(((Binary)n).operator()) || 
                    Binary.NE.equals(((Binary)n).operator()))) {
            Binary b = (Binary)n;
            // b is an == or != expression
            if (b.left() instanceof NullLit || b.right() instanceof NullLit) {                
                // b is a comparison to null                
                // e is the expression being compared with null.
                Expr e = (b.left() instanceof NullLit) ? b.right() : b.left();
                
                Map m = comparisonToNull(e, Binary.EQ.equals(b.operator()), dfIn, succEdgeKeys);
                return checkNPE(m, n);
            }                        
        }
        else if (n instanceof Expr && ((Expr)n).type().isBoolean() && 
                (n instanceof Binary || n instanceof Unary)) {
            if (trueItem == null) trueItem = dfIn;
            if (falseItem == null) falseItem = dfIn;
            
            Map ret = flowBooleanConditions(trueItem, falseItem, dfIn, graph, (Expr)n, succEdgeKeys);
            if (ret == null) {
                ret = itemToMap(dfIn, succEdgeKeys);
            }
            return checkNPE(ret, n); 
        } 

        return checkNPE(itemToMap(dfIn, succEdgeKeys), n);
    }
    
    /**
     * This method improves the analysis of the not null checking, by examining
     * the map that is to be returned. If the map contains an ExceptionEdgeKey
     * for a Null Pointer Exception, (meaning that the node can throw a
     * Null Pointer Exception), then we can conclude that on all other flows
     * from the node, any local variables that were accessed must be non-null.
     * For example:
     * <pre>
     *   int foo(Object o) {
     *     o.bar();               // line 1
     *     o.quux();              // line 2
     *   }
     * </pre>
     * If the control flow enters line 2, then we know that o is not null, as
     * otherwise a NullPointerException would have been thrown at line 1.
     * 
     * @param m the Map from ExceptionEdgeKeys to DataFlowItems to be returned
     * @param node the node we are calculating flow for.
     * @return a Map such that if m contains an ExceptionEdgeKey
     *          for a Null Pointer Exception, then all local variables accessed
     *          by node are added to the notNullVars set of all other edges 
     *          out of the node.
     */
    private Map checkNPE(Map m, Term node) {
        if (node instanceof Field || node instanceof Call) {
            Receiver r;
            if (node instanceof Field) {
                r = ((Field)node).target();
            }
            else {
                r = ((Call)node).target();
            }
            AccessPath ap = null;
            if (r instanceof Expr) ap = PreciseClassChecker.findAccessPathForExpr((Expr)r);
            
            if (ap != null && m.get(EDGE_KEY_NPE) != null) {
                // the receiver is an expression we track, and there is an edge for a null
                // pointer exception! This means that if the local is null,
                // the NPE edge will be taken, meaning all other flows can have
                // the access path ap added to their notNullVars set.                
                Map newMap = new HashMap();
                for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry e = (Map.Entry)i.next();
                    if (e.getKey().equals(EDGE_KEY_NPE)) {
                        newMap.put(e.getKey(), e.getValue());
                    }
                    else {
                        DataFlowItem dfi = (DataFlowItem)e.getValue();
                        if (dfi.notNullAccessPaths.contains(ap)) {
                            newMap.put(e.getKey(), dfi);                            
                        }
                        else {
                            Set s = addNotNull(dfi.notNullAccessPaths, ap);
                            newMap.put(e.getKey(), new DataFlowItem(s));                                                        
                        }
                    }
                }
                return newMap;
            }
        }
        return m;
    }
    
    
    /**
     * Utility method used to produce appropriate
     * DataFlowItems when expressions that compare local variables to null
     * are evaluated to true and false.
     */
    private static Map comparisonToNull(Expr expr, boolean equalsEquals, DataFlowItem in, Set edgeKeys) {
        AccessPath ap = PreciseClassChecker.findAccessPathForExpr(expr);
        if (ap != null) {                        
            Set sEq = killAccessPath(in.notNullAccessPaths, ap);
            Set sNeq = addNotNull(in.notNullAccessPaths, ap);

            if (equalsEquals) {
                return itemsToMap(new DataFlowItem(sEq), new DataFlowItem(sNeq), in, edgeKeys);
            }
            else {
                return itemsToMap(new DataFlowItem(sNeq), new DataFlowItem(sEq), in, edgeKeys);
            }                
        }
        return itemToMap(in, edgeKeys);
    }        
    
    private static Set killAccessPath(Set paths, AccessPath ap) {
        Set p = new HashSet(paths);
        boolean changed = p.remove(ap);
        if (ap instanceof AccessPathLocal) {
            // go through the set and remove any access paths rooted at this local
            for (Iterator iter = p.iterator(); iter.hasNext(); ) {
                AccessPath v = (AccessPath)iter.next();
                if (ap.equals(v.findRoot())) {
                    iter.remove();
                    changed = true;
                }
            }
            
        }
        return changed?p:paths;
    }

    private static Set addNotNull(Set notNullAccessPaths, AccessPath ap) {
        Set s = new HashSet(notNullAccessPaths);
        s.add(ap);
        return s;
    }

    
    /**
     * The confluence operator is intersection: a variable is not null only
     * if it is not null on all paths flowing in. 
     */
    protected Item confluence(List items, Term node, FlowGraph graph) {
        return intersect(items);
    }
        
    /**
     * Utility method takes the intersection of a List of DataFlowItems,
     * by intersecting all of their notNullVars sets.
     */
    private static DataFlowItem intersect(List items) {
        // take the intersection of all the not null variable sets of the
        // DataFlowItems
        Set intersectSet = null;
        for (int i = 0; i < items.size(); i++) {
            Set m = ((DataFlowItem)items.get(i)).notNullAccessPaths;
            if (intersectSet == null ) {
                intersectSet = new HashSet(m);
            }
            else {
                intersectSet.retainAll(m);
            }
        }

        if (intersectSet == null) intersectSet = Collections.EMPTY_SET;
        return new DataFlowItem(intersectSet);
        
    }

    
    /**
     * "Check" the nodes of the graph for the not null analysis. This actually
     * consists of setting various "not null" flags in the Jif extensions to
     * nodes, so that their exceptionCheck methods can decide whether to 
     * suppress the NullPointerExceptions that they would otherwise declare
     * would be thrown.
     */
    protected void check(FlowGraph graph, Term n, Item inItem, Map outItems) throws SemanticException {
        if (n instanceof Assign) {
            if (n instanceof FieldAssign) {
                checkField((Field)((Assign)n).left(), (DataFlowItem)inItem);
            }
            else if (n instanceof ArrayAccessAssign) {
                checkArrayAccess((ArrayAccess)((Assign)n).left(), (DataFlowItem)inItem);                
            }
        }
        else if (n instanceof Field) {
            checkField((Field)n, (DataFlowItem)inItem);
        }
        else if (n instanceof Call) {
            Call c = (Call)n;
            Receiver r = c.target();
            checkReceiver(r, n, (DataFlowItem)inItem);
            // also check the type of the method instance container
            checkType(c.methodInstance().container(), (DataFlowItem)inItem);
        }
        else if (n instanceof Throw) {
            Throw t = (Throw)n;
            if ((inItem != null && ((DataFlowItem)inItem).exprIsNotNull(t.expr())) 
		|| (inItem == null && DataFlowItem.exprIsNotNullStatic(t.expr()))) {
                // The object thrown by this throw statement can never be
                // null, e.g. it is a new expression, or it is a variable
                // that is never null.
                ((JifThrowDel)t.del()).setThrownIsNeverNull();                
            }
        }
        else if (n instanceof ArrayAccess) {
            checkArrayAccess((ArrayAccess)n, (DataFlowItem)inItem);
        }
        else if (n instanceof LabelExpr) {
            checkLabelExpr((LabelExpr)n, (DataFlowItem)inItem);
        }
        else if (n instanceof PrincipalNode) {
            checkPrincipalNode((PrincipalNode)n, (DataFlowItem)inItem);
        }
        else if (n instanceof Cast) {
            checkTypeNode(((Cast)n).castType(), (DataFlowItem)inItem);
        }
        else if (n instanceof Instanceof) {
            checkTypeNode(((Instanceof)n).compareType(), (DataFlowItem)inItem);
        }
        else if (n instanceof New) {
            checkTypeNode(((New)n).objectType(), (DataFlowItem)inItem);
        }
    }    
    
    private void checkField(Field f, DataFlowItem inItem) {
        checkReceiver(f.target(), f, inItem);
    }
    private void checkReceiver(Receiver r, Term n, DataFlowItem inItem) {
        if (r instanceof Expr) {
            Expr e = (Expr)r;
            boolean neverNull = false; 
            if ((inItem != null && inItem.exprIsNotNull(e)) ||
                (inItem == null && DataFlowItem.exprIsNotNullStatic(e))) {        
                // the receiver is not null
                neverNull = true;
            }
            if (n instanceof Field) {
                ((JifFieldDel)n.del()).setTargetIsNeverNull(neverNull);                    
            }
            else {
                ((JifCallDel)n.del()).setTargetIsNeverNull(neverNull);                    
            }
        }
    }
    private void checkArrayAccess(ArrayAccess a, DataFlowItem inItem) {
        if (inItem.exprIsNotNull(a.array())) {
            // The array accessed by this array access statement can never be
            // null, e.g. it is a new expression, or it is a variable
            // that is never null.
            ((JifArrayAccessDel)a.del()).setArrayIsNeverNull();                
        }        
    }
    private void checkLabelExpr(LabelExpr e, DataFlowItem inItem) {
        Label l = e.label().label();
        
        LabelNotNullSubst lnns = new LabelNotNullSubst(inItem);
        try {
            l.subst(lnns);
        }
        catch (SemanticException se) {
            throw new InternalCompilerError("Unexpected SemanticException", se);
        }
    }
    private void checkPrincipalNode(PrincipalNode pn, DataFlowItem inItem) {
        LabelNotNullSubst lnns = new LabelNotNullSubst(inItem);
        try {
            pn.principal().subst(lnns);
        }
        catch (SemanticException se) {
            throw new InternalCompilerError("Unexpected SemanticException", se);
        }
    }

    private void checkTypeNode(TypeNode tn, DataFlowItem inItem) {
        checkType(tn.type(), inItem);
    }
    private void checkType(Type t, DataFlowItem inItem) {
        if (t instanceof JifSubstType && ((JifTypeSystem)ts).isParamsRuntimeRep(t)) {            
            LabelNotNullSubst lnns = new LabelNotNullSubst(inItem);
            JifSubstType jst = (JifSubstType)t;
            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                if (arg instanceof Label) {
                    Label L = (Label)arg;
                    try {
                        L.subst(lnns);
                    }
                    catch (SemanticException se) {
                        throw new InternalCompilerError("Unexpected SemanticException", se);
                    }
                    
                }
                else if (arg instanceof Principal) {
                    Principal p = (Principal)arg;
                    try {
                        p.subst(lnns);
                    }
                    catch (SemanticException se) {
                        throw new InternalCompilerError("Unexpected SemanticException", se);
                    }
                }
                else {
                    throw new InternalCompilerError("Unexpected type for entry: "
                                                    + arg.getClass().getName());
                }
            }            
        }
    }
    
    private class LabelNotNullSubst extends LabelSubstitution {
        DataFlowItem inItem;
        LabelNotNullSubst(DataFlowItem inItem) {
            this.inItem = inItem;
        }
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof DynamicLabel) {
                DynamicLabel dl = (DynamicLabel)L;
                checkPath(dl.path());                
            }
            return L;
        }
        public Principal substPrincipal(Principal p) throws SemanticException {
            if (p instanceof DynamicPrincipal) {
                DynamicPrincipal dp = (DynamicPrincipal)p;
                checkPath(dp.path());                
            }
            return p;
        }
        private void checkPath(jif.types.label.AccessPath p) {
            while (p instanceof AccessPathField) {
                AccessPath ap = labelAccessPathToDFAccessPath(p);
                AccessPathField apf = (AccessPathField)p;
                p = apf.path(); 

                if (ap != null && inItem.notNullAccessPaths.contains(ap)) {
                    apf.setIsNeverNull();
                }
            }
            if (p instanceof jif.types.label.AccessPathLocal) {
                AccessPath ap = labelAccessPathToDFAccessPath(p);
                jif.types.label.AccessPathLocal apl = (jif.types.label.AccessPathLocal)p;
                if (inItem.notNullAccessPaths.contains(ap)) {
                    apl.setIsNeverNull();                    
                }                    
            }            
        }

        private AccessPath labelAccessPathToDFAccessPath(jif.types.label.AccessPath p) {
            if (p instanceof jif.types.label.AccessPathLocal) {
                return new AccessPathLocal(((jif.types.label.AccessPathLocal)p).localInstance());
                
            }
            else if (p instanceof jif.types.label.AccessPathThis) {
                return new PreciseClassChecker.AccessPathThis();                
            }
            else if (p instanceof jif.types.label.AccessPathClass) {
                return new PreciseClassChecker.AccessPathClass(((AccessPathClass)p).type());                                
            }
            else if (p instanceof AccessPathField) {
                AccessPathField apf = (AccessPathField)p;
                AccessPath target = labelAccessPathToDFAccessPath(apf.path());
                if (target != null && apf.fieldInstance().flags().isFinal()) {
                    return new PreciseClassChecker.AccessPathFinalField(target, apf.fieldInstance());
                }
            }
            return null;
        }
    }
}
