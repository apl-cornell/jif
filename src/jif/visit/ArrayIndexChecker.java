package jif.visit;

import java.util.*;

import jif.extension.JifArrayAccessDel;
import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.DataFlow;
import polyglot.visit.FlowGraph;

/**
 * ###TODO
 */
public class ArrayIndexChecker extends DataFlow
{
    /**
     * Constructor
     */
    public ArrayIndexChecker(Job job, TypeSystem ts, NodeFactory nf) {
	super(job, ts, nf, true /* forward analysis */);
    }

    /**
     * An ArrayInstanceLength represents the length of a particular array
     * object. Everytime a local variable of an array type is assigned to,
     * a new ArrayInstanceLength is generated for that local variable, to 
     * prevent the following type of (erroneous) dataflow analysis:
     * <code>
     *    ...
     *    int a[] = {1, 2, 3};             // line 1
     *    int len = a.length;              // line 2
     *    a = new int{} [1];               // line 3
     *    for (int i = 0; i < len; i++) {  // line 4
     *      a[i]++;                        // line 5
     *    }
     *    ...
     * </code>
     * 
     * Unless the dataflow detects that the array a has changed at line 3,
     * it will not realize that the array access in line 5 is unsafe.
     */
    static class ArrayInstanceLength {
        /**
         * The local variable of some array type that this object 
         * represents the length of.
         */
        LocalInstance array;
        
        /**
         * A number to indicate uniqueness of objects pointed to by the
         * local instance.
         */
        int instance;
        
        /**
         * Mechanism to provide unique instance numbers
         */
        private static int counter = 0;
        
        /**
         * Constructor
         * 
         * @param array 
         * @param newInstance true if the object is being constructed to 
         *        represent a new assignment to the local variable. False if
         *        it is being constructed to represent the default value of
         *        the local (for example, if the local is an argument that hasn't 
         *        been assigned to, then it will not be in the integerBounds
         *        map of the DataFlowItems, meaning it is the original instance)
         *        
         */
        ArrayInstanceLength(LocalInstance array, boolean newInstance) {
            this.array = array;
            if (newInstance) {
                this.instance = ++counter;
            }
            else {
                this.instance = 0;
            }
        }

        public int hashCode() {
            return array.hashCode() + instance;
        }
        
        public boolean equals(Object o) {
            if (o instanceof ArrayInstanceLength) {
                return array.equals(((ArrayInstanceLength)o).array) && instance == ((ArrayInstanceLength)o).instance;
            }
            return false;
        }
        
        public String toString() {
            return "length(" + array.name() + "{" + instance + "})";
        }
    }
    
    /**
     * A Bound represents a constraint on a Local variable or on the length
     * of some array instance. At least one of the upper and lower bounds must
     * be of class LocalInstance or ArrayInstanceLength.
     * 
     * Less than (<) and Less-than-equals (<=) constraints can be represented.
     */
    static class Bound {
        /**
         * Constructor
         * 
         * If the upper or lower bound is an Integer, then the constraint
         * must be a Bound.LESS_THAN_EQUALS. 
         */
        Bound(Object lowerBound, int constraint, Object upperBound) {
            // convert all integer constraints to a canonical form.
            if (constraint == Bound.LESS_THAN) {
                if (lowerBound instanceof Integer) {
                    lowerBound = new Integer(((Integer)lowerBound).intValue() + 1);
                    constraint = Bound.LESS_THAN_EQUALS;
                }
                else if (upperBound instanceof Integer) {
                    upperBound = new Integer(((Integer)upperBound).intValue() - 1);
                    constraint = Bound.LESS_THAN_EQUALS;
                }
            }

            this.lowerBound = lowerBound;
            this.constraint = constraint;
            this.upperBound = upperBound;
        }
        
        // constants for the types of constraints.
        static final int LESS_THAN = 1;
        static final int LESS_THAN_EQUALS = 2;
        
        final Object lowerBound;
        final int constraint;
        final Object upperBound;
        
        public String toString() {
            return "[" + lowerBound.toString() + 
                    ((constraint==LESS_THAN)?" < ":" <= ") +
                    upperBound.toString() + "]";
        }
        
        public boolean equals(Object o) {
            if (o instanceof Bound) {
                Bound b = (Bound)o;
                return (this.lowerBound == b.lowerBound || this.lowerBound.equals(b.lowerBound)) &&
                        this.constraint == b.constraint &&
                        (this.upperBound == b.upperBound || this.upperBound.equals(b.upperBound));
            }
            return false;
        }
        public int hashCode() {
            return lowerBound.hashCode() + upperBound.hashCode() + constraint;
        }
    }
    
    /**
     * BoundSets is a pair of Sets of Bounds, being a set of 
     * upper bounds and a set of lowerbounds. Thus, if b and c are Bounds
     * in the set lowerbounds, then b.upperBound.equals(c.upperBound) == true.
     * Similarly, if b and c are Bounds
     * in the set upperBounds, then b.lowerBound.equals(c.lowerBound) == true.
     */
    static class BoundSets {
        BoundSets(Set lower, Set upper) {
            this.lowerBounds = lower;
            this.upperBounds = upper;
        }
        
        /**
         * Set of Bounds, all with the same lowerBound
         */
        final Set upperBounds;

        /**
         * Set of Bounds, all with the same upperBound
         */
        final Set lowerBounds;
        
