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

    protected static final Set<Operator> INTERESTING_BINARY_OPERATORS = 
        new HashSet<Operator>(Arrays.asList(new Binary.Operator[] { 
                Binary.EQ, Binary.LE, Binary.LT, Binary.GE, Binary.GT, }));
    
    public Map flow(Item trueItem, Item falseItem, Item otherItem, 
            FlowGraph graph, Term n, Set succEdgeKeys) {
        Item inItem = safeConfluence(trueItem, FlowGraph.EDGE_KEY_TRUE, 
                                     falseItem, FlowGraph.EDGE_KEY_FALSE,
                                     otherItem, FlowGraph.EDGE_KEY_OTHER,
                                     n, graph);
//        System.err.println("flow for " + n + " : in " + inItem);

        DataFlowItem inDFItem = ((DataFlowItem)inItem);
        
        // create a map of updates, that is, the information that we know to
        // be true as a result of flowing over the term n
        Map<LocalInstance, Bounds> updates = new HashMap<LocalInstance, Bounds>();
        // if the value of a local changes, it goes in one of these two, or both
        LocalInstance increased = null, decreased = null;

        if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl) n;
            
            if (ld.init() != null) {
                Local l = (Local) nodeFactory().Local(
                        Position.compilerGenerated(), ld.id()).localInstance(
                                ld.localInstance()).type(ld.localInstance().type());
                // li = init, so add li <= init, and init <= li
                addBounds(updates, l, false, ld.init());
                addBounds(updates, ld.init(), false, l);
                
                increased = decreased = ld.localInstance();
            }
        } else if (n instanceof LocalAssign) {
            LocalAssign la = (LocalAssign) n;
            Expr right = la.right();
            
            if (la.operator() != Assign.ASSIGN) {
                // fake the experssion.
                Binary.Operator op = la.operator().binaryOperator();
                right = nodeFactory().Binary(Position.compilerGenerated(), la.left(), op, la.right());
                right = right.type(la.left().type());
            }
            
            // li = e, so add li <= e, and e <= li
            addBounds(updates, la.left(), false, right);
            addBounds(updates, right, false, la.left());
            
            // XXX can be smarter and decide if only increases or decreases
            increased = decreased = ((Local) la.left()).localInstance();
        } else if (n instanceof Unary) {
            Unary u = (Unary) n;
            
            if (u.expr() instanceof Local) {
                Local l = (Local) u.expr();
                
                if (u.operator() == Unary.POST_INC || 
                        u.operator() == Unary.PRE_INC) {
                    Bounds b = inDFItem.bounds.get(l.localInstance());
                    
                    if (b != null) {
                        addRangeBound(updates, l.localInstance(),
                                b.range.shift(1));
                    }
                    
                    increased = l.localInstance();
                } else if (u.operator() == Unary.POST_DEC ||
                        u.operator() == Unary.PRE_DEC) {
                    Bounds b = inDFItem.bounds.get(l.localInstance());
                    
                    if (b != null) {
                        addRangeBound(updates, l.localInstance(),
                                b.range.shift(-1));
                    }
                    
                    decreased = l.localInstance();
                }
            }
        } else if (n instanceof Binary && ((Binary)n).type().isBoolean() &&
                ((Binary)n).left().type().isNumeric() &&  
                INTERESTING_BINARY_OPERATORS.contains(((Binary) n).operator())) {
            // it's a comparison operation! We care about tracking the
            // information that may be gained by these comparisons
            Map<LocalInstance, Bounds> falseupdates = 
                new HashMap<LocalInstance, Bounds>();
            
            Binary b = (Binary) n;
            Expr left = b.left();
            Expr right = b.right();
            
            boolean flowedOverBinary = false;
            
            if (b.operator() == Binary.LT) {
                addBounds(updates, left, true, right);       // left < right
                addBounds(falseupdates, right, false, left); // !(left < right) => right <= left
                flowedOverBinary = true;
            } else if (b.operator() == Binary.LE) {
                addBounds(updates, left, false, right);       // left <= right
                addBounds(falseupdates, right, true, left);   // negation: right < left    
                flowedOverBinary = true;
            } else if (b.operator() == Binary.GT) {
                addBounds(updates, right, true, left);       // right < left               
                addBounds(falseupdates, left, false, right); // negation: left <= right
                flowedOverBinary = true;
            } else if (b.operator() == Binary.GE) {
                addBounds(updates, right, false, left);      // right <= left           
                addBounds(falseupdates, left, true, right);  // negation: left < right                    
                flowedOverBinary = true;
            } else if (b.operator() == Binary.EQ) {
                addBounds(updates, left, false, right);        // left <= right, and right <= left        
                addBounds(updates, right, false, left);
                // note: no negation, since we don't know why the equality failed
                flowedOverBinary = true;
            }
            
            if (flowedOverBinary) {
                // track the true and false branches precisely.
                DataFlowItem trueOutDFItem = inDFItem.update(updates, increased, decreased);
                DataFlowItem falseOutDFItem = inDFItem.update(falseupdates, increased, decreased);
                return itemsToMap(trueOutDFItem, falseOutDFItem, null, succEdgeKeys);
            }
        }
        
        // apply the updates to the data flow item.
        DataFlowItem outDFItem = inDFItem.update(updates, increased, decreased);

        if (n instanceof Expr && ((Expr)n).type().isNumeric()) {
            setExprBounds((Expr) n, findNumericRange((Expr) n, inDFItem).lower);
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
    
    protected void setExprBounds(Expr e, Long bound) {
        // TODO store intervals?
        JifExprExt ext = (JifExprExt) e.ext();
        Long b = ext.getNumericLowerBound();
        
        if (b == null || bound < b) {
            b = bound;
        }
        
        ext.setNumericLowerBound(b);
    }

    /**
     * Add bounds to updates given left < right or left <= right, depending on
     * whether strict is set.
     */
    protected void addBounds(Map<LocalInstance, Bounds> updates, 
            Expr left, boolean strict, Expr right) {
        if (!left.type().isNumeric() || !right.type().isNumeric()) {
            return;
        }
        
        Set<LocalInstance> lli = findLocalInstanceBounds(left, Bound.lower(false));
        Set<LocalInstance> rli = findLocalInstanceBounds(right, Bound.upper(false));
        
        Interval lrng = findNumericRange(left, null);
        Interval rrng = findNumericRange(right, null);
        
        for (LocalInstance l : lli) {
            Bounds b = updates.get(l);
            
            if (b == null) {
                b = new Bounds();
            }
            
            Long lupper = b.range.upper;
            
            if (rrng.upper != Bounds.POS_INF && rrng.upper <= lupper) {
                lupper = strict ? rrng.upper - 1 : rrng.upper;
            }
            
            for (LocalInstance r : rli) {
                if (r != l) {
                    b.bounds.add(new LocalBound(Bound.upper(strict), r));
                }
            }
            
            updates.put(l, new Bounds(b.range.lower, lupper, b.bounds));
        }

        for (LocalInstance r : rli) {
            Bounds b = updates.get(r);
            
            if (b == null) {
                b = new Bounds();
            }
            
            Long rlower = b.range.lower;
            
            if (lrng.lower != Bounds.NEG_INF && lrng.lower >= rlower) {
                rlower = strict ? lrng.lower + 1 : lrng.lower;
            }
            
            for (LocalInstance l : rli) {
                if (l != r) {
                    b.bounds.add(new LocalBound(Bound.lower(strict), l));
                }
            }
            
            updates.put(r, new Bounds(rlower, b.range.upper, b.bounds));
        }
    }

    protected void addRangeBound(Map<LocalInstance, Bounds> updates, 
            LocalInstance li, Interval rng) {
        Bounds b = updates.get(li);
        
        if (b == null) {
            b = new Bounds();
        }
        
        updates.put(li, new Bounds(b.range.intersect(rng), b.bounds));
    }
    
    /**
     * Returns the set of LocalInstances that are (non-strict) lower bounds on
     * the expression
     */
    protected Set<LocalInstance> findLocalInstanceBounds(Expr expr, Bound.Type type) {
        if (expr instanceof Local) {
            return Collections.singleton(((Local) expr).localInstance());
        } else if (expr instanceof Unary) {
            Unary u = (Unary) expr;
            
            if (u.operator()== Unary.PRE_INC || u.operator() == Unary.PRE_DEC) {
                return findLocalInstanceBounds(u.expr(), type);
            } else if (u.operator() == Unary.POST_INC && type.isUpper()) {
                return findLocalInstanceBounds(u.expr(), type);
            } else if (u.operator() == Unary.POST_DEC && type.isLower()) {
                return findLocalInstanceBounds(u.expr(), type);
            }
        } else if (expr instanceof Conditional) {
            Conditional c = (Conditional) expr;
            Set<LocalInstance> con = findLocalInstanceBounds(c.consequent(), type);
            Set<LocalInstance> alt = findLocalInstanceBounds(c.alternative(), type);
            // return the intersection of con and alt.
            con.retainAll(alt);
            return con;
        } else if (expr instanceof Binary) {
            Binary b = (Binary) expr;
            
            if (b.operator() == Binary.ADD) {
                Set<LocalInstance> left = findLocalInstanceBounds(b.left(), type);                
                Set<LocalInstance> right = findLocalInstanceBounds(b.right(), type);                
                Interval lrng = findNumericRange(b.left(), null);
                Interval rrng = findNumericRange(b.right(), null);
                Set<LocalInstance> result = new HashSet<LocalInstance>();
                
                if ((type.isLower() && lrng.lower >= 0) ||
                        (type.isUpper() && lrng.upper <= 0)) {
                    result.addAll(right);
                }
                
                if ((type.isLower() && rrng.lower >= 0) ||
                        (type.isUpper() && rrng.upper <= 0)) {
                    result.addAll(left);
                }
                
                return result;
            } else if (b.operator() == Binary.SUB) {
                
            }
        } else if (expr instanceof Assign) {
            Assign a = (Assign) expr;
            Set<LocalInstance> result = new HashSet<LocalInstance>();
            
            if (a.left() instanceof Local && a.operator() == Assign.ASSIGN) {
                result.add(((Local) a.left()).localInstance());
            }
            
            result.addAll(findLocalInstanceBounds(a.right(), type));
            return result;
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
            Long bound = findNumericRange((Expr)n, dfIn).lower;
//            System.err.println("bound for " + n + " : " + bound);
            if (bound != null) {
                setExprBounds((Expr) n, bound);
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
     * Finds the tightest numeric bound possible for li. Type can be
     * lower/upper, strict/non-strict.
     */
    protected Long findNumericBound(LocalInstance li, DataFlowItem df, 
            Bound.Type type) {
        return findNumericBound(li, df, type, new HashSet<LocalInstance>());
    }

    /**
     * Finds the tightest numeric bound possible for li. Type can be
     * lower/upper, strict/non-strict.
     */
    protected Long findNumericBound(LocalInstance li, DataFlowItem df, 
            Bound.Type type, Set<LocalInstance> seen) {
        if (df == null || seen.contains(li)) {
            return type.isLower() ? Bounds.NEG_INF : Bounds.POS_INF;
        }
        
        seen.add(li);
        Bounds bnds = df.bounds.get(li);
        
        if (bnds == null) {
            return type.isLower() ? Bounds.NEG_INF : Bounds.POS_INF;
        }
        
        Long best = bnds.getNumericBound(type);
        
        for (Bound b : bnds.bounds) {
            if (b instanceof LocalBound) {
                LocalBound lb = (LocalBound) b;
                
                if (type.isLower() == lb.isLower()) {
                    best = Bounds.refine(best, 
                            findNumericBound(lb.li, df, lb.type, seen), type);
                }
            }
        }
        
        if (best != Bounds.POS_INF && best != Bounds.NEG_INF && type.isStrict()) {
            if (type.isLower()) {
                best -= 1;
            } else {
                best += 1;
            }
        }
        
        return best;
    }
    
    /**
     * Finds the greatest lower bound or least upper bound B it can for expr,
     * such that B < expr or expr < B, respectively.
     */
    protected Interval findNumericRange(Expr expr, DataFlowItem df) {
        if (!expr.type().isNumeric()) {
            throw new IllegalArgumentException();
        }
        
        Interval best = Interval.FULL, existing = Interval.FULL;
        
        // TODO storing upper bounds in JifExprExt as well?
        if (df == null) {
            Long n = ((JifExprExt)expr.ext()).getNumericLowerBound();
            existing = new Interval(n == null ? Bounds.NEG_INF : n, Bounds.POS_INF);
        }
        
        if (expr instanceof Local) {
            LocalInstance li = ((Local) expr).localInstance();
            Long low = findNumericBound(li, df, Bound.lower(false));
            Long high = findNumericBound(li, df, Bound.upper(false));
            best = best.intersect(new Interval(low, high));
        } else if (expr.isConstant() && expr.constantValue() instanceof Number) {
            // needs to be a strict bound
            long n = ((Number) expr.constantValue()).longValue();
            best = best.intersect(Interval.singleton(n));
        } else if (expr instanceof Unary) {
            Unary u = (Unary)expr;
            Interval rng = findNumericRange(u.expr(), df);
            
            if (u.operator() == Unary.PRE_INC || u.operator() == Unary.PRE_DEC) {
                best = best.intersect(rng);
            } else if (u.operator() == Unary.POST_INC) {
                best = best.intersect(rng.shift(-1));
            } else if (u.operator() == Unary.POST_DEC) {
                best = best.intersect(rng.shift(1));
            }
        } else if (expr instanceof Conditional) {
            Conditional c = (Conditional) expr;
            // TODO storing intervals?
            Long n = ((JifExprExt) c.consequent().ext()).getNumericLowerBound();
            Interval con = new Interval(n == null ? Bounds.NEG_INF : n, Bounds.POS_INF);
            n = ((JifExprExt)c.alternative().ext()).getNumericLowerBound();
            Interval alt = new Interval(n == null ? Bounds.NEG_INF : n, Bounds.POS_INF);
            best = best.intersect(con.union(alt));
        } else if (expr instanceof DowngradeExpr) {
            DowngradeExpr e = (DowngradeExpr) expr;
            best = best.intersect(findNumericRange(e.expr(), df));
        } else if (expr instanceof Binary) {
            Binary b = (Binary) expr;
            
            if (b.operator() == Binary.ADD) {
                Interval left = findNumericRange(b.left(), df);
                Interval right = findNumericRange(b.right(), df);
                best = best.intersect(left.add(right));
            } else if (b.operator() == Binary.SUB) {
                Interval left = findNumericRange(b.left(), df);
                Interval right = findNumericRange(b.right(), df);
                best = best.intersect(left.subtract(right));
            } else if (b.operator() == Binary.MUL) {
                Interval left = findNumericRange(b.left(), df);
                Interval right = findNumericRange(b.right(), df);
                best = best.intersect(left.multiply(right));
            }
        } else if (expr instanceof Assign) {
            Assign a = (Assign) expr;

            best = best.intersect(findNumericRange(a.left(), df));
            Expr right = a.right();

            if (a.operator() != Assign.ASSIGN) {
                // fake the experssion.
                Binary.Operator op = a.operator().binaryOperator();
                right = nodeFactory().Binary(Position.compilerGenerated(),
                        a.left(), op, a.right());
                right = right.type(a.left().type());
            }

            best = best.intersect(findNumericRange(right, df));     
        } else if (expr instanceof Field) {
            Field f = (Field) expr;
            
            if ("length".equals(f.name()) && f.type().isInt() && 
                    f.target().type().isArray()) {
                // it's an array length, e.g., x.length.
                // thus it is of non-negative length
                best = best.intersect(Interval.POS);
            }
        }
        
        return best.intersect(existing);
    }
    
    protected static Long max(Long a, Long b) {
        if (a == Bounds.NEG_INF) return b;
        if (b == Bounds.NEG_INF) return a;
        return a < b ? b : a;
    }

    protected static Long min(Long a, Long b) {
        if (a == Bounds.POS_INF) return b;
        if (b == Bounds.POS_INF) return a;
        return a < b ? a : b;
    }
    
    protected static abstract class Bound {
        
        protected static enum Type {
            
            LT("<"), LE("<="), GT(">"), GE(">=");
            
            private final String name;
            
            private Type(String name) {
                this.name = name;
            }
            
            public boolean isLower() {
                return this == GT || this == GE;
            }
            
            public boolean isUpper() {
                return this == LT || this == LE;
            }
            
            public boolean isStrict() {
                return this == LT || this == GT;
            }
            
            public Type strict() {
                switch (this) {
                case LE: return LT;
                case GE: return GT;
                default: return this;
                }
            }
            
            public Type nonStrict() {
                switch (this) {
                case LT: return LE;
                case GT: return GE;
                default: return this;
                }
            }
            
            public Type strict(boolean strict) {
                return strict ? strict() : nonStrict();
            }
            
            public String toString() {
                return name;
            }
            
        }
        
        public static final Type LT = Type.LT;
        public static final Type LE = Type.LE;
        public static final Type GT = Type.GT;
        public static final Type GE = Type.GE;
        
        public static Type lower(boolean strict) {
            return GT.strict(strict);
        }
        
        public static Type upper(boolean strict) {
            return LT.strict(strict);
        }
        
        protected final Type type;
        
        public Bound(Type type) {
            this.type = type;
        }

        public boolean isLower() {
            return type.isLower();
        }
        
        public boolean isUpper() {
            return type.isUpper();
        }
        
        public boolean equals(Object o) {
            if (o instanceof Bound) {
                Bound other = (Bound) o;
                return type == other.type;
            } else {
                return false;
            }
        }

        public int hashCode() {
            return type.hashCode();
        }
        
        public String toString() {
            return type.toString();
        }
        
        public abstract Bound strict(boolean strict);
        
    }
    
    protected static class LocalBound extends Bound {
        
        protected final LocalInstance li;
        
        public LocalBound(Type type, LocalInstance bound) {
            super(type);
            this.li = bound;
        }
        
        public Bound strict(boolean strict) {
            if (type.isStrict() == strict) return this;
            return new LocalBound(type.strict(strict), li);
        }
        
        public boolean equals(Object o) {
            if (o instanceof LocalBound) {
                LocalBound other = (LocalBound) o;
                return super.equals(o) && li.equals(other.li);
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            return super.hashCode() ^ li.hashCode();
        }

        public String toString() {
            return type + li.name();
        }
        
    }
    
    /**
     * Checks two reference for object equality. Deals with null pointers.
     * Should really be a utility method somewhere...
     */
    protected static boolean nullableEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;  // includes null == null
        } else if (o1 == null || o2 == null) {
            return false;  // can't both be null
        } else {
            return o1.equals(o2);  // both non-null
        }
    }
    
    /**
     * Gets the hash code for a given object, dealing with null pointers.
     */
    protected static int nullableHashCode(Object o) {
        if (o == null) {
            return 0;
        } else {
            return o.hashCode();
        }
    }
    
    /**
     * A closed interval over the integers.
     */
    protected static class Interval {
        
        /**
         * Interval representing all integers.
         */
        public static final Interval FULL = 
            new Interval(Bounds.NEG_INF, Bounds.POS_INF);
        
        /**
         * The non-negative integers (includes 0).
         */
        public static final Interval POS = new Interval(0L, Bounds.POS_INF);
        
        /**
         * Returns an interval containing only one integer.
         */
        public static Interval singleton(long i) {
            return new Interval(i, i);
        }
        
        protected final Long lower;
        protected final Long upper;
        
        public Interval(Long lower, Long upper) {
            if (lower == null || upper == null) {
                throw new NullPointerException();
            }
            
            this.lower = lower;
            this.upper = upper;
        }

        public Long getLower() {
            return lower;
        }

        public Long getUpper() {
            return upper;
        }
        
        /**
         * Returns whether this interval is a superset of the other.
         */
        public boolean contains(Interval other) {
            return lower <= other.lower && upper >= other.upper;
        }
        
        /**
         * Returns the smallest interval that contains this and the other
         * interval.
         */
        public Interval union(Interval other) {
            Long low = min(lower, other.lower);
            Long high = max(upper, other.upper);
            return new Interval(low, high);
        }
        
        /**
         * Returns the intersection of this and the other interval.
         */
        public Interval intersect(Interval other) {
            Long low = max(lower, other.lower);
            Long high = min(upper, other.upper);
            return new Interval(low, high);
        }
        
        /**
         * Returns an interval that is this one shifted by the given amount.
         */
        public Interval shift(long i) {
            Long low = lower == Bounds.NEG_INF ? Bounds.NEG_INF : lower + i;
            Long high = upper == Bounds.POS_INF ? Bounds.POS_INF : upper + i;
            return new Interval(low, high);
        }
        
        public Interval add(Interval other) {
            Long low = Bounds.NEG_INF, high = Bounds.POS_INF;
            
            if (lower != Bounds.NEG_INF && other.lower != Bounds.NEG_INF) {
                low = lower + other.lower;
            }
            
            if (upper != Bounds.POS_INF && other.upper != Bounds.POS_INF) {
                high = upper + other.upper;
            }
            
            return new Interval(low, high);
        }
        
        public Interval subtract(Interval other) {
            Long low = Bounds.NEG_INF, high = Bounds.POS_INF;
            
            if (lower != Bounds.NEG_INF && other.upper != Bounds.POS_INF) {
                low = lower - other.upper;
            }
            
            if (upper != Bounds.POS_INF && other.lower != Bounds.NEG_INF) {
                high = upper - other.lower;
            }
            
            return new Interval(low, high);
        }
        
        protected Long longMult(Long i, Long j) {
            if (i == 0 || j == 0) {
                return 0L;
            }

            if ((i == Bounds.POS_INF && j > 0) ||
                    (j == Bounds.POS_INF && i > 0)) {
                return Bounds.POS_INF;
            }
            
            if ((i == Bounds.POS_INF && j < 0) |
                    (j == Bounds.POS_INF && i < 0)) {
                return Bounds.NEG_INF;
            }
            
            if ((i == Bounds.NEG_INF && j > 0) ||
                    (j == Bounds.NEG_INF && i > 0)) {
                return Bounds.NEG_INF;
            }
            
            if ((i == Bounds.NEG_INF && j < 0) ||
                    (j == Bounds.NEG_INF && i < 0)) {
                return Bounds.POS_INF;
            }
            
            return i * j;
        }
        
        public Interval multiply(Interval other) {
            // [a, b] x [c, d] = [min{ac, ad, bc, bd}, max{ac, ad, bc, bd}]
            Long ac = longMult(lower, other.lower);
            Long ad = longMult(lower, other.upper);
            Long bc = longMult(upper, other.lower);
            Long bd = longMult(upper, other.upper);
            
            Long low = min(min(ac, ad), min(bc, bd));
            Long high = max(max(ac, ad), max(bc, bd));
            return new Interval(low, high);
        }
        
        public boolean equals(Object o) {
            if (o instanceof Interval) {
                Interval other = (Interval) o;
                return nullableEquals(this.lower, other.lower) &&
                    nullableEquals(this.upper, other.upper);
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            return nullableHashCode(lower) ^ nullableHashCode(upper);
        }
        
        private static String longString(Long i) {
            if (i == Bounds.POS_INF || i == Bounds.NEG_INF) {
                return "-";
            } else {
                return i.toString();
            }
        }
        
        public String toString() {
            return "[" + longString(lower) + "," + longString(upper) + "]";
        }

    }
    
    protected static class Bounds {
        
        public static final Long POS_INF = new Long(Long.MAX_VALUE);
        public static final Long NEG_INF = new Long(Long.MIN_VALUE);
        
        public static Long refine(Long i, Long j, Bound.Type type) {
            if (type.isLower()) {
                return max(i, j);
            } else {
                return min(i, j);
            }
        }
        
        protected final Interval range;
        protected final Set<Bound> bounds;

        public Bounds() {
            range = Interval.FULL;
            bounds = new HashSet<Bound>();
        }
        
        public Bounds(Interval range, Set<Bound> bounds) {
            if (range == null || bounds == null) {
                throw new NullPointerException();
            }
            
            this.range = range;
            this.bounds = bounds;
        }
        
        public Bounds(Long lowerBound, Long upperBound, Set<Bound> bounds) {
            this(new Interval(lowerBound, upperBound), bounds);
        }
        
        public Long getNumericLower() {
            return range.lower;
        }
        
        public Long getNumericUpper() {
            return range.upper;
        }
        
        public Set<Bound> getBounds() {
            return bounds;
        }
        
        public Long getNumericBound(Bound.Type type) {
            if (type.isLower()) {
                return range.lower;
            } else {
                return range.upper;
            }
        }
        
        /**
         * Returns whether these bounds are at least as tight as the other
         * bounds.
         */
        public boolean isTighterThan(Bounds other) {
            return other.range.contains(this.range) && 
                this.bounds.containsAll(other.bounds);
        }
        
        /**
         * Merge two bounds. The merge is conservative, meaning that
         * the numeric (greatest lower) bound is the lower of the two,
         * and the set of locals is the intersection of both.
         */
        public Bounds merge(Bounds b1) {
        	Bounds b0 = this;
        	
            if (b1.isTighterThan(b0)) {
                // the merge is just b0, so save some time and memory...
                return b0;
            } else if (b0.isTighterThan(b1)) {
                return b1;
            }

            Interval rng = b0.range.union(b1.range);
            Set<Bound> bnds = new HashSet<Bound>(b0.bounds);
            bnds.retainAll(b1.bounds);
            return new Bounds(rng, bnds);
        }
        
        /**
         * Merge two bounds. The merge is not conservative, meaning that the
         * facts in both branches are true. So the numeric (greatest lower)
         * bound is the greater of the two, and the set of locals is the union
         * of both.
         */
        public Bounds refine(Bounds b1) {
        	Bounds b0 = this;
        	
            if (b0.isTighterThan(b1)) {
                // the merge is just b0, so save some time and memory...
                return b0;                
            } else if (b1.isTighterThan(b0)) {
                return b1;
            }

            Interval rng = b0.range.intersect(b1.range);
            Set<Bound> bnds = new HashSet<Bound>(b0.bounds);
            bnds.addAll(b1.bounds);
            return new Bounds(rng, bnds);
        }

        public boolean equals(Object o) {
            if (o instanceof Bounds) {
                Bounds that = (Bounds) o;
                return range.equals(that.range) && bounds.equals(that.bounds);
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            return range.hashCode() ^ bounds.hashCode();
        }

        public String toString() {
            return "(" + range + ", " + bounds + ")";
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
        protected final Map<LocalInstance, Bounds> bounds;

        public DataFlowItem() {
            bounds = Collections.emptyMap();
        }
        
        protected DataFlowItem(Map<LocalInstance, Bounds> bounds) {
            this.bounds = bounds;
        }
        
        public DataFlowItem(DataFlowItem d) {
            bounds = new HashMap<LocalInstance, Bounds>(d.bounds);
        }

        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                DataFlowItem other = (DataFlowItem) o;
                return bounds.equals(other.bounds);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return bounds.hashCode();
        }

        public String toString() {
            return bounds.toString();
        }
        
        /**
         * Produce a new DataFlowItem that is updated with the updates. In
         * particular, the bounds in updates are known to be true. Information
         * about the LocalInstance invalid is no longer true; otherwise, all
         * other facts in this DataFlowItem continue to be true.
         * 
         * @deprecated Use update(Map, LocalInstance, LocalInstance).
         */
        public DataFlowItem update(Map<LocalInstance, Bounds> updates, LocalInstance invalid) {
            if (invalid == null && (updates == null || updates.isEmpty())) {
                // no changes
                return this;
            }
            
            //System.err.println("Update " + updates + " invalid " + invalid + " to " + this.lowerBounds);
            
            // create a copy of the bounds map.
            Map<LocalInstance, Bounds> newBounds = new HashMap<LocalInstance, Bounds>(bounds);
            boolean changed = false;

            if (invalid != null) {
                // the bounds information for invalid no longer holds.
                if (newBounds.containsKey(invalid)) {
                    newBounds.remove(invalid);
                    changed = true;
                }
            }
            
            // apply each of the updates.
            for (Iterator<Entry<LocalInstance, Bounds>> iterator = 
                    updates.entrySet().iterator(); iterator.hasNext();) {
                Entry<LocalInstance, Bounds> entry = iterator.next();
                LocalInstance li = entry.getKey();
                Bounds b0 = entry.getValue();
                if (li.equals(invalid)) {
                    // if the old value of the invalid is a lower bound for the new value of the invalid,
                    // the lower bounds of the old value are lower bounds for the new value.
                    LocalBound invalidStrict = new LocalBound(Bound.lower(true), invalid);
                    LocalBound invalidNonStrict = new LocalBound(Bound.lower(false), invalid);
                    
                    if (this.bounds.containsKey(invalid) && 
                            (b0.bounds.contains(invalidStrict) || 
                                    b0.bounds.contains(invalidNonStrict))) {
                        // NOTE: if invalidBounds.bounds.contains(invalidStrict), then we can be more precise, as all
                        // the old bounds are now strict bounds.
                        Bounds oldInvBounds = this.bounds.get(invalid);
                        Interval rng = b0.range; 
                        if (rng.contains(oldInvBounds.range)) {
                            rng = oldInvBounds.range;
                        }
                        changed = true;
                        b0 = new Bounds(rng, new HashSet<Bound>(b0.bounds));
                        b0.bounds.addAll(oldInvBounds.bounds);
                    }                    
                }
                Bounds b1 = newBounds.get(li);
                if (b1 == null) {
                    newBounds.put(li, b0);
                    changed = true;
                }
                else {
                    // already had some bounds. Merge them, knowing that all the facts in both are correct.
                    Bounds b2 = b1.refine(b0);
                    if (b2 != b1) {
                        newBounds.put(li, b2);
                        changed = true;
                    }
                }
            }

            // take care of the invalidated local instance
            if (invalid != null) {
                // remove any reference to the invalid local variable
                LocalBound invalidStrict = new LocalBound(Bound.lower(true), invalid);
                LocalBound invalidNonStrict = new LocalBound(Bound.lower(false), invalid);
                Map<LocalInstance, Bounds> cleanedBounds = new HashMap<LocalInstance, Bounds>();
                for (Iterator<Entry<LocalInstance, Bounds>> iter = 
                        newBounds.entrySet().iterator(); iter.hasNext();) {
                    Entry<LocalInstance, Bounds> entry = iter.next();
                    
                    Bounds b = entry.getValue();
                    if (b.bounds.contains(invalidStrict) || b.bounds.contains(invalidNonStrict)) {
                        changed = true;
                        b = new Bounds(b.range, new HashSet<Bound>(b.bounds));
                        b.bounds.remove(invalidStrict);
                        b.bounds.remove(invalidNonStrict);
                        
                    }
                    cleanedBounds.put(entry.getKey(), b);                    
                }
                
                newBounds = cleanedBounds;
            }            
                
            DataFlowItem result = this;
            if (changed) result = new DataFlowItem(newBounds);
            return result;
        }

        /**
         * Produce a new DataFlowItem that is updated with the updates. In
         * particular, the bounds in updates are known to be true. If a
         * LocalInstance has changed, it can be specified as increased,
         * decreased, or both.
         */
        public DataFlowItem update(Map<LocalInstance, Bounds> updates, 
                LocalInstance increased, LocalInstance decreased) {
            if (increased == null && decreased == null && 
                    (updates == null || updates.isEmpty())) {
                // no changes
                return this;
            }
            
            boolean changed = false;
            Map<LocalInstance, Bounds> updated = 
                new HashMap<LocalInstance, Bounds>(this.bounds);
            
            if (increased != null || decreased != null) {
                // go through current bounds and invalidate ones that might
                // no longer hold
                for (LocalInstance li : bounds.keySet()) {
                    Bounds bnds = bounds.get(li);
                    Set<Bound> old = bnds.bounds;
                    Set<Bound> now = new HashSet<Bound>(old);
                    Interval rng = bnds.range;
                    
                    // see if the tracked instance itself changed
                    if (li == increased || li == decreased) {
                        for (Bound b : old) {
                            if (b.isLower() && li == decreased) {
                                // since li decreased, remove all old lower bounds
                                now.remove(b);
                            } else if (b.isUpper() && li == increased) {
                                // li increased, removed old upper bounds
                                now.remove(b);
                            }
                        }
                        
                        // endpoints of numeric range might be invalid now
                        Long low = li == decreased ? Bounds.NEG_INF : rng.lower;
                        Long high = li == increased ? Bounds.POS_INF : rng.upper;
                        rng = new Interval(low, high);
                    } else {  // see if changed instance is in symbolic bounds
                        for (Bound b : old) {
                            if (b instanceof LocalBound) {
                                LocalBound lb = (LocalBound) b;
                                
                                if (lb.li == li) {
                                    if (lb.isLower() && li == increased) {
                                        // li was a lower bound but has now increased,
                                        // so it might not be a lower bound any more
                                        now.remove(lb);
                                    } else if (lb.isUpper() && li == decreased) {
                                        // was upper bound but has decreased
                                        now.remove(lb);
                                    }
                                }
                            }
                        }
                    }

                    if (old.size() != now.size() || !bnds.range.equals(rng)) {
                        updated.put(li, new Bounds(rng, now));
                        changed = true;
                    }
                }
            }
            
            // now apply new bounds
            for (LocalInstance li : updates.keySet()) {
                Bounds b0 = updates.get(li);
                Bounds b1 = updated.get(li);
                
                if (b1 == null) {
                    updated.put(li, b0);
                    changed = true;
                } else {
                    // already had some bounds. Merge them, knowing that all the
                    // facts in both are correct
                    b0 = b1.refine(b0);
                    
                    if (b0 != b1) {
                        updated.put(li, b0);
                        changed = true;
                    }
                }
            }
            
            if (changed) {
                return new DataFlowItem(updated);
            } else {
                return this;
            }
        }
        
    }
    
}
