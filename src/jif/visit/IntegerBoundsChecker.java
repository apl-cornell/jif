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
        System.err.println("flow for " + n + " : in " + inItem);

        DataFlowItem inDFItem = ((DataFlowItem)inItem);
        
        // create a map of updates, that is, the information that we know to
        // be true as a result of flowing over the term n
        Map<LocalInstance, Bounds> updates = new HashMap<LocalInstance, Bounds>();
        // if the value of a local changes, it goes in one of these two, or both
        LocalInstance increased = null, decreased = null;

        if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl)n;
            if (ld.init() != null) {
                // li = init, so add li <= init, and init <= li
                addBound(updates, ld.localInstance(), false, ld.init());
                addBound(updates, ld.init(), false, ld.localInstance());
            }
            
            // XXX can be smarter and decide if only increases or decreases
            increased = decreased = ld.localInstance();
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
            
            // XXX can be smarter and decide if only increases or decreases
            increased = decreased = ((Local) la.left()).localInstance();
        }
        else if (n instanceof Unary) {
            Unary u = (Unary)n;
            if (u.expr() instanceof Local) {
                Local l = (Local)u.expr();
                if (u.operator().equals(Unary.POST_INC) || 
                        u.operator().equals(Unary.PRE_INC)) {
                    increased = l.localInstance();
                } else if (u.operator().equals(Unary.POST_DEC) ||
                        u.operator().equals(Unary.PRE_DEC)) {
                    decreased = l.localInstance();
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
                DataFlowItem trueOutDFItem = inDFItem.update(updates, increased, decreased);
                DataFlowItem falseOutDFItem = inDFItem.update(falseupdates, increased, decreased);
                return itemsToMap(trueOutDFItem, falseOutDFItem, null, succEdgeKeys);
            }
            
        }
        // apply the updates to the data flow item.
        DataFlowItem outDFItem = inDFItem.update(updates, increased, decreased);

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
            b.bounds.add(new LocalBound(Bound.lower(strict), lli));            
        }

        if (lnum != null) {
            if (b.range.lower == null || b.range.lower < lnum) {
                b = new Bounds(lnum, b.bounds);
                updates.put(rli, b);
            }
        }
    }
    
    /**
     * Returns the set of LocalInstances that are (non-strict) lower bounds on
     * the expression.
     * 
     * @deprecated Use findLocalInstanceBounds.
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
     * Returns the set of LocalInstances that are (non-strict) lower bounds on
     * the expression
     */
    protected Set<LocalInstance> findLocalInstanceBounds(Expr expr, Bound.Type type) {
        if (expr instanceof Local) {
            return Collections.singleton(((Local) expr).localInstance());
        } else if (expr instanceof Unary) {
            Unary u = (Unary) expr;
            
            if (u.operator().equals(Unary.PRE_INC) || u.operator().equals(Unary.PRE_DEC)) {
                return findLocalInstanceBounds(u.expr(), type);
            } else if (u.operator().equals(Unary.POST_INC) && type.isUpper()) {
                return findLocalInstanceBounds(u.expr(), type);
            } else if (u.operator().equals(Unary.POST_DEC) && type.isLower()) {
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
            
            if (b.operator().equals(Binary.ADD)) {
                Set<LocalInstance> left = findLocalInstanceBounds(b.left(), type);                
                Set<LocalInstance> right = findLocalInstanceBounds(b.right(), type);                
                Long leftNum = findNumericRange(b.left(), null).lower;
                Long rightNum = findNumericRange(b.right(), null).lower;
                Set<LocalInstance> result = new HashSet<LocalInstance>();
                
                if (leftNum != null && leftNum >= -1) {
                    result.addAll(right);
                }
                
                if (rightNum != null && rightNum >= -1) {
                    result.addAll(left);
                }
                
                return result;
            }
        }
        if (expr instanceof Assign) {
            Assign a = (Assign) expr;
            
            if (a.left() instanceof Local && a.operator().equals(Assign.ASSIGN)) {
                return Collections.singleton(((Local) a.left()).localInstance());
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
            Long bound = findNumericRange((Expr)n, dfIn).lower;
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
     * Find the greatest numeric lower bound for li, such that B < li.
     * 
     * @deprecated Use findNumericBound.
     */
    protected Long findNumericLowerBound(LocalInstance li, DataFlowItem df) {
        return findNumericLowerBound(li, df, new HashSet<LocalInstance>());
    }
    
    /**
     * Finds the greatest lower bound B it can for li, such that B < li.
     * 
     * @deprecated Use findNumericBound.
     */
    protected Long findNumericLowerBound(LocalInstance li, DataFlowItem df, 
            Set<LocalInstance> seen) {
        if (df == null || seen.contains(li)) {
            return null;
        }
        
        seen.add(li);
        Bounds bnds = df.bounds.get(li);
        
        if (bnds == null) {
            return null;
        }
        
        Long low = bnds.getNumericLower();
        
        for (Bound b : bnds.bounds) {
            if (b instanceof LocalBound) {
                LocalBound lb = (LocalBound) b;
                
                if (lb.isLower()) {
                    low = max(low, findNumericLowerBound(lb.li, df, seen));
                }
            }
        }
        
        return low;
    }

    /**
     * Finds the greatest lower bound or least upper bound B it can for li, such
     * that B < li or li < B, respectively.
     */
    protected Long findNumericBound(LocalInstance li, DataFlowItem df, 
            Bound.Type type) {
        return findNumericBound(li, df, type, new HashSet<LocalInstance>());
    }
    
    /**
     * Finds the greatest lower bound or least upper bound B it can for li, such
     * that B < li or li < B, respectively.
     */
    protected Long findNumericBound(LocalInstance li, DataFlowItem df, 
            Bound.Type type, Set<LocalInstance> seen) {
        if (df == null || seen.contains(li)) {
            return null;
        }
        
        seen.add(li);
        Bounds bnds = df.bounds.get(li);
        
        if (bnds == null) {
            return null;
        }
        
        Long best = bnds.getNumericBound(type);
        
        for (Bound b : bnds.bounds) {
            if (b instanceof LocalBound) {
                LocalBound lb = (LocalBound) b;
                
                if (type.isLower() == lb.isLower()) {
                    best = Bounds.refine(best, 
                            findNumericBound(lb.li, df, type, seen), type);
                }
            }
        }
        
        return best;
    }
    
    /**
     * Finds the greatest lower bound B it can for expr, such that B < expr.
     * 
     * @deprecated Use findNumericRange.
     */
    protected Long findNumericLowerBound(Expr expr, DataFlowItem df) {
        if (!expr.type().isNumeric()) return null;
        Long best = null, existing = null;
        if (df == null) {
            existing = ((JifExprExt)expr.ext()).getNumericLowerBound();
        }
        
        if (expr instanceof Local) {
            LocalInstance li = ((Local)expr).localInstance();
            best = max(best, findNumericBound(li, df, Bound.lower(true)));
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

    /**
     * Finds the greatest lower bound or least upper bound B it can for expr,
     * such that B < expr or expr < B, respectively.
     */
    protected Interval findNumericRange(Expr expr, DataFlowItem df) {
        if (!expr.type().isNumeric()) {
            return null;
        }
        
        Interval best = Interval.FULL, existing = Interval.FULL;
        
        // TODO storing upper bounds in JifExprExt as well?
        if (df == null) {
            existing = new Interval(((JifExprExt)expr.ext()).getNumericLowerBound(), null);
        }
        
        if (expr instanceof Local) {
            LocalInstance li = ((Local) expr).localInstance();
            Long low = findNumericBound(li, df, Bound.lower(true));
            Long high = findNumericBound(li, df, Bound.upper(true));
            best = best.intersect(new Interval(low, high));
        } else if (expr.isConstant() && expr.constantValue() instanceof Number) {
            // needs to be a strict bound
            long n = ((Number) expr.constantValue()).longValue();
            best = best.intersect(Interval.singleton(n));
        } else if (expr instanceof Unary) {
            Unary u = (Unary)expr;
            Interval r = findNumericRange(u.expr(), df);
            
            if (u.operator().equals(Unary.PRE_INC) || u.operator().equals(Unary.PRE_DEC)) {
                best = best.intersect(r);
            } else if (u.operator().equals(Unary.POST_INC)) {
                best = best.intersect(r.shift(-1));
            } else if (u.operator().equals(Unary.POST_DEC)) {
                best = best.intersect(r.shift(1));
            }
        } else if (expr instanceof Conditional) {
            Conditional c = (Conditional) expr;
            // TODO storing intervals?
            Interval con = new Interval(((JifExprExt) c.consequent().ext()).getNumericLowerBound(), null);
            Interval alt = new Interval(((JifExprExt)c.alternative().ext()).getNumericLowerBound(), null);
            best = best.intersect(con.union(alt));
        } else if (expr instanceof DowngradeExpr) {
            DowngradeExpr e = (DowngradeExpr) expr;
            best = best.intersect(findNumericRange(e.expr(), df));
        } else if (expr instanceof Binary) {
            Binary b = (Binary) expr;
            
            if (b.operator().equals(Binary.ADD)) {
                Interval left = findNumericRange(b.left(), df);
                Interval right = findNumericRange(b.right(), df);
                best = best.intersect(left.add(right));
            }
            if (b.operator().equals(Binary.MUL)) {
                // TODO
                //Interval left = findNumericRange(b.left(), df);
                //Interval right = findNumericRange(b.right(), df);
                //best = best.intersect(left.multiply(right));
            }
        } else if (expr instanceof Assign) {
            Assign a = (Assign) expr;
            
            if (a.left() instanceof Local) {
                // it's an assignment to a local, which we 
                // track pretty precisely, so just look at the local.
                best = best.intersect(findNumericRange(a.left(), df));                
            } else {
                // the LHS is not a local, so it's ok to use the same
                // dataflowitem.
                Expr right = a.right();
                
                if (!a.operator().equals(Assign.ASSIGN)) {
                    // fake the experssion.
                    Binary.Operator op = a.operator().binaryOperator();
                    right = nodeFactory().Binary(Position.compilerGenerated(), 
                            a.left(), op, a.right());
                    right = right.type(a.left().type());
                }
                
                best = best.intersect(findNumericRange(right, df));                                
            }
        } else if (expr instanceof Field) {
            Field f = (Field) expr;
            
            if ("length".equals(f.name()) && f.type().isInt() && 
                    f.target().type().isArray()) {
                // it's an array length, e.g., x.length.
                // thus it is of non-negative length
                best = best.intersect(new Interval(-1L, null));
            }
        }
        
        return best.intersect(existing);
    }
    
    protected static Long max(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return a < b ? b : a;
    }

    protected static Long min(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
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
     * An (open) interval over the integers.
     */
    protected static class Interval {
        
        /**
         * Interval representing all integers.
         */
        public static final Interval FULL = new Interval(null, null);

        /**
         * Returns an interval containing only one integer.
         */
        public static Interval singleton(long i) {
            return new Interval(i - 1, i + 1);
        }
        
        protected final Long lower;
        protected final Long upper;
        
        public Interval(Long lower, Long upper) {
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
            boolean low = lower == null || 
                (other.lower != null && lower <= other.lower);
            boolean high = upper == null ||
                (other.upper != null && upper >= other.upper);
            return low && high;
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
            Long low = lower == null ? null : lower + i;
            Long high = upper == null ? null : upper + i;
            return new Interval(low, high);
        }
        
        public Interval add(Interval other) {
            Long low = null, high = null;
            
            if (lower != null && other.lower != null) {
                low = lower + other.lower + 1;
            }
            
            if (upper != null && other.upper != null) {
                high = upper + other.upper - 1;
            }
            
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
        
        private static String nullableLong(Long i) {
            return i == null ? "-" : i.toString();
        }
        
        public String toString() {
            return "[" + nullableLong(lower) + "," + nullableLong(upper) + "]";
        }

    }
    
    protected static class Bounds {
        
        public static Long refine(Long i, Long j, Bound.Type type) {
            if (type.isLower()) {
                return max(i, j);
            } else {
                return min(i, j);
            }
        }
        
        protected final Interval range;  // is always strict
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
        
        public Bounds(Long lowerBound, Set<Bound> bounds) {
            this(lowerBound, null, bounds);
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
                        Long low = li == decreased ? null : rng.lower;
                        Long high = li == increased ? null : rng.upper;
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