        public boolean equals(Object o) {
            if (o instanceof BoundSets) {
                BoundSets bs = (BoundSets)o;
                return (bs == this || ((bs.lowerBounds == this.lowerBounds || bs.lowerBounds.equals(this.lowerBounds)) && 
                                       (bs.upperBounds == this.upperBounds || bs.upperBounds.equals(this.upperBounds))));
            }
            return false;
        }
        public int hashCode() {
            return upperBounds.hashCode() + lowerBounds.hashCode();
        }
        public String toString() {
            return "lower: " + lowerBounds + " upper: " + upperBounds;
        }
    }
    
    /**
     * The items that this dataflow analysis operates on is essetially a set
     * of integer constraints.
     * 
     * There is a Map from LocalInstances (of type int) and 
     * ArrayInstanceLengths to BoundSets, representing the bounds that apply
     * to LocalInstances (of type int) and ArrayInstanceLengths. There is also
     * a map from LocalInstances (of any array type) to ArrayInstanceLengths,
     * representing which object the local variable is pointing to.
     */
    static class DataFlowItem extends Item {
        /**
         * map from LocalInstances (of type int) and ArrayInstanceLength to 
         * BoundSets, representing the bounds that apply
         * to LocalInstances (of type int) and ArrayInstanceLengths.
         */
        final Map integerBounds;
        
        /**
         * map from LocalInstances  (of some array type) to ArrayInstanceLength, 
         * being the current constant representing the array length. If nothing
         * is in the map, then it is assumed that the ArrayInstanceLength
         * with instance == 0 is the appropriate one.
         */
        final Map currentArrayInstances;

        DataFlowItem() {
            integerBounds = Collections.EMPTY_MAP;
            currentArrayInstances = Collections.EMPTY_MAP;
        }
        DataFlowItem(Map integerBounds, Map currentArrayInstances) {
            this.integerBounds = Collections.unmodifiableMap(integerBounds);
            this.currentArrayInstances = Collections.unmodifiableMap(currentArrayInstances);
        }
        DataFlowItem(DataFlowItem d) {
            integerBounds = new HashMap(d.integerBounds);
            currentArrayInstances = new HashMap(d.currentArrayInstances);
        }

        public boolean equals(Object o) {
            if (o instanceof DataFlowItem) {
                return (this.integerBounds == ((DataFlowItem)o).integerBounds || 
                       this.integerBounds.equals(((DataFlowItem)o).integerBounds)) &&
                        (this.currentArrayInstances == ((DataFlowItem)o).currentArrayInstances || 
                       this.currentArrayInstances.equals(((DataFlowItem)o).currentArrayInstances));
            }
            return false;
        }

        public int hashCode() {
            return integerBounds.hashCode();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("\n[integer bounds: \n");
            for (Iterator iter = integerBounds.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry e = (Map.Entry)iter.next();
                sb.append("  " + e.getKey() + ": \n");
                BoundSets bs = (BoundSets)e.getValue();
                for (Iterator iter2 = bs.lowerBounds.iterator(); iter2.hasNext(); ) {
                    Bound b = (Bound)iter2.next();
                    sb.append("    " + b + "\n");
                }
                for (Iterator iter2 = bs.upperBounds.iterator(); iter2.hasNext(); ) {
                    Bound b = (Bound)iter2.next();
                    sb.append("    " + b + "\n");
                }
            }
            sb.append(" current array instances: \n");
            for (Iterator iter = currentArrayInstances.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry e = (Map.Entry)iter.next();
                sb.append("  " + e.getKey() + ": \n");
                ArrayInstanceLength ail = (ArrayInstanceLength)e.getValue();
                sb.append("    " + ail + "\n");                
            }
            sb.append("]");                
            return sb.toString();
        }        
    }


    /**
     * Create an initial Item for the dataflow analysis. By default, the 
     * map of integer bounds is empty.
     */
    protected Item createInitialItem(FlowGraph graph, Term node) {
        return new DataFlowItem();
    }
    
    /**
     * Add two constraints: x <= y and y <= x.
     * 
     * @param inFlow the input data flow
     * @param x
     * @param y
     * @param isModifiableDataFlowItem If true, then this method will alter
     *          the object inFlow.
     * @return the output dataflowitem. If isModifiableDataFlowItem is true,
     *          then the return value is the same object as inFlow.
     */
    private static DataFlowItem addEquals(DataFlowItem inFlow, 
                                          Object x, 
                                          Object y, 
                                          boolean isModifiableDataFlowItem) {
        // if the data flow item inFlow is new, then we can modify it.
        Map m = isModifiableDataFlowItem ? inFlow.integerBounds : 
                                           new HashMap(inFlow.integerBounds);
       addEquals(m, x, y);
     
       return isModifiableDataFlowItem ? inFlow :
                                         new DataFlowItem(m, 
                                                          inFlow.currentArrayInstances);
    }

    /**
     * Add two constraints: x <= y and y <= x
     * @param bounds a Map from LocalInstances (of type int) and 
     *               ArrayInstanceLength to BoundSets, that is, a 
     *               DataFlowItem.integerBounds map.
     * @param x
     * @param y
     */
    private static void addEquals(Map bounds, Object x, Object y) {
        Bound bnd1 = new Bound(x, Bound.LESS_THAN_EQUALS, y);
        Bound bnd2 = new Bound(y, Bound.LESS_THAN_EQUALS, x);
        
        addBound(bounds, bnd1, true);
        addBound(bounds, bnd2, true);        
    }
    
