package jif.visit;

import java.util.*;
import java.util.Map.Entry;

import polyglot.ast.*;
import polyglot.ast.Binary.Operator;
import polyglot.frontend.Job;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.DataFlow;
import polyglot.visit.FlowGraph;

/**
 * This class finds integral bounds on expressions. It uses that information to
 * determine whether it is impossible for certain exceptions to be thrown.
 */
public class IntegerBoundsChecker extends DataFlow
{
    public IntegerBoundsChecker(Job job) {
        this(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory());
    }

    public IntegerBoundsChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf, true /* forward analysis */);
    }

    /**
     * Create an initial Item for the dataflow analysis. By default, the 
     * map of integer bounds is empty.
     */
    protected Item createInitialItem(FlowGraph graph, Term node) {
        return new DataFlowItem();
    }
    protected Map flow(List inItems, List inItemKeys, FlowGraph graph, Term n, Set edgeKeys) {
        return this.flowToBooleanFlow(inItems, inItemKeys, graph, n, edgeKeys);
    }

    private static Set<Operator> INTERESTING_BINARY_OPERATORS = new HashSet<Operator>(Arrays.asList(
                                                                                new Binary.Operator[] {
                                                                                        Binary.EQ,
                                                                                        Binary.LE,
                                                                                        Binary.LT,
                                                                                        Binary.GE,
                                                                                        Binary.GT,
                                                                                } ));
    public Map flow(Item trueItem, Item falseItem, Item otherItem, FlowGraph graph, Term n, Set succEdgeKeys) {
        Item inItem = safeConfluence(trueItem, FlowGraph.EDGE_KEY_TRUE, 
                                     falseItem, FlowGraph.EDGE_KEY_FALSE,
                                     otherItem, FlowGraph.EDGE_KEY_OTHER,
                                     n, graph);
        System.out.println(inItem);

        DataFlowItem inDFItem = ((DataFlowItem)inItem);
        Map<LocalInstance, Bounds> updates = new HashMap<LocalInstance, Bounds>(); 
        
        if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl)n;
            if (ld.init() != null) {
                addBound(updates, ld.localInstance(), false, ld.init());
                addBound(updates, ld.init(), false, ld.localInstance());
            }
        }
        else if (n instanceof LocalAssign) {
            LocalAssign la = (LocalAssign)n;
            addBound(updates, la.left(), false, la.right());
            addBound(updates, la.right(), false, (Local)la.left());
        }
        else if (n instanceof Unary) {
            Unary u = (Unary)n;
            if (u.expr() instanceof Local) {
                Local l = (Local)u.expr();
                if (u.operator().equals(Unary.POST_INC) || u.operator().equals(Unary.PRE_INC) ||
                        u.operator().equals(Unary.POST_DEC) || u.operator().equals(Unary.PRE_DEC)) {
                    // x = x + 1, or x = x -1 , therefore x < x
                    addBound(updates, l, true, l);
                }
            }
        }
        else if (n instanceof Binary && ((Binary)n).left().type().isNumeric() &&  
                INTERESTING_BINARY_OPERATORS.contains(((Binary)n).operator())) {
            Binary b = (Binary)n;
            Expr left = b.left();
            Expr right = b.right();
            if (b.operator().equals(Binary.LT)) {
                addBound(updates, left, true, right);
            }
            if (b.operator().equals(Binary.LE) || b.operator().equals(Binary.EQ)) {
                addBound(updates, left, false, right);                
            }
            if (b.operator().equals(Binary.GT)) {
                addBound(updates, right, true, left);                                                
            }
            if (b.operator().equals(Binary.GE) || b.operator().equals(Binary.EQ)) {
                addBound(updates, right, false, left);                                
            }
        }
        
        DataFlowItem outDFItem = inDFItem.update(updates);
        if (n instanceof Expr && ((Expr)n).type().isBoolean() && 
                (n instanceof Binary || n instanceof Unary)) {
            if (trueItem == null) trueItem = outDFItem;
            if (falseItem == null) falseItem = outDFItem;
            Map m = flowBooleanConditions(trueItem, falseItem, outDFItem, graph, (Expr)n, succEdgeKeys);
            if (m != null) return m;
        } 

        return itemToMap(outDFItem, succEdgeKeys);
    }

    private void addBound(Map<LocalInstance, Bounds> updates, Object left, boolean strict, Expr right) {
        if (right instanceof Local) {
            addBound(updates, left, strict, ((Local)right).localInstance());
        }
    }
    
    /**
     * Add a bound. If strict is true, then it is left < rli. If strict is false, it is
     * left <= rli.
     */
    private void addBound(Map<LocalInstance, Bounds> updates, Object left, boolean strict, LocalInstance rli) {
        Set<LocalInstance> liLowerBounds = Collections.emptySet();
        Long lnum = null;
        if (left instanceof LocalInstance) {
            liLowerBounds = Collections.singleton((LocalInstance)left);
        }
        else if (left instanceof Expr) {
            liLowerBounds = findLocalInstanceLowerBounds((Expr)left);
            lnum = findLowerNumericBound((Expr)left, null);
        }
        if (liLowerBounds.isEmpty() && lnum == null) return;
        
        Bounds b = updates.get(rli);
        if (b == null) {
            b = new Bounds();
            updates.put(rli, b);
        }
        for (Iterator<LocalInstance> iter = liLowerBounds.iterator(); iter.hasNext();) {
            LocalInstance lli = iter.next();
            b.bounds.add(new LocalBound(strict, lli));            
        }

        if (lnum != null) {
            if (b.numericBound == null || b.numericBound.longValue() < lnum.longValue()) {
                b.numericBound = lnum;
            }
        }
    }
    /**
     * Returns the set of LocalInstances that are (non-strict) lower bounds on the expression
     */
    private Set<LocalInstance> findLocalInstanceLowerBounds(Expr expr) {
        if (expr instanceof Local) {
            return Collections.singleton(((Local)expr).localInstance());
        }
        if (expr instanceof Unary) {
            Unary u = (Unary)expr;
            if (u.operator().equals(Unary.PRE_INC) || u.operator().equals(Unary.POST_INC)) {
                return findLocalInstanceLowerBounds(u.expr());
            }
        }
        if (expr instanceof Conditional) {
            Conditional c = (Conditional)expr;
            Set<LocalInstance> con = findLocalInstanceLowerBounds(c.consequent());
            Set alt = findLocalInstanceLowerBounds(c.alternative());
            // return the intersection of con and alt.
            Set<LocalInstance> result = new HashSet<LocalInstance>(con);
            result.retainAll(alt);
            return result;
        }
        if (expr instanceof Binary) {
            Binary b = (Binary)expr;
            if (b.operator().equals(Binary.ADD)) {
                Set<LocalInstance> left = findLocalInstanceLowerBounds(b.left());                
                Set<LocalInstance> right = findLocalInstanceLowerBounds(b.right());                
                Long leftNum = findLowerNumericBound(b.left(), null);
                Long rightNum = findLowerNumericBound(b.right(), null);
                if (leftNum != null && leftNum.longValue() >= -1) {
                    return right;
                }
                if (rightNum != null && rightNum.longValue() >= -1) {
                    return left;
                }
            }
        }
        if (expr instanceof Assign) {
            Assign a = (Assign)expr;
            if (a.left() instanceof Local) {
                return Collections.singleton(((Local)a.left()).localInstance());
            }
        }
        return Collections.emptySet();
    }

    public void check(FlowGraph graph, Term n, Item inItem, Map outItems) throws SemanticException {
        DataFlowItem dfIn = (DataFlowItem)inItem;
        if (n instanceof Expr && ((Expr)n).type().isNumeric()) {
            System.err.println("bound for " + n + " : " + findLowerNumericBound((Expr)n, dfIn));
        }
        // !@!
    }

    protected Item confluence(List items, Term node, FlowGraph graph) {
        Map<LocalInstance, Bounds> newMap = new HashMap<LocalInstance, Bounds>();
        for (Iterator iter = items.iterator(); iter.hasNext();) {
            DataFlowItem df = (DataFlowItem)iter.next();
            for (Iterator iterator = df.lowerBounds.keySet().iterator(); iterator.hasNext();) {
                LocalInstance li = (LocalInstance)iterator.next();
                Bounds b = df.lowerBounds.get(li);
                if (newMap.containsKey(li)) {
                    b = mergeBounds(newMap.get(li), b);
                    
                }
                if (b != null) newMap.put(li, b);
            }
        }
        return new DataFlowItem(newMap);
    }
    
    protected static Bounds mergeBounds(Bounds b0, Bounds b1) {
        if (b0.bounds.containsAll(b1.bounds)) {
            if (b0.numericBound == b1.numericBound || b1.numericBound == null ||
                    (b0.numericBound != null && b0.numericBound.longValue() <= b1.numericBound.longValue())) {
                // merging would do nothing. Save some memory.
                return b0;
            }
        }
        
        Bounds b = new Bounds(null, new HashSet<Bound>());
        b.bounds.addAll(b0.bounds);
        b.bounds.addAll(b1.bounds);
        b.numericBound = b0.numericBound;
        if (b.numericBound == null || (b1.numericBound != null && b.numericBound > b1.numericBound)) {
            b.numericBound = b1.numericBound;
        }
        return b;
    }
    
    
    private Long findLowerNumericBound(LocalInstance li, DataFlowItem df) {
        return findLowerNumericBound(li, df, new HashSet<LocalInstance>());
    }
    
    /**
     * Finds the greatest lower bound B it can for li, such that B < li
     */
    private Long findLowerNumericBound(LocalInstance li, DataFlowItem df, Set<LocalInstance> seen) {
        if (df == null) return null;
        if (seen.contains(li)) return null;
        seen.add(li);        
        
        Bounds b = df.lowerBounds.get(li);
        if (b == null) return null;
        Long num = b.numericBound;
        for (Iterator iter = b.bounds.iterator(); iter.hasNext();) {
            LocalBound lb = (LocalBound)iter.next();
            Long lbb = findLowerNumericBound(lb.li, df, seen);
            if (lbb != null && (num == null || num.longValue() < lbb.longValue())) {
                num = lbb;
            }
        }
        b.numericBound = num;
        return num;
    }
    /**
     * Finds the greatest lower bound B it can for expr, such that B < expr
     */
    private Long findLowerNumericBound(Expr expr, DataFlowItem df) {
        if (!expr.type().isNumeric()) return null;
        if (expr instanceof Local) {
            LocalInstance li = ((Local)expr).localInstance();
            return findLowerNumericBound(li, df);
        }
        if (expr.isConstant()) {
            return Long.valueOf(((Number)expr.constantValue()).longValue() - 1); // needs to be a strict bound
        }
        if (expr instanceof Unary) {
            Unary u = (Unary)expr;
            Long b = findLowerNumericBound(u.expr(), df);
            if (b == null) return null;
            if (u.operator().equals(Unary.POST_DEC) || u.operator().equals(Unary.POST_INC)) {
                return b;
            }
            if (u.operator().equals(Unary.PRE_DEC)) {
                return Long.valueOf(b.longValue() - 1);
            }
            if (u.operator().equals(Unary.PRE_INC)) {
                return Long.valueOf(b.longValue() + 1);
            }
        }
        if (expr instanceof Conditional) {
            Conditional c = (Conditional)expr;
            Long con = findLowerNumericBound(c.consequent(), df);
            if (con != null) {
                Long alt = findLowerNumericBound(c.alternative(), df);
                if (alt != null) {
                    // return the min of them
                    return (con.longValue() < alt.longValue()) ? con : alt;
                }
            }
        }
        if (expr instanceof Binary) {
            Binary b = (Binary)expr;
            if (b.operator().equals(Binary.ADD)) {
                Long left = findLowerNumericBound(b.left(), df);
                Long right = findLowerNumericBound(b.right(), df);
                if (left != null && right != null) {
                    // leftB < left, rightB < right, so leftB + rightB + 1 < left + right 
                    return Long.valueOf(left.longValue() + right.longValue() + 1);
                }
            }
        }
        if (expr instanceof Assign) {
            Assign a = (Assign)expr;
            return findLowerNumericBound(a.right(), df);
        }
        return null;
    }
    
    private abstract static class Bound {
        final boolean strict;        
        Bound(boolean strict) {
            this.strict = strict;
        }
        public abstract int hashCode();
        public abstract boolean equals(Object o);
    }
    private static class LocalBound extends Bound {
        final LocalInstance li;
        LocalBound(boolean strict, LocalInstance bound) {
            super(strict);
            this.li = bound;
        }
        public String toString() {
            return li.name() + (strict?"<":"<=");
        }
        public boolean equals(Object o) {
            if (o instanceof LocalBound) {
                LocalBound that = (LocalBound)o;
                return this.strict == that.strict && this.li.equals(that.li);
            }
            return false;
        }
        public int hashCode() {
            return li.hashCode();
        }
    }
    private static class Bounds {
        Long numericBound; // is always strict
        Set<Bound> bounds;
        Bounds() {
            bounds = new HashSet<Bound>();
        }
        Bounds(Long numericBound, Set<Bound> bounds) {
            this.numericBound = numericBound;
            this.bounds = bounds;
        }
        public String toString() {
            return "(" + numericBound + ", " + bounds + ")";
        }
        public boolean equals(Object o) {
            if (o instanceof Bounds) {
                Bounds that = (Bounds)o;
                if (this.numericBound == that.numericBound || 
                        (this.numericBound != null && this.numericBound.equals(that.numericBound))) {
                    return this.bounds.equals(that.bounds);
                }
            }
            return false;
        }
        public int hashCode() {
            return (numericBound==null?0:numericBound.hashCode()) ^ bounds.hashCode();
        }

    }
    /**
     * The items that this dataflow analysis operates on is essetially a set
     * of integer constraints.
     * 
     * There is a Map from LocalInstances (of type int) to a set of lower bounds.
     */
    static class DataFlowItem extends Item {
        /**
         * map from LocalInstances (of type int) a set of lower bounds. Elements in
         * the set are either Number or LocalInstances
         */
        final Map<LocalInstance, Bounds> lowerBounds;


        DataFlowItem() {
            lowerBounds = Collections.emptyMap();
        }
        private DataFlowItem(Map<LocalInstance, Bounds> lowerBounds) {
            this.lowerBounds = lowerBounds;
        }
        DataFlowItem(DataFlowItem d) {
            lowerBounds = new HashMap<LocalInstance, Bounds>(d.lowerBounds);
        }

        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return (this.lowerBounds == ((DataFlowItem)o).lowerBounds || 
                        this.lowerBounds.equals(((DataFlowItem)o).lowerBounds));
            }
            return false;
        }

        public int hashCode() {
            return lowerBounds.hashCode();
        }

        public String toString() {
            return lowerBounds.toString();
        }        
        public DataFlowItem update(Map<LocalInstance, Bounds> updates) {
            if (updates == null || updates.isEmpty()) {
                return this;
            }
            HashMap<LocalInstance, Bounds> newBounds = new HashMap<LocalInstance, Bounds>(lowerBounds);
            boolean changed = false;
            for (Iterator iterator = updates.entrySet().iterator(); iterator.hasNext();) {
                Entry<LocalInstance, Bounds> entry = (Entry<LocalInstance, Bounds>)iterator.next();
                LocalInstance li = entry.getKey();
                Bounds b = newBounds.get(li);
                if (b == null) {
                    newBounds.put(li, entry.getValue());
                    changed = true;
                }
                else {
                    // already had some bounds. Merge them.
                    Bounds nb = mergeBounds(b, entry.getValue());
                    if (nb != b) {
                        newBounds.put(li, nb);
                        changed = true;
                    }
                }
            }
            if (!changed) return this;
            return new DataFlowItem(newBounds);
        }

    }

}
