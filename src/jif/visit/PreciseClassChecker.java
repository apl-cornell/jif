package jif.visit;

import java.util.*;
import java.util.Map.Entry;

import jif.extension.JifPreciseClassDel;
import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.visit.*;

/**
 * Visitor which determines at which program points local variables and
 * final fields of this class cannot be
 * null, and thus field access and method calls to them cannot produce
 * NullPointerExceptions. This information is then stored in the appropriate
 * delegates. 
 */
public class PreciseClassChecker extends DataFlow
{
    public PreciseClassChecker(Job job, TypeSystem ts, NodeFactory nf) {
	super(job, ts, nf, true /* forward analysis */);
        EDGE_KEY_CLASS_CAST_EXC = null; 
    }

    private FlowGraph.ExceptionEdgeKey EDGE_KEY_CLASS_CAST_EXC;
    
    public NodeVisitor begin() {
        EDGE_KEY_CLASS_CAST_EXC = new FlowGraph.ExceptionEdgeKey(typeSystem().ClassCastException());                
        return super.begin();
            
    }

    public Item createItem(FlowGraph graph, Term n) {
        return new DataFlowItem();
    }

    static class DataFlowItem extends Item {
        // Maps VarInstances of Sets of ClassTypes
        Map classTypes;

        DataFlowItem() {
            classTypes = new HashMap();
        }
        DataFlowItem(Map classTypes) {
            this.classTypes = classTypes;
        }
        DataFlowItem(DataFlowItem d) {
            classTypes = new HashMap(d.classTypes);
        }
        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return this.classTypes == ((DataFlowItem)o).classTypes || 
                       this.classTypes.equals(((DataFlowItem)o).classTypes);
            }
            return false;
        }
        public int hashCode() {
            return classTypes.hashCode();
        }
        public String toString() {
            return "[" + classTypes + "]";
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

        if (n instanceof Instanceof) {
            Instanceof io = (Instanceof)n;
            Expr e = io.expr();
            VarInstance vi = findVarInstance(e);
            if (vi != null) {                        
                // on the true branch of an instanceof, we know that
                // the var instance is in fact an instance of the compare type.
                Map trueBranch = addClass(dfIn.classTypes, vi, io.compareType().type());
                Map m = itemsToMap(new DataFlowItem(trueBranch), 
                                  dfIn, dfIn, succEdgeKeys);
                return m;
            }
        }
        else if (n instanceof Cast) {
            Cast cst = (Cast)n;
            Expr ex = cst.expr();
            VarInstance vi = findVarInstance(ex);
            if (vi != null) {                        
                // on the non-ClassCastException edges, we know that
                // the cast succeeded, and var instance is in fact an 
                // instance of the cast type.
                Map m = itemToMap(dfIn, succEdgeKeys);
                for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry e = (Map.Entry)i.next();
                    if (!e.getKey().equals(EDGE_KEY_CLASS_CAST_EXC)) {
                        DataFlowItem df = (DataFlowItem)e.getValue();
                        e.setValue(new DataFlowItem(addClass(df.classTypes, vi, cst.castType().type())));
                    }
                }
                return m;
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
            return ret; 
        } 
        return itemToMap(dfIn, succEdgeKeys);
    }
    
    
    private Map addClass(Map map, VarInstance vi, Type type) {        
        if (!type.isClass()) {
            // don't bother adding this type.
            return map;
        }
        Map m = new HashMap(map);
        Set s = (Set)m.get(vi);
        if (s == null) {
            s = new HashSet();
        }
        else {
            s = new HashSet(s);
        }
        m.put(vi, s);
        s.add(type);
        return m;
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
     * by intersecting all of their classTypes sets.
     */
    private static DataFlowItem intersect(List items) {
        // take the intersection of all the not null variable sets of the
        // DataFlowItems, by examining the smallest one

        // find the smallest Map.
        Map smallest = null;

        for (int i = 0; i < items.size(); i++) {
            Map candidate = ((DataFlowItem)items.get(i)).classTypes;
            if (candidate.isEmpty()) {
                // any intersection will be empty.
                return new DataFlowItem(Collections.EMPTY_MAP);
            }
            if (smallest == null || smallest.size() > candidate.size()) {
                smallest = candidate;
            }
        }
        
        Map intersectMap = new HashMap(smallest);
        for (int i = 0; i < items.size(); i++) {
            Map m = ((DataFlowItem)items.get(i)).classTypes;
            // go through the entries of intersectMap
            for (Iterator iter = intersectMap.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry e = (Entry)iter.next();
                if (m.containsKey(e.getKey())) {
                    Set s = new HashSet((Set)e.getValue());
                    Set t = (Set)m.get(e.getKey());
                    s.retainAll(t); // could be more precise here, a la SubtypeSet
                }
                else {
                    // no entry for the set, the intersection is empty, so remove
                    // the key.
                    iter.remove();
                }
            }
        }

        return new DataFlowItem(intersectMap);
    }

    
    /**
     * "Check" the nodes of the graph for the precise class analysis. This actually
     * consists of setting the preciseClass field in the Jif extensions to
     * nodes, so that their exceptionCheck methods can decide whether to 
     * suppress the ClassCastExceptions that they would otherwise declare
     * would be thrown.
     */
    protected void check(FlowGraph graph, Term n, Item inItem, Map outItems) throws SemanticException {
        if (n.del() instanceof JifPreciseClassDel) {
            DataFlowItem dfi = (DataFlowItem)inItem;
            JifPreciseClassDel jpcd = (JifPreciseClassDel)n.del();
            VarInstance vi = findVarInstance(jpcd.getPreciseClassExpr());
            if (vi != null) {
                jpcd.setPreciseClass((Set)dfi.classTypes.get(vi));
            }            
        }
    }

    private VarInstance findVarInstance(Expr expr) {
        if (expr instanceof Local) {
            return ((Local)expr).localInstance();
        }
        if (expr instanceof Field) {
            Field f = (Field)expr;
            if (wantField(f)) {
                return f.fieldInstance();
            }
        }
        return null;
    }

    /**
     * Do we want to keep track of this field? Only if it is a final field
     * of "this"
     */
    private boolean wantField(Field f) {
        return (f.target() instanceof Special && f.flags().isFinal());
    }        
}
