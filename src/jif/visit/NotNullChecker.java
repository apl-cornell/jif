package jif.visit;

import java.util.*;

import jif.extension.JifArrayAccessDel;
import jif.extension.JifCallDel;
import jif.extension.JifFieldDel;
import jif.extension.JifThrowDel;
import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.VarInstance;
import polyglot.visit.DataFlow;
import polyglot.visit.FlowGraph;
import polyglot.visit.NodeVisitor;

/**
 * Visitor which determines at which program points local variables cannot be
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

    /**
     * If a local variable is initialized with a non-null expression, then
     * the variable is not null. If a local variable is assigned non-null
     * expression then the variable is not null; if a local variable is assigned
     * a possibly null expression, then the local variable is possibly null.
     */
    public Map flow(Item in, FlowGraph graph, Term n, Set succEdgeKeys) {
        DataFlowItem dfIn = (DataFlowItem)in;
        if (n instanceof LocalDecl) {
            LocalDecl x = (LocalDecl)n;
            if (((DataFlowItem)in).exprIsNotNull(x.init())) {                
                Set s = new HashSet(dfIn.notNullVars);
                s.add(x.localInstance());
                DataFlowItem newItem = new DataFlowItem(s);
                return checkNPE(itemToMap(newItem, succEdgeKeys), n);
            }
        }
        else if (n instanceof Assign) {
            Assign x = (Assign)n; 
            if (x.left() instanceof Local) {
                if (((DataFlowItem)in).exprIsNotNull(x.right())) {
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
        else if (n instanceof Expr && super.hasTrueFalseBranches(succEdgeKeys)) {
            // we have a condition, i.e. a branch on an expression.
            Expr e = (Expr)n;
            if (e.type().isBoolean()) {
                return checkNPE(constructItemsFromCondition(e, 
                                     in, 
                                     succEdgeKeys, 
                                     navigator), 
                                n);
            }            
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
     * This object is a subclass of ConditionNavigator that implements the
     * combine and handleExpression methods for the Not Null analysis. 
     * In particular, combining DataFlowItems consists of unioning their 
     * notNullVars sets; handling expressions consists of treating comparisons
     * to null appropriately.
     */
    private static final ConditionNavigator navigator = 
        new ConditionNavigator() {
            public Item combine(Item item1, Item item2) {
                Set s = new HashSet(((DataFlowItem)item1).notNullVars);
                s.addAll(((DataFlowItem)item2).notNullVars);
                return new DataFlowItem(s);
            }

            public BoolItem handleExpression(Expr expr, Item startingItem) {
                if (expr instanceof Binary) {
                    Binary b = (Binary)expr;
                    if (Binary.EQ.equals(b.operator()) || 
                        Binary.NE.equals(b.operator())) {
                        
                        // b is an == or != expression
                        if (b.left() instanceof NullLit ||
                            b.right() instanceof NullLit) {
                            
                            // b is a comparison to null
                            
                            // e is the expression being 
                            // compared with null.
                            Expr e = (b.left() instanceof NullLit) ? b.right() : b.left();

                            return comparisonToNull(e, 
                                                    Binary.EQ.equals(b.operator()),
                                                    (DataFlowItem)startingItem);
                            
                        }                        
                    }
                }
                return new BoolItem(startingItem, startingItem);
            }            
    };

    /**
     * Utility method used by the ConditionNavigator to produce appropriate
     * DataFlowItems when expressions that compare local variables to null
     * are evaluated to true and false.
     */
    private static BoolItem comparisonToNull(Expr expr, boolean equalsEquals, DataFlowItem in) {
        if (expr instanceof Local) {                        
            Set sEq = new HashSet(in.notNullVars);
            Set sNeq = new HashSet(in.notNullVars);

            sEq.remove(((Local)expr).localInstance());
            sNeq.add(((Local)expr).localInstance());

            if (equalsEquals) {
                return new BoolItem(new DataFlowItem(sEq), 
                                    new DataFlowItem(sNeq));
            }
            else {
                return new BoolItem(new DataFlowItem(sNeq), 
                                    new DataFlowItem(sEq));
            }                
        }
        return new BoolItem(in, in);
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
        if (((DataFlowItem)inItem).exprIsNotNull(a.array())) {
            // The array accessed by this array access statement can never be
            // null, e.g. it is a new expression, or it is a variable
            // that is never null.
            ((JifArrayAccessDel)a.del()).setArrayIsNeverNull();                
        }        
    }
}