    /**
     * Are we interested in this Object? That is, can it be a key in a
     * DataFlowItem.integerBounds map?
     */
    private static boolean trackThisObject(Object o) {
        if (o instanceof LocalInstance && ((LocalInstance)o).type().isInt()) {
            return true;
        }
        return o instanceof ArrayInstanceLength;
    }
    
    /**
     * Add a bound b to the map bounds, which must be a map suitable for 
     * a DataFlowItem.integerBounds map.
     * 
     * If the bound is "x < y" (or "x <= y"), then this method will add 
     * entries to the map for x, if we 
     * are interested in tracking bounds for x, and similarly for y.
     * If the comparison is "<" (as opposed to "<=") and one of x and y is
     * an Integer, then the bound is converted to a "<=" bound.
     * 
     * if sharedBoundSets == true, then this method will conservatively 
     * create new BoundSets rather than modifying existing BoundSets.
     */
    private static void addBound(Map bounds, Bound b, boolean sharedBoundSets) {
        Object x = b.lowerBound;
        Object y = b.upperBound;
        
        if (trackThisObject(x)) {
            BoundSets s = (BoundSets)bounds.get(x);
            if (s == null) {
                s = new BoundSets(new HashSet(), new HashSet());
                bounds.put(x, s);
            }
            else if (sharedBoundSets) {
                // the bound sets are shared, we need to make a copy before
                // we make any modifications.
                s = new BoundSets(s.lowerBounds, new HashSet(s.upperBounds));
                bounds.put(x, s);
            }
            if (y instanceof Integer) {
                // decide if we want to add y.
                Bound minGreatestIntBound = findExtremeIntegerBound(s, true, false);
                if (minGreatestIntBound == null ||
		    ((Integer)minGreatestIntBound.upperBound).compareTo((Integer)y) > 0) {
                    // we do want to add it
                    s.upperBounds.add(b);                
                    s.upperBounds.remove(minGreatestIntBound);                
                }
            }
            else {
                // y is not an integer.
                s.upperBounds.add(b);                
            }
        }
        if (trackThisObject(y)) {
            BoundSets s = (BoundSets)bounds.get(y);
            if (s == null) {
                s = new BoundSets(new HashSet(), new HashSet());
                bounds.put(y, s);
            }
            else if (sharedBoundSets) {
                // the bound sets are shared, we need to make a copy before
                // we make any modifications.
                s = new BoundSets(new HashSet(s.lowerBounds), s.upperBounds);
                bounds.put(y, s);
            }
            if (x instanceof Integer) {
                // decide if we want to add x.
                Bound maxLowestIntBound = findExtremeIntegerBound(s, false, true);
                if (maxLowestIntBound == null ||
		    ((Integer)maxLowestIntBound.lowerBound).compareTo((Integer)x) < 0) {
                    // we do want to add it
                    s.lowerBounds.add(b);     
                    s.lowerBounds.remove(maxLowestIntBound);     
                }
            }
            else {
                // x is not an integer.
                s.lowerBounds.add(b);                
            }
        }
    }
    
    /**
     * Invalidate all bounds on the tracked object o, in the data flow item
     * flow.
     * 
     * This method is called when the value of the object may have just changed,
     * for example, with an assignment, and thus all bounds based on the value of
     * the object are now invalid.
     */
    private static void invalidateTrackedObject(DataFlowItem flow, Object o) {
        if (!trackThisObject(o)) {
            throw new RuntimeException("We do not track bounds for " + o.getClass().getName());
        }
        BoundSets bs = (BoundSets)flow.integerBounds.get(o);
        if (bs == null) {
            return;
        }
        
        Set lowerBounds = bs.lowerBounds;
        Set upperBounds = bs.upperBounds;
        
        bs = new BoundSets(new HashSet(), new HashSet());
        flow.integerBounds.put(o, bs);
        
        // all the bounds in bs are now invalid, and will be removed. However,
        // first we must make sure that any other pointers to these bounds are
        // also removed.
        for (Iterator iter = lowerBounds.iterator(); iter.hasNext(); ) {
            Bound b = (Bound)iter.next();
            if (trackThisObject(b.lowerBound)) {
                // b.lowerBound is in the map...
                BoundSets bs2 = (BoundSets)flow.integerBounds.get(b.lowerBound);
                if (bs2 != null) {
                    bs2 = new BoundSets(bs2.lowerBounds, new HashSet(bs2.upperBounds));
                    flow.integerBounds.put(b.lowerBound, bs2);
                    bs2.upperBounds.remove(b);
                }
            }
        }
        for (Iterator iter = upperBounds.iterator(); iter.hasNext(); ) {
            Bound b = (Bound)iter.next();
            if (trackThisObject(b.upperBound)) {
                // b.upperBound is in the map...
                BoundSets bs2 = (BoundSets)flow.integerBounds.get(b.upperBound);
                if (bs2 != null) {
                    bs2 = new BoundSets(new HashSet(bs2.lowerBounds), bs2.upperBounds);
                    flow.integerBounds.put(b.upperBound, bs2);
                    bs2.lowerBounds.remove(b);
                }
            }
        }
    }

