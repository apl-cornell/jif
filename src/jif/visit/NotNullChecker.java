package jif.visit;

import java.util.*;

import jif.ast.LabelExpr;
import jif.extension.*;
import jif.types.LabelSubstitution;
import jif.types.label.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
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
        Set notNullVars;

        DataFlowItem() {
            notNullVars = new HashSet();
        }
        DataFlowItem(Set notNullVars) {
            this.notNullVars = notNullVars;
        }
        DataFlowItem(DataFlowItem d) {
            notNullVars = new HashSet(d.notNullVars);
        }
        static boolean exprIsNotNullStatic(Expr e) {
            // expression is not null if it is a "new" expression,
            // or if it is a cast of a non-null expression.
            return (e instanceof New ) || 
		(e instanceof NewArray ) ||
		(e instanceof ArrayInit ) ||
		(e instanceof Lit && !(e instanceof NullLit)) ||
		(e instanceof Cast && exprIsNotNullStatic(((Cast)e).expr()));
        }        
        boolean exprIsNotNull(Expr e) {
            // expression is not null if it is a "new" expression,
            // or if it is a VarInstance that is contained in 
            // notNullVariables, or if it is a cast of a 
        // non-null expression.
            return exprIsNotNullStatic(e) ||
                (e instanceof Local && notNullVars.contains(((Local)e).localInstance())) ||
                (e instanceof Field && ((Field)e).target() instanceof Special &&
                        ((Field)e).fieldInstance().flags().isFinal() &&
                        notNullVars.contains(((Field)e).fieldInstance())) ||
        (e instanceof Cast && exprIsNotNull(((Cast)e).expr()));
        }        

        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return this.notNullVars == ((DataFlowItem)o).notNullVars || 
                       this.notNullVars.equals(((DataFlowItem)o).notNullVars);
            }
            return false;
        }
        public int hashCode() {
            return notNullVars.hashCode();
        }
        public String toString() {
            return "[nn vars: " + notNullVars + "]";
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
                Set s = new HashSet(dfIn.notNullVars);
                s.add(x.localInstance());
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
                Set s = new HashSet(dfIn.notNullVars);
                s.add(f.localInstance());
                DataFlowItem newItem = new DataFlowItem(s);
                return checkNPE(itemToMap(newItem, succEdgeKeys), n);
            }
        }
        else if (n instanceof Instanceof) {
            Instanceof io = (Instanceof)n;
            Expr e = io.expr();
            VarInstance vi = null;
            if (e instanceof Local) {
                vi = ((Local)e).localInstance();
            }
            else if (e instanceof Field && 
                       ((Field)e).target() instanceof Special &&
                       ((Field)e).fieldInstance().flags().isFinal()) {
                vi = ((Field)e).fieldInstance();
            }
            if (vi != null) {                        
                // on the true branch of an instanceof, we know that
                // the local (or final field of this) is not null
                // e.g., if (o instanceof String) { /* o is not null */ }
                Set trueBranch = new HashSet(dfIn.notNullVars);

                trueBranch.add(vi);

                return itemsToMap(new DataFlowItem(trueBranch), 
                                  dfIn, dfIn, succEdgeKeys);
            }
        }
        else if (n instanceof Assign) {
            Assign x = (Assign)n; 
            if (x.left() instanceof Local) {
                if (dfIn.exprIsNotNull(x.right())) {
                    Set s = new HashSet(dfIn.notNullVars);
                    s.add(((Local)x.left()).localInstance());
                    DataFlowItem newItem = new DataFlowItem(s);
                    return checkNPE(itemToMap(newItem, succEdgeKeys), n);
                }
                else {
                    Set s = new HashSet(dfIn.notNullVars); 
                    s.remove(((Local)x.left()).localInstance());
                    DataFlowItem newItem = new DataFlowItem(s);
                    return checkNPE(itemToMap(newItem, succEdgeKeys), n);
                }
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
        if (m==null) {Thread.dumpStack();}
        if (node instanceof Field || node instanceof Call) {
            Receiver r;
            if (node instanceof Field) {
                r = ((Field)node).target();
            }
            else {
                r = ((Call)node).target();
            }
            
            if (r instanceof Local && m.get(EDGE_KEY_NPE) != null) {
                // the receiver is a local, and there is an edge for a null
                // pointer exception! This means that if the local is null,
                // the NPE edge will be taken, meaning all other flows can have
                // the local added to their notNullVars set.

                VarInstance v = ((Local)r).localInstance();
                Map newMap = new HashMap();
                for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry e = (Map.Entry)i.next();
                    if (e.getKey().equals(EDGE_KEY_NPE)) {
                        newMap.put(e.getKey(), e.getValue());
                    }
                    else {
                        DataFlowItem dfi = (DataFlowItem)e.getValue();
                        if (dfi.notNullVars.contains(v)) {
                            newMap.put(e.getKey(), dfi);                            
                        }
                        else {
                            Set s = new HashSet(dfi.notNullVars);
                            s.add(v);
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
        VarInstance vi = null;
        if (expr instanceof Local) {
            vi = ((Local)expr).localInstance();
        }
        else if (expr instanceof Field && 
                   ((Field)expr).target() instanceof Special &&
                   ((Field)expr).fieldInstance().flags().isFinal()) {
            vi = ((Field)expr).fieldInstance();
        }
        if (vi != null) {                        
            Set sEq = new HashSet(in.notNullVars);
            Set sNeq = new HashSet(in.notNullVars);

            sEq.remove(vi);
            sNeq.add(vi);

            if (equalsEquals) {
                return itemsToMap(new DataFlowItem(sEq), new DataFlowItem(sNeq), in, edgeKeys);
            }
            else {
                return itemsToMap(new DataFlowItem(sNeq), new DataFlowItem(sEq), in, edgeKeys);
            }                
        }
        return itemToMap(in, edgeKeys);
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
        // DataFlowItems, by examining the smallest one

        // find the index of the smallest set.
        int smallestSize = -1, 
            smallestIndex = 0;

        for (int i = 0; i < items.size(); i++) {
            int size = ((DataFlowItem)items.get(i)).notNullVars.size();
            if (size == 0) {
                // any intersection will be empty.
                return new DataFlowItem(Collections.EMPTY_SET);
            }
            if (smallestSize < 0 || size < smallestSize) {
                smallestIndex = i;
            }
        }

        List setsOfVars = new ArrayList(items.size()-1);
        Set smallestSet = null;
        for (int i = 0; i < items.size(); i++) {
            if (i == smallestIndex) {
                smallestSet = ((DataFlowItem)items.get(i)).notNullVars;
            }
            else {
                setsOfVars.add(((DataFlowItem)items.get(i)).notNullVars);
            }
        }


        Set intersect = new HashSet();

        Iterator iter = smallestSet.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            boolean allContain = true;
            for (Iterator i = setsOfVars.iterator(); i.hasNext(); ) {
                if (!((Set)i.next()).contains(o)) {
                    allContain = false;
                    break;
                }
            }
            if (allContain) {
                intersect.add(o);
            }
        }

        return new DataFlowItem(intersect);
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
            Receiver r = ((Call)n).target();
            checkReceiver(r, n, (DataFlowItem)inItem);
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
    }    
    
    private void checkField(Field f, DataFlowItem inItem) {
        checkReceiver(f.target(), f, inItem);
    }
    private void checkReceiver(Receiver r, Term n, DataFlowItem inItem) {
        if (r instanceof Expr) {
            Expr e = (Expr)r;
            if ((inItem != null && inItem.exprIsNotNull(e)) ||
                (inItem == null && DataFlowItem.exprIsNotNullStatic(e))) {        
                // the receiver is not null
                if (n instanceof Field) {
                    ((JifFieldDel)n.del()).setTargetIsNeverNull();                    
                }
                else {
                    ((JifCallDel)n.del()).setTargetIsNeverNull();                    
                }
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
        private void checkPath(AccessPath p) {
            while (p instanceof AccessPathField) {
                AccessPathField apf = (AccessPathField)p;
                FieldInstance fi = apf.fieldInstance();
                p = apf.path(); 

                if (fi.flags().isFinal() && p instanceof AccessPathThis &&
                        inItem.notNullVars.contains(fi)) {
                    apf.setIsNeverNull();
                }
            }
            if (p instanceof AccessPathLocal) {
                AccessPathLocal apl = (AccessPathLocal)p;
                if (inItem.notNullVars.contains(apl.localInstance())) {
                    apl.setIsNeverNull();                    
                }                    
            }            
        }
    }
}
