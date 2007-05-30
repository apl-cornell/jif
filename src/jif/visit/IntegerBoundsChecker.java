package jif.visit;

import java.util.*;
import java.util.Map.Entry;

import jif.ast.DowngradeExpr;
import jif.extension.JifExprExt;
import polyglot.ast.*;
import polyglot.ast.Binary.Operator;
import polyglot.frontend.Job;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
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
    /**
     * We use boolean flows for this dataflow analysis, i.e., we want to track
     * different information on the true and false branches.
     */
    protected Map flow(List inItems, List inItemKeys, FlowGraph graph, Term n, Set edgeKeys) {
        return this.flowToBooleanFlow(inItems, inItemKeys, graph, n, edgeKeys);
    }

    protected static final Set<Operator> INTERESTING_BINARY_OPERATORS = new HashSet<Operator>(Arrays.asList(
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
        System.err.println("flow for " + n + " : in " + inItem);

        DataFlowItem inDFItem = ((DataFlowItem)inItem);
        
        // create a map of updates, that is, the information that we know to
        // be true as a result of flowing over the term n
        Map<LocalInstance, Bounds> updates = new HashMap<LocalInstance, Bounds>(); 
        LocalInstance invalid = null; // if any local variable is invalidated

        if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl)n;
            if (ld.init() != null) {
                // li = init, so add li <= init, and init <= li
                addBound(updates, ld.localInstance(), false, ld.init());
                addBound(updates, ld.init(), false, ld.localInstance());
            }
            invalid = ld.localInstance();
        }
        else if (n instanceof LocalAssign) {
            LocalAssign la = (LocalAssign)n;
            Expr right = la.right();
            if (!la.operator().equals(Assign.ASSIGN)) {
                // fake the experssion.
                Binary.Operator op = la.operator().binaryOperator();
                right = nodeFactory().Binary(Position.compilerGenerated(), la.left(), op, la.right());
                right = right.type(la.left().type());
            }
            // li = e, so add li <= e, and e <= li
            addBound(updates, la.left(), false, right);
            addBound(updates, right, false, (Local)la.left());
            invalid = ((Local)la.left()).localInstance();
        }
        else if (n instanceof Unary) {
            Unary u = (Unary)n;
            if (u.expr() instanceof Local) {
                Local l = (Local)u.expr();
                if (u.operator().equals(Unary.POST_INC) || u.operator().equals(Unary.PRE_INC) ||
                        u.operator().equals(Unary.POST_DEC) || u.operator().equals(Unary.PRE_DEC)) {
                    // x = x + 1, or x = x -1 , therefore x < x
                    addBound(updates, l, true, l);
                    invalid = l.localInstance();
                }
            }
        }
        else if (n instanceof Binary && ((Binary)n).type().isBoolean() &&
                ((Binary)n).left().type().isNumeric() &&  
                INTERESTING_BINARY_OPERATORS.contains(((Binary)n).operator())) {
            
            // it's a comparison operation! We care about tracking the
            // information that may be gained by these comparisons
            
            Map<LocalInstance, Bounds> falseupdates = new HashMap<LocalInstance, Bounds>();
            
            Binary b = (Binary)n;
            Expr left = b.left();
            Expr right = b.right();
            
            boolean flowedOverBinary = false;
            if (b.operator().equals(Binary.LT)) {
                addBound(updates, left, true, right);       // left < right
                addBound(falseupdates, right, false, left); // !(left < right) => right <= left
                flowedOverBinary = true;
            }
            if (b.operator().equals(Binary.LE)) {
                addBound(updates, left, false, right);       // left <= right
                addBound(falseupdates, right, true, left);   // negation: right < left    
                flowedOverBinary = true;
            }
            if (b.operator().equals(Binary.GT)) {
                addBound(updates, right, true, left);       // right < left               
                addBound(falseupdates, left, false, right); // negation: left <= right
                flowedOverBinary = true;
            }
            if (b.operator().equals(Binary.GE)) {
                addBound(updates, right, false, left);      // right <= left           
                addBound(falseupdates, left, true, right);  // negation: left < right                    
                flowedOverBinary = true;
            }
            if (b.operator().equals(Binary.EQ)) {
                addBound(updates, left, false, right);        // left <= right, and right <= left        
                addBound(updates, right, false, left);
                // note: no negation, since we don't know why the equality failed
                flowedOverBinary = true;
            }
            if (flowedOverBinary) {
                // track the true and false branches precisely.
                DataFlowItem trueOutDFItem = inDFItem.update(updates, invalid);
                DataFlowItem falseOutDFItem = inDFItem.update(falseupdates, invalid);
                return itemsToMap(trueOutDFItem, falseOutDFItem, null, succEdgeKeys);
            }
            
        }
        // apply the updates to the data flow item.
        DataFlowItem outDFItem = inDFItem.update(updates, invalid);

        if (n instanceof Expr && ((Expr)n).type().isNumeric()) {
            ((JifExprExt)n.ext()).setNumericLowerBound(findNumericLowerBound((Expr)n, inDFItem));
        }
        if (n instanceof Expr && ((Expr)n).type().isBoolean() && 
                (n instanceof Binary || n instanceof Unary)) {
            // flow over boolean conditions (e.g. &&, ||, !, etc) if we can.
            DataFlowItem otherDFItem = outDFItem;
            if (trueItem == null) trueItem = outDFItem;
            if (falseItem == null) falseItem = outDFItem;
            Map m = flowBooleanConditions(trueItem, falseItem, otherDFItem, graph, (Expr)n, succEdgeKeys);            
            if (m != null) return m;
        } 

        return itemToMap(outDFItem, succEdgeKeys);
    }

    /**
     * Add a bound. If strict is true, then it is left < rli. If strict is false, it is
     * left <= rli.
     */
    protected void addBound(Map<LocalInstance, Bounds> updates, Object left, boolean strict, Expr right) {
        if (right instanceof Local) {
            addBound(updates, left, strict, ((Local)right).localInstance());
        }
    }
    
    /**
     * Add a bound. If strict is true, then it is left < rli. If strict is false, it is
     * left <= rli.
     */
    protected void addBound(Map<LocalInstance, Bounds> updates, Object left, boolean strict, LocalInstance rli) {
        Set<LocalInstance> liLowerBounds = Collections.emptySet();
        Long lnum = null;
        if (left instanceof LocalInstance) {
            liLowerBounds = Collections.singleton((LocalInstance)left);
        }
        else if (left instanceof Expr) {
            liLowerBounds = findLocalInstanceLowerBounds((Expr)left);
            lnum = findNumericLowerBound((Expr)left, null);
            if (strict && lnum != null) {
                // lnum < left < rli, so lnum + 1 < rli
                lnum = Long.valueOf(lnum.longValue() + 1);
            }
            //System.err.println(rli.name()+" liLowerBounds="+liLowerBounds + " numLowerBound="+lnum);
        }
        if (liLowerBounds.isEmpty() && lnum == null) return;
        
        Bounds b = updates.get(rli);
        if (b == null) {
            b = new Bounds();
            updates.put(rli, b);
        }
        for (Iterator<LocalInstance> iter = liLowerBounds.iterator(); iter.hasNext();) {
            LocalInstance lli = iter.next();
            b.lowerBounds.add(new LocalBound(strict, lli));            
        }

        if (lnum != null) {
            if (b.numericLowerBound == null || b.numericLowerBound.longValue() < lnum.longValue()) {
                b = new Bounds(lnum, b.lowerBounds);
                updates.put(rli, b);
            }
        }
    }
    /**
     * Returns the set of LocalInstances that are (non-strict) lower bounds on the expression
     */
    protected Set<LocalInstance> findLocalInstanceLowerBounds(Expr expr) {
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
                Long leftNum = findNumericLowerBound(b.left(), null);
                Long rightNum = findNumericLowerBound(b.right(), null);
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
            if (a.left() instanceof Local && a.operator().equals(Assign.ASSIGN)) {
                return Collections.singleton(((Local)a.left()).localInstance());
            }
        }
        return Collections.emptySet();
    }

    /**
     * Record the bounds information. We could be extensible here, and allow
     * other uses of the info.
     */
    public void check(FlowGraph graph, Term n, Item inItem, Map outItems) throws SemanticException {
        DataFlowItem dfIn = (DataFlowItem)inItem;
        if (n instanceof Expr && ((Expr)n).type().isNumeric()) {
            Long bound = findNumericLowerBound((Expr)n, dfIn);
//            System.err.println("bound for " + n + " : " + bound);
            if (bound != null) {
                ((JifExprExt)n.ext()).setNumericLowerBound(bound);
            }
        }
    }

    /**
     * The confluence of a list of items. Only facts that are true of all
     * items should be retained.
     */
    protected Item confluence(List items, Term node, FlowGraph graph) {
        Map<LocalInstance, Bounds> newMap = null;
        for (Iterator iter = items.iterator(); iter.hasNext();) {
            DataFlowItem df = (DataFlowItem)iter.next();
            if (newMap == null) {
                newMap = new HashMap<LocalInstance, Bounds>(df.bounds);
                continue;
            }

            for (Iterator<LocalInstance> iterator = newMap.keySet().iterator(); iterator.hasNext();) {                
                LocalInstance li = iterator.next();
                if (df.bounds.containsKey(li)) {
                    // merge the the bounds
                    newMap.put(li, newMap.get(li).merge(df.bounds.get(li)));                    
                }
                else {
                    // the local does not exist in both, so conservatively we ignore it.
                    iterator.remove();
                }
            }
        }
        Item result = new DataFlowItem(newMap);
        return result;
    }
    
    /**
     * Find the greatest numeric lower bound for li, such that B < li 
     */
    protected Long findNumericLowerBound(LocalInstance li, DataFlowItem df) {
        return findNumericLowerBound(li, df, new HashSet<LocalInstance>());
    }
    
    /**
     * Finds the greatest lower bound B it can for li, such that B < li
     */
    protected Long findNumericLowerBound(LocalInstance li, DataFlowItem df, Set<LocalInstance> seen) {
        if (df == null) return null;
        if (seen.contains(li)) return null;
        seen.add(li);        
        
        Bounds b = df.bounds.get(li);
        if (b == null) return null;
        Long num = b.numericLowerBound;
        for (Iterator iter = b.lowerBounds.iterator(); iter.hasNext();) {
            LocalBound lb = (LocalBound)iter.next();
            Long lbb = findNumericLowerBound(lb.li, df, seen);
            if (lbb != null && (num == null || num.longValue() < lbb.longValue())) {
                num = lbb;
            }
        }
        return num;
    }
    /**
     * Finds the greatest lower bound B it can for expr, such that B < expr
     */
    protected Long findNumericLowerBound(Expr expr, DataFlowItem df) {
        if (!expr.type().isNumeric()) return null;
        Long best = null, existing = null;
        if (df == null) {
            existing = ((JifExprExt)expr.ext()).getNumericLowerBound();
        }
        
        if (expr instanceof Local) {
            LocalInstance li = ((Local)expr).localInstance();
            best = max(best, findNumericLowerBound(li, df));
        }
        if (expr.isConstant() && expr.constantValue() instanceof Number) {
            best = max(best, Long.valueOf(((Number)expr.constantValue()).longValue() - 1)); // needs to be a strict bound
        }
        if (expr instanceof Unary) {
            Unary u = (Unary)expr;
            Long b = findNumericLowerBound(u.expr(), df);
            if (b != null) {
                if (u.operator().equals(Unary.POST_DEC) || u.operator().equals(Unary.POST_INC)) {
                    best = max(best, b);
                }
                if (u.operator().equals(Unary.PRE_DEC)) {
                    best = max(best, Long.valueOf(b.longValue() - 1));
                }
                if (u.operator().equals(Unary.PRE_INC)) {
                    best = max(best, Long.valueOf(b.longValue() + 1));
                }
            }
        }
        if (expr instanceof Conditional) {
            Conditional c = (Conditional)expr;
            Long con = ((JifExprExt)c.consequent().ext()).getNumericLowerBound();
            if (con != null) {
                Long alt = ((JifExprExt)c.alternative().ext()).getNumericLowerBound();
                if (alt != null) {
                    // return the min of them
                    best = max(best, (con.longValue() < alt.longValue()) ? con : alt);
                }
            }
        }
        if (expr instanceof DowngradeExpr) {
            DowngradeExpr e = (DowngradeExpr)expr;
            best = max(best, findNumericLowerBound(e.expr(), df));
        }
        if (expr instanceof Binary) {
            Binary b = (Binary)expr;
            if (b.operator().equals(Binary.ADD)) {
                Long left = findNumericLowerBound(b.left(), df);
                Long right = findNumericLowerBound(b.right(), df);
                if (left != null && right != null) {
                    // leftB < left, rightB < right, so leftB + rightB + 1 < left + right 
                    best = max(best, Long.valueOf(left.longValue() + right.longValue() + 1));
                }
            }
            if (b.operator().equals(Binary.MUL)) {
                Long left = findNumericLowerBound(b.left(), df);
                Long right = findNumericLowerBound(b.right(), df);
                if (left != null && right != null) {
                    // expression is l * r, where lb < l, and rb < r.
                    // if lb > 0 or rb > 0, then lb * rb < l * r
                    if (left.longValue() > 0 || right.longValue() > 0) {
                        best = max(best, Long.valueOf(left.longValue() * right.longValue()));
                    }
                }
            }
        }
        if (expr instanceof Assign) {
            Assign a = (Assign)expr;            
            if (a.left() instanceof Local) {
                // it's an assignment to a local, which we 
                // track pretty precisely, so just look at the local.
                best = max(best, findNumericLowerBound(a.left(), df));                
            }
            else {
                // the LHS is not a local, so it's ok to use the same
                // dataflowitem.
                Expr right = a.right();
                if (!a.operator().equals(Assign.ASSIGN)) {
                    // fake the experssion.
                    Binary.Operator op = a.operator().binaryOperator();
                    right = nodeFactory().Binary(Position.compilerGenerated(), a.left(), op, a.right());
                    right = right.type(a.left().type());
                }
                best = max(best, findNumericLowerBound(right, df));                                
            }
        }
        if (expr instanceof Field) {
            Field f = (Field)expr;
            if ("length".equals(f.name()) && 
                    f.type().isInt() && 
                    f.target().type().isArray()) {
                // it's an array length, e.g., x.length.
                // thus it is of non-negative length
                best = max(best, Long.valueOf(-1));
            }
        }
        return max(best, existing);
    }
    
    protected static Long max(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.longValue() < b.longValue() ? b : a;
    }
    
    protected static abstract class Bound {
        protected final boolean strict;        
        public Bound(boolean strict) {
            this.strict = strict;
        }
        public abstract Bound strict(boolean strict);
        public abstract int hashCode();
        public abstract boolean equals(Object o);
    }
    
    protected static class LocalBound extends Bound {
        protected final LocalInstance li;
        LocalBound(boolean strict, LocalInstance bound) {
            super(strict);
            this.li = bound;
        }
        public Bound strict(boolean strict) {
            if (this.strict == strict) return this;
            return new LocalBound(strict, li);
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
    
    protected static class Bounds {
        protected final Long numericLowerBound; // is always strict
        protected final Set<Bound> lowerBounds;

        public Bounds() {
            lowerBounds = new HashSet<Bound>();
            numericLowerBound = null;
        }
        public Bounds(Long numericBound, Set<Bound> bounds) {
            this.numericLowerBound = numericBound;
            this.lowerBounds = bounds;
        }
        public String toString() {
            return "(" + numericLowerBound + ", " + lowerBounds + ")";
        }
        public boolean equals(Object o) {
            if (o instanceof Bounds) {
                Bounds that = (Bounds)o;
                if (this.numericLowerBound == that.numericLowerBound || 
                        (this.numericLowerBound != null && this.numericLowerBound.equals(that.numericLowerBound))) {
                    return this.lowerBounds.equals(that.lowerBounds);
                }
            }
            return false;
        }
        public int hashCode() {
            return (numericLowerBound==null?0:numericLowerBound.hashCode()) ^ lowerBounds.hashCode();
        }

        /**
         * Merge two bounds. The merge is conservative, meaning that
         * the numeric (greatest lower) bound is the lower of the two,
         * and the set of locals is the intersection of both.
         */
        public Bounds merge(Bounds b1) {
        	Bounds b0 = this;
        	
            if (b0.numericLowerBound == null || (b1.numericLowerBound != null && b0.numericLowerBound <= b1.numericLowerBound)) {                
                if (b1.lowerBounds.containsAll(b0.lowerBounds)) {
                    // the merge is just b0, so save some time and memory...
                    return b0;
                }
            }
            
            Set<Bound> bnds = new HashSet<Bound>();
            bnds.addAll(b0.lowerBounds);
            bnds.retainAll(b1.lowerBounds);
            Long numBnd = b0.numericLowerBound;        
            if (b1.numericLowerBound == null || (numBnd != null && numBnd > b1.numericLowerBound)) {
                numBnd = b1.numericLowerBound;
            }
            return new Bounds(numBnd, bnds);
        }
        
        /**
         * Merge two bounds. The merge is not conservative, meaning that
         * the facts in both branches are true. So the numeric (greatest lower) bound is the greater 
         * of the two, and the set of locals is the union of both.
         */
        public Bounds mergeNonconservative(Bounds b1) {
        	Bounds b0 = this;
        	
            if (b1.numericLowerBound == null || (b0.numericLowerBound != null && b0.numericLowerBound >= b1.numericLowerBound)) {
                if (b0.lowerBounds.containsAll(b1.lowerBounds)) {
                    // the merge is just b0, so save some time and memory...
                    return b0;                
                }
            }
            
            Set<Bound> bnds = new HashSet<Bound>();
            bnds.addAll(b0.lowerBounds);
            bnds.addAll(b1.lowerBounds);
            Long numBnd = b0.numericLowerBound;
            if (numBnd == null || (b1.numericLowerBound != null && numBnd < b1.numericLowerBound)) {
                numBnd = b1.numericLowerBound;
            }
            return new Bounds(numBnd, bnds);
        }

    }
    
    /**
     * The items that this dataflow analysis operates on is essetially a set
     * of integer constraints.
     * 
     * There is a Map from LocalInstances (of type int) to a set of lower bounds.
     */
    protected static class DataFlowItem extends Item {
        /**
         * map from LocalInstances (of type int) a set of lower bounds. Elements in
         * the set are either Number or LocalInstances
         */
        private final Map<LocalInstance, Bounds> bounds;


        DataFlowItem() {
            bounds = Collections.emptyMap();
        }
        private DataFlowItem(Map<LocalInstance, Bounds> lowerBounds) {
            this.bounds = lowerBounds;
        }
        DataFlowItem(DataFlowItem d) {
            bounds = new HashMap<LocalInstance, Bounds>(d.bounds);
        }

        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return (this.bounds == ((DataFlowItem)o).bounds || 
                        this.bounds.equals(((DataFlowItem)o).bounds));
            }
            return false;
        }

        public int hashCode() {
            return bounds.hashCode();
        }

        public String toString() {
            return bounds.toString();
        }
        
        /**
         * Produce a new DataFlowItem that is updated with the updates. In particular, the bounds
         * in updates are known to be true. Information about the LocalInstance invalid is no longer
         * true; otherwise, all other facts in this DataFlowItem continue to be true.
         */
        public DataFlowItem update(Map<LocalInstance, Bounds> updates, LocalInstance invalid) {
            if (invalid == null && (updates == null || updates.isEmpty())) {
                // no changes
                return this;
            }
            
            //System.err.println("Update " + updates + " invalid " + invalid + " to " + this.lowerBounds);
            
            // create a copy of the bounds map.
            HashMap<LocalInstance, Bounds> newBounds = new HashMap<LocalInstance, Bounds>(bounds);
            boolean changed = false;

            if (invalid != null) {
                // the bounds information for invalid no longer holds.
                if (newBounds.containsKey(invalid)) {
                    newBounds.remove(invalid);
                    changed = true;
                }
            }
            
            // apply each of the updates.
            for (Iterator iterator = updates.entrySet().iterator(); iterator.hasNext();) {
                Entry<LocalInstance, Bounds> entry = (Entry<LocalInstance, Bounds>)iterator.next();
                LocalInstance li = entry.getKey();
                Bounds b0 = entry.getValue();
                if (li.equals(invalid)) {
                    // if the old value of the invalid is a lower bound for the new value of the invalid,
                    // the lower bounds of the old value are lower bounds for the new value.
                    LocalBound invalidStrict = new LocalBound(true, invalid);
                    LocalBound invalidNonStrict = new LocalBound(false, invalid);
                    if (this.bounds.containsKey(invalid) && (b0.lowerBounds.contains(invalidStrict) || b0.lowerBounds.contains(invalidNonStrict))) {
                        // NOTE: if invalidBounds.bounds.contains(invalidStrict), then we can be more precise, as all
                        // the old bounds are now strict bounds.
                        Bounds oldInvBounds = this.bounds.get(invalid);
                        Long numBound = b0.numericLowerBound; 
                        if (numBound == null || (oldInvBounds.numericLowerBound != null && 
                                numBound.longValue() < oldInvBounds.numericLowerBound.longValue())) {
                            numBound = oldInvBounds.numericLowerBound;
                        }
                        changed = true;
                        b0 = new Bounds(numBound, new HashSet<Bound>(b0.lowerBounds));
                        b0.lowerBounds.addAll(oldInvBounds.lowerBounds);
                    }                    
                }
                Bounds b1 = newBounds.get(li);
                if (b1 == null) {
                    newBounds.put(li, b0);
                    changed = true;
                }
                else {
                    // already had some bounds. Merge them, knowing that all the facts in both are correct.
                    Bounds b2 = b1.mergeNonconservative(b0);
                    if (b2 != b1) {
                        newBounds.put(li, b2);
                        changed = true;
                    }
                }
            }

            // take care of the invalidated local instance
            if (invalid != null) {
                // remove any reference to the invalid local variable
                LocalBound invalidStrict = new LocalBound(true, invalid);
                LocalBound invalidNonStrict = new LocalBound(false, invalid);
                Map<LocalInstance, Bounds> cleanedBounds = new HashMap<LocalInstance, Bounds>();
                for (Iterator iter = newBounds.entrySet().iterator(); iter.hasNext();) {
                    Entry<LocalInstance, Bounds> entry = (Entry<LocalInstance, Bounds>)iter.next();
                    
                    Bounds b = entry.getValue();
                    if (b.lowerBounds.contains(invalidStrict) || b.lowerBounds.contains(invalidNonStrict)) {
                        changed = true;
                        b = new Bounds(b.numericLowerBound, new HashSet<Bound>(b.lowerBounds));
                        b.lowerBounds.remove(invalidStrict);
                        b.lowerBounds.remove(invalidNonStrict);
                        
                    }
                    cleanedBounds.put(entry.getKey(), b);                    
                }
            }            
                
            DataFlowItem result = this;
            if (changed) result = new DataFlowItem(newBounds);
            return result;
        }

    }
}