    /**
     * Handle a local variable being incremented or decremented, that is,
     * modify the bounds on the local variable as appropriate.
     * 
     * @param inFlow the input data flow item.
     * @param li LocalInstance of the local variable
     * @param isIncr true if an increment, false if a decrement.
     * @return output data flow item.
     */
    private static DataFlowItem localIncrDecr(DataFlowItem inFlow,
                                              LocalInstance li, 
                                              boolean isIncr) {
        // get the bounds for li
        BoundSets bs = (BoundSets)inFlow.integerBounds.get(li);
        if (bs == null) {
            return inFlow;
        }
        
        DataFlowItem outFlow = new DataFlowItem(inFlow);
        
        // For an increment:
        //    All the lowerbounds still hold. 
        // For a decrement:
        //    All the upperbounds still hold. 
        BoundSets newBS = isIncr ? new BoundSets(bs.lowerBounds, new HashSet())
                                 : new BoundSets(new HashSet(), bs.upperBounds);
        
        outFlow.integerBounds.put(li, newBS);
        Set bsBounds = isIncr ? bs.upperBounds : bs.lowerBounds; 
        Set newBounds = isIncr ? newBS.upperBounds : newBS.lowerBounds; 
        
        // For an increment:
        //    All LESS THAN upper bounds can change to LESS_THAN_EQUAL
        //    All other constraints must be discarded.
        // For a decrement:
        //    All LESS THAN lower bounds can change to LESS_THAN_EQUAL
        //    All other constraints must be discarded.
        for (Iterator iter = bsBounds.iterator(); iter.hasNext(); ) {
            Bound bound = (Bound)iter.next();

            Object obj = isIncr ? bound.upperBound : bound.lowerBound; 

            if (bound.constraint == Bound.LESS_THAN){
                Bound toAdd = isIncr ? new Bound(li,
                                                 Bound.LESS_THAN_EQUALS,
                                                 bound.upperBound)
                                     : new Bound(bound.lowerBound,
                                                 Bound.LESS_THAN_EQUALS,
                                                 li);

                newBounds.add(toAdd);
            }
            else {
                // all other bounds must be discarded...
                // Clean up any pointers to the bound.
                BoundSets otherBs = (BoundSets)outFlow.integerBounds.get(obj);
                if (otherBs != null) {
                    if (isIncr) {
                        otherBs = new BoundSets(new HashSet(otherBs.lowerBounds), otherBs.upperBounds);
                        otherBs.lowerBounds.remove(bound);
                    }
                    else {
                        otherBs = new BoundSets(otherBs.lowerBounds, new HashSet(otherBs.upperBounds));
                        otherBs.upperBounds.remove(bound);                        
                    }
                    outFlow.integerBounds.put(obj, otherBs);
                }
            }
        }
        
        return outFlow;
    }
    
    /**
     * Handle a local variable being assigned to, that is,
     * modify the bounds on the local variable as appropriate.
     * 
     * @param inFlow the input data flow item.
     * @param li LocalInstance of the local variable
     * @param newVal the new value of the local variable.
     * @return output data flow item.
     */
    private static DataFlowItem localVarAssignment(DataFlowItem inFlow,
                                                   LocalInstance li, 
                                                   Expr newVal) {
        if (li.type().isArray()) {
            // the local instance (presumably of some array type) has just
            // been assigned to a new object.
            if (newVal instanceof Local) {
                // we know something about this thing...
                Object currInstance = inFlow.currentArrayInstances.get(li);
                if (currInstance != null) {
                    Map currArrInst = new HashMap(inFlow.currentArrayInstances);
                    currArrInst.put(li, currInstance);
                    return new DataFlowItem(inFlow.integerBounds, currArrInst);
                }
            }

            DataFlowItem outFlow = new DataFlowItem(inFlow);
            ArrayInstanceLength ail = new ArrayInstanceLength(li, true);
            outFlow.currentArrayInstances.put(li, ail);
            
            if (newVal instanceof ArrayInit) {
                addEquals(outFlow.integerBounds, 
                          ail, 
                          new Integer(((ArrayInit)newVal).elements().size()));
            }
            else if (newVal instanceof NewArray) {
                addEquals(outFlow.integerBounds, 
                          ail, 
                          new Integer(((NewArray)newVal).init().elements().size()));
            }
            else {
                // otherwise, it is too hard, assume it is some new object
                // that we don't know anything about...
            }
            return outFlow;
        } 
        else if (li.type().isInt()) {
            DataFlowItem outFlow = new DataFlowItem(inFlow);

            // All current constraints involving
            // li are now invalidated!!!            
            invalidateTrackedObject(outFlow, li);
            
            Object newValForBound = convertExprToBoundForm(newVal, inFlow);
            if (newValForBound != null) {
                return addEquals(outFlow, li, newValForBound, true);
            }
            else {
                return outFlow;
            }
        }        
        
        // if we fall through to here, then there is no change.
        return inFlow;
    }
    
    /**
     * Implement the flow equations for this dataflow analysis.
     */
    public Map flow(Item in, FlowGraph graph, Term n, Set succEdgeKeys) {
        DataFlowItem dfIn = (DataFlowItem)in;
        DataFlowItem dfOut = dfIn;
        Map inBounds = dfIn.integerBounds;
        
        if (n instanceof LocalDecl) {
            // a new variable is declared. If it is of type int or an array
            // type we will be interested, but the method localVarAssignment
            // will work that out.
            LocalDecl ld = (LocalDecl)n;

            if (ld.init() != null) {
                dfOut = localVarAssignment(dfIn, ld.localInstance(), ld.init());
            }            
        }
        else if (n instanceof Assign) {
            Assign a = (Assign)n;
            if (a.left() instanceof Local) {
                // a local variable has been assigned to. If it is of type 
                // int or an array type we will be interested, but the 
                // method localVarAssignment will work that out.
                dfOut = localVarAssignment(dfIn, ((Local)a.left()).localInstance(), a.right());
            }
        }
        else if (n instanceof Unary) {
            Unary u = (Unary)n;
            if (u.expr() instanceof Local &&
                 (Unary.PRE_INC.equals(u.operator()) ||
                  Unary.POST_INC.equals(u.operator()) ||
                  Unary.PRE_DEC.equals(u.operator()) ||
                  Unary.POST_DEC.equals(u.operator()))) {
                // we are interested in this. It is common in for loops...
                dfOut = localIncrDecr(dfIn, 
                                      ((Local)u.expr()).localInstance(), 
                                      (Unary.PRE_INC.equals(u.operator()) ||
                                       Unary.POST_INC.equals(u.operator())));

            }
        }
        else if (n instanceof Expr && super.hasTrueFalseBranches(succEdgeKeys)) {
            // we have a condition, that is, a branch on an expression.
            Expr e = (Expr)n;
            if (e.type().isBoolean()) {
                return constructItemsFromCondition(e, 
                                     dfIn, 
                                     succEdgeKeys, 
                                     navigator);
            }            
        }
        
        return itemToMap(dfOut, succEdgeKeys);
    }
    
    /**
     * Utility method to find the extreme bound (either min or max bound,
     * and either the upper or lower bound) from all the bounds for
     * boundedObject from a List of DataFlowItems.
     * 
     * @param boundedObject the object whose extreme bound we are looking for
     * @param items a List of DataFlowItems
     * @param findMin if true, find the minimum bound, else find the maximum
     * @param findLowerBound if true, find the extreme lower bound, else find 
     *          the extreme upper bound.
     * @return the Integer that is the extreme bound, null if none exists.
     */
    private static Integer findExtremeIntegerBound(Object boundedObject, 
                                                   List items, 
                                                   boolean findMin, 
                                                   boolean findLowerBound) {
        Integer extremeVal = null; 
        for (Iterator iter = items.iterator(); iter.hasNext(); ) {
            DataFlowItem item = (DataFlowItem)iter.next();
            BoundSets itemBounds = (BoundSets)item.integerBounds.get(boundedObject);

            if (itemBounds == null) {
                // nothing there, we can finish up.
                extremeVal = null;
                break;
            }


            Bound boundFound = findExtremeIntegerBound(itemBounds, findMin, findLowerBound);
            if (boundFound == null) {
                // this bound not found, we can finish up.
                extremeVal = null;
                break;                            
            }

            Integer val = (Integer)(findLowerBound ? boundFound.lowerBound : boundFound.upperBound);
            if (extremeVal == null || val.compareTo(extremeVal) * (findMin ? 1 : -1) < 0) { 
                extremeVal = val;
            }
        }
        return extremeVal;        
    }

    /**
     * Utility method to find the extreme bound (either min or max bound,
     * and either the upper or lower bound) from the bounds in the 
     * BoundSets itemBounds.
     * 
     * @param itemBounds the Bounds to search through
     * @param findMin if true, find the minimum bound, else find the maximum
     * @param findLowerBound if true, find the extreme lower bound, else find 
     *          the extreme upper bound.
     * @return the Integer that is the extreme bound, null if none exists.
     */
    private static Bound findExtremeIntegerBound(BoundSets itemBounds, boolean findMin, boolean findLowerBound) {
        Bound extremeBound = null;
        Integer extremeVal = null;
        
        Set appropriateBounds = (findLowerBound ? itemBounds.lowerBounds : itemBounds.upperBounds);
        
        for (Iterator iter = appropriateBounds.iterator(); iter.hasNext(); ) {
            Bound bnd = (Bound)iter.next();
            Object val = (findLowerBound ? bnd.lowerBound : bnd.upperBound);
            if (val instanceof Integer) {                    
                if (extremeVal == null || 
                    extremeVal.compareTo((Integer)val) * (findMin ? 1 : -1) > 0) { 
                    extremeVal = (Integer)val;
                    extremeBound = bnd;
                }
            }
        }
        
        return extremeBound;
    }

    /**
     * Utility method to determine if the Bound bound exists in all the
     * DataFlowItems contained in items. 
     * 
     * @param items a List of DataFlowItems
     * @param bound the Bound to check for
     * @return true if the Bound is found in all DataFlowItems.
     */
    private static boolean isBoundFoundInAllItems(List items, Bound bound) {
        Object key = bound.lowerBound;
        if (!trackThisObject(key)) {
            key = bound.upperBound;
        }
        boolean boundFoundInAll = true;
        
        for (Iterator iter = items.iterator(); iter.hasNext(); ) {
            DataFlowItem item = (DataFlowItem)iter.next();
            BoundSets itemBounds = (BoundSets)item.integerBounds.get(key);
            if (itemBounds == null) {
                // nothing there, we can finish up.
                boundFoundInAll = false;
                break;
            }
            if (!itemBounds.lowerBounds.contains(bound) && !itemBounds.upperBounds.contains(bound)) {
                // this bound not found, we can finish up.
                boundFoundInAll = false;
                break;                            
            }
        }
        return boundFoundInAll;
    }
    
    
    /**
     * The confluence operator is essentially intersection: a bound must be
     * in all DataFlowItems going into the confluence for it to be valid.
     */
    protected Item confluence(List items, Term node, FlowGraph graph) {
        // there must be at least two items, so we can do this safely...
        DataFlowItem firstItem = (DataFlowItem)items.iterator().next(); 
        
        Map integerBounds = new HashMap();
        Map currentArrayInstances = new HashMap();
        

        // loop through upper bounds && lower bounds
        for (Iterator iter = firstItem.integerBounds.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry e = (Map.Entry)iter.next();
            Object key = e.getKey();
            BoundSets bs = (BoundSets)e.getValue();
            
            // loop through the lower bounds
            for (Iterator iter2 = bs.lowerBounds.iterator(); iter2.hasNext(); ) {
                Bound b = (Bound)iter2.next();
                
                // if it is an integer, then find the integer on the other side, and 
                // take the min lower bound.
                if (b.lowerBound instanceof Integer) {
                    Integer minLowestBound = findExtremeIntegerBound(key, items, true, true);
                    if (minLowestBound != null) {
                        // all the DataFlowItems had the bound! This means that
                        // it stays in...
                        addBound(integerBounds, new Bound(minLowestBound, Bound.LESS_THAN_EQUALS, key), false); 
                    }                    
                }
                // if it is a local instance or an arrayinstancelength, then it has to exist
                // on both sides.
                else {
                    boolean boundFoundInAll = true;
                    if (isBoundFoundInAllItems(items, b)) {
                        // all the DataFlowItems had the bound! This means that
                        // it stays in...
                        addBound(integerBounds, b, false);
                    }                    
                }                
            }

            // loop through the upper bounds 
            for (Iterator iter2 = bs.upperBounds.iterator(); iter2.hasNext(); ) {
                Bound b = (Bound)iter2.next();

                // if it is an integer, then find the integer on the other side, and 
                // take the max upper bound.
                if (b.upperBound instanceof Integer) {
                    Integer maxGreatestBound = findExtremeIntegerBound(key, items, false, false);
                    if (maxGreatestBound != null) {
                        // all the DataFlowItems had the bound! This means that
                        // it stays in...
                        addBound(integerBounds, new Bound(key, Bound.LESS_THAN_EQUALS, maxGreatestBound), false); 
                    }                    
                }
                // if it is a local instance or an arrayinstancelength, then it has to exist
                // on both sides.
                else {
                    boolean boundFoundInAll = true;
                    if (isBoundFoundInAllItems(items, b)) {
                        // all the DataFlowItems had the bound! This means that
                        // it stays in...
                        addBound(integerBounds, b, false);
                    }                    
                }                
            }
            
        }
        
        
        // loop through current array instances 
        Set itemsToCheck = new HashSet(items);
        for (Iterator iter = items.iterator(); iter.hasNext(); ) {
            DataFlowItem item = (DataFlowItem)iter.next();
            itemsToCheck.remove(item);
            
            for (Iterator iter2 = item.currentArrayInstances.entrySet().iterator(); iter2.hasNext(); ) {
                Map.Entry e = (Map.Entry)iter2.next();
                
                if (currentArrayInstances.get(e.getKey()) != null) {
                    // this local variable has already been processedd.
                    continue;
                }
                
                // go through all the other items in search of this local 
                // instance variable, and which array instance it maps to...
                ArrayInstanceLength currentArrayInstance = (ArrayInstanceLength)e.getValue();
                boolean equalInAll = true;
                for (Iterator iter3 = itemsToCheck.iterator(); iter3.hasNext(); ) {
                    DataFlowItem itemToCheck = (DataFlowItem)iter3.next();
                    ArrayInstanceLength toCheckArrayInstance = (ArrayInstanceLength)itemToCheck.currentArrayInstances.get(e.getKey());
                    if (toCheckArrayInstance == null || !toCheckArrayInstance.equals(currentArrayInstance)) {
                        // there is a disagreement between the two items.
                        // put a new array instance in, so that it won't be
                        // equal to any...
                        currentArrayInstances.put(e.getKey(), new ArrayInstanceLength(currentArrayInstance.array, true));
                        equalInAll = false;
                        break;
                    }
                }
                if (equalInAll) {
                    currentArrayInstances.put(e.getKey(), e.getValue());                    
                }
            }
        }
             
        
        return new DataFlowItem(integerBounds, currentArrayInstances);
    }
        
    /**
     * Utility method to find the form of an Expression appropriate to
     * place in a Bound. If the Expression is not suitable to put in a 
     * bound, then return null.
     */
    private static Object convertExprToBoundForm(Expr e, DataFlowItem item) {
        if (e instanceof Field && 
              ((Field)e).target() instanceof Local &&
              ((Field)e).target().type().isArray() &&
              "length".equals(((Field)e).fieldInstance().name())) {
            // This is the length field on a local array...
            LocalInstance li = ((Local)((Field)e).target()).localInstance();
            ArrayInstanceLength ail = (ArrayInstanceLength)item.currentArrayInstances.get(li);
            if (ail == null) {
                ail = new ArrayInstanceLength(li, false);
            }
            return ail;
        }
        if (e.type().isInt()) {
            if (e.isConstant()) {
                // This is a constant integer expression
                return (Integer)e.constantValue();
            }
            if (e instanceof Local) {
                // this is a local variable of type int.
                return ((Local)e).localInstance();
            }
        }        
        return null;
    }
    
    /**
     * This object is a subclass of ConditionNavigator that implements the
     * combine and handleExpression methods for this dataflow analysis.
     * Essentially it looks at integer comparisons, and if the comparisons
     * are on expressions that we are interested in tracking, then we figure
     * out what bounds those comparisons imply, if the condition is true,
     * and if it is false.
     */
    private static final ConditionNavigator navigator = 
        new ConditionNavigator() {
            public Item combine(Item item1, Item item2) {
                DataFlowItem dfi1 = (DataFlowItem)item1;
                DataFlowItem dfi2 = (DataFlowItem)item2;
                
                DataFlowItem combo = new DataFlowItem(dfi1);
                
                // put all the bounds in together
                for (Iterator iter = dfi2.integerBounds.values().iterator(); iter.hasNext(); ) {
                    BoundSets bs = (BoundSets)iter.next();
                    for (Iterator iter2 = bs.lowerBounds.iterator(); iter2.hasNext(); ) {
                        Bound b = (Bound)iter2.next();
                        addBound(combo.integerBounds, b, true);
                    }
                    for (Iterator iter2 = bs.upperBounds.iterator(); iter2.hasNext(); ) {
                        Bound b = (Bound)iter2.next();
                        addBound(combo.integerBounds, b, true);
                    }
                }
                
                
                // put all array instances in together, unless there is a 
                // conflict, in which case, put in a new one.
                for (Iterator iter = dfi2.currentArrayInstances.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry e = (Map.Entry)iter.next();
                    ArrayInstanceLength ail = (ArrayInstanceLength)combo.currentArrayInstances.get(e.getKey());
                    if (ail == null) {
                        combo.currentArrayInstances.put(e.getKey(), e.getValue());
                    }
                    else if (ail == e.getValue() || ail.equals(e.getValue())) {
                        // they are the same instance, don't need to do anything
                    }
                    else {
                        // the instances are different! Be conservative, and put
                        // a new one in instead...
                        combo.currentArrayInstances.put(e.getKey(), new ArrayInstanceLength((LocalInstance)e.getKey(), true));
                    }
                }
                
                return combo;
            }

            public BoolItem handleExpression(Expr expr, Item startingItem) {
                
                //###@@@ I haven't dealt with assignment in a condition. Do I need to explicitly?
                if (expr instanceof Binary) {                    
                    Binary b = (Binary)expr;
                    DataFlowItem dfStartItem = (DataFlowItem)startingItem;
                    Object left = convertExprToBoundForm(b.left(), dfStartItem);
                    Object right = convertExprToBoundForm(b.right(), dfStartItem);
                    if (left != null && right != null) {
                        if (Binary.EQ.equals(b.operator())) {
                            DataFlowItem trueItem = addEquals((DataFlowItem)startingItem,
                                    b.left(),
                                    b.right(),
                                    false);
                            return new BoolItem(trueItem, dfStartItem);
                        }
                        else if (Binary.LE.equals(b.operator()) ||
                                 Binary.LT.equals(b.operator()) ||
                                 Binary.GE.equals(b.operator()) ||
                                 Binary.GT.equals(b.operator())) {
                            DataFlowItem trueItem = new DataFlowItem(dfStartItem);
                            DataFlowItem falseItem = new DataFlowItem(dfStartItem);

                            // set the defaults for LE
                            int trueComparison = Bound.LESS_THAN_EQUALS;
                            int falseComparison = Bound.LESS_THAN;
                            Object trueLeft = left;
                            Object trueRight = right;

                            if (Binary.LT.equals(b.operator())) {
                                trueComparison = Bound.LESS_THAN;
                                falseComparison = Bound.LESS_THAN_EQUALS;
                            }
                            else if (Binary.GE.equals(b.operator())) {
                                trueLeft = right;
                                trueRight = left;
                            }
                            else if (Binary.GT.equals(b.operator())) {
                                trueComparison = Bound.LESS_THAN;
                                falseComparison = Bound.LESS_THAN_EQUALS;
                                trueLeft = right;
                                trueRight = left;
                            }


                            addBound(trueItem.integerBounds, 
                                     new Bound(trueLeft, 
                                     trueComparison, 
                                     trueRight), 
                                     true);
                            addBound(falseItem.integerBounds, 
                                     new Bound(trueRight, 
                                     falseComparison, 
                                     trueLeft), 
                                     true);
                            return new BoolItem(trueItem, falseItem);
                        }
                    }
                }
                return new BoolItem(startingItem, startingItem);
            }       
            
    };
    
    /**
     * Is the value of obj (a local instance or array instance length) 
     * guaranteed to be greater than val, according to the bounds in 
     * integerBounds?
     * 
     * This method performs a depth first search through the integerBounds
     * to determine if this is true.
     * 
     * @param integerBounds
     * @param obj
     * @param val
     * @param visited the set of objects visited so far. Here to prevent infinite loops. 
     *                All non-recursive calls to this method should pass in
     *                "new HashSet()" for this parameter.
     * 
     */
    private static boolean isGreaterThan(Map integerBounds, 
                                         Object obj, 
                                         Integer val,
                                         Set visited) {
            
        if (obj instanceof Integer) {
            return ((Integer)obj).compareTo(val) > 0;
        }
        
        if (visited.contains(obj)) {
            return false;
        }
        
        BoundSets bs = (BoundSets)integerBounds.get(obj);
        if (bs == null) {
            return false;
        }
        
        if (bs.lowerBounds.contains(new Bound(val, Bound.LESS_THAN, obj))) {
                return true;
        }
        
        // nothing for it, we'll have to search...
        visited.add(obj);
        for (Iterator iter = bs.lowerBounds.iterator(); iter.hasNext(); ) {
            Bound b = (Bound)iter.next();
            if (isGreaterThan(integerBounds, b.lowerBound, val, visited)) {
                return true;
            }
        }
        visited.remove(obj);
        return false;
    }
    
    /**
     * Is the value of obj (a local instance or array instance length) 
     * guaranteed to be less than val, according to the bounds in 
     * integerBounds?
     * 
     * This method performs a depth first search through the integerBounds
     * to determine if this is true.
     * 
     * @param integerBounds
     * @param obj
     * @param upper 
     * @param isEqualsOK if true then obj must be less than or equal to upper, 
     *                   otherwise obj must be strictly less than upper.
     * @param intLower an integer lower bound of upper. This obj is less than
     *                 upper if "obj < (or <= as appropraite) upper" is true,
     *                 or if  "obj < (or <= as appropraite) intLower <= upper"
     *                 is true. May be null
     * @param visited the set of objects visited so far. Here to prevent infinite loops. 
     *                All non-recursive calls to this method should pass in
     *                "new HashSet()" for this parameter.
     * 
     */
    private static boolean isLessThan(Map integerBounds, 
                                     Object obj, 
                                     ArrayInstanceLength upper, 
                                     boolean isEqualsOK,
                                     Integer intLower, 
                                     Set visited) {
        if (obj instanceof Integer && intLower != null) {
            if (isEqualsOK) {
                return ((Integer)obj).compareTo(intLower) <= 0;
            }
            else {
                return ((Integer)obj).compareTo(intLower) < 0;                
            }
        }
        
        if (visited.contains(obj)) {
            return false;
        }
        
        BoundSets bs = (BoundSets)integerBounds.get(obj);
        if (bs == null) {
            return false;
        }
        
        if (bs.upperBounds.contains(new Bound(obj, 
                                              Bound.LESS_THAN, 
                                              upper)) ||
            (isEqualsOK && bs.upperBounds.contains(new Bound(obj, 
                                                             Bound.LESS_THAN_EQUALS, 
                                                             upper)))) {
            return true;
        }
        
        // nothing for it, we'll have to search...
        visited.add(obj);
        for (Iterator iter = bs.upperBounds.iterator(); iter.hasNext(); ) {
            Bound b = (Bound)iter.next();
            if (isLessThan(integerBounds, 
                           b.upperBound, 
                           upper, 
                           isEqualsOK || b.constraint == Bound.LESS_THAN,
                           intLower, 
                           visited)) {
                // so we found that the solution holds recursively. Put it 
                // into the bounds, essentially caching it.
                bs.upperBounds.add(new Bound(obj, 
                                             isEqualsOK ? b.constraint : Bound.LESS_THAN, 
                                             upper));
                return true;
            }
        }
        visited.remove(obj);
        return false;
    }
    
    /**
     * For each array access, determine if the array access could throw
     * an ArrayIndexOutOfBoundsException, and annotate the node appropriately.
     */
    protected void check(FlowGraph graph, Term n, Item inItem, Map outItems) throws SemanticException {
        if (n instanceof ArrayAccess) {
            ArrayAccess aa = (ArrayAccess)n;
            DataFlowItem dfIn = (DataFlowItem)inItem;
            boolean accessThrowsException = true;
            if (aa.array() instanceof Local) {
                // need to decide if the array access can throw an 
                // ArrayIndexOutOfBounds exception.
                
                LocalInstance liArray = ((Local)aa.array()).localInstance();
                
                // get the appropriate ArrayInstanceLength for the array.
                ArrayInstanceLength ail = (ArrayInstanceLength)dfIn.currentArrayInstances.get(liArray);
                if (ail == null) {
                    ail = new ArrayInstanceLength(liArray, false);
                }

                // find an integer lower bound the length of the array, if
                // we know about it.
                BoundSets arrayBS = (BoundSets)dfIn.integerBounds.get(ail);
                Integer ailIntLowerBound = null;
                if (arrayBS != null) {
                    Bound ailBound = findExtremeIntegerBound(arrayBS, true, true);
                    if (ailBound != null) {
                        ailIntLowerBound = (Integer)ailBound.lowerBound;
                    }
                }
                
                
                if (aa.index().isConstant()) {
                    // the array index is constant, e.g. "a[3]"
                    Integer index = (Integer)aa.index().constantValue();
                    if (index.intValue() >= 0) {
                        accessThrowsException = !isLessThan(dfIn.integerBounds, 
                                                            index, 
                                                            ail, 
                                                            false,
                                                            ailIntLowerBound, 
                                                            new HashSet());
                    }

                }
                else if (aa.index() instanceof Local) {
                    // the array index is a local variable, e.g. "a[i]"
                    LocalInstance li = ((Local)aa.index()).localInstance();
                    accessThrowsException = !isGreaterThan(dfIn.integerBounds, 
                                                       li, 
                                                       new Integer(-1),
                                                       new HashSet()) ||
                                            !isLessThan(dfIn.integerBounds, 
                                                        li, 
                                                        ail, 
                                                        false,
                                                        ailIntLowerBound, 
                                                        new HashSet());
                }
            }
            if (!accessThrowsException) {
                ((JifArrayAccessDel)aa.del()).setNoOutOfBoundsExcThrown();                
            }
        }
    }    
}
