package jif.types.hierarchy;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.util.InternalCompilerError;

/**
 * The wrapper of a set of assumptions that can be used to decide
 * whether L1 &lt;= L2. 
 */
class LabelEnv_c implements LabelEnv
{
    private final PrincipalHierarchy ph;
    private final List assertions;
    private Solver solver;
    
    /**
     * Do any of the assertions have variables in them?
     */
    private boolean hasVariables;
    
    public LabelEnv_c() {
        this(new PrincipalHierarchy(), new LinkedList(), false);
    }
    private LabelEnv_c(PrincipalHierarchy ph, List assertions, boolean hasVariables) {
        this.ph = ph;
        this.assertions = assertions;
        this.hasVariables = false;
        this.solver = null;        
        this.hasVariables = hasVariables;
    }
    
    public void setSolver(Solver s) {
        if (this.solver == null) {
            this.solver = s;
        }
        else if (this.solver != s) {
            throw new InternalCompilerError("LabelEnv given two different solvers");
        }
    }
    
    public PrincipalHierarchy ph() {
        return ph;
    }
    
    public boolean hasVariables() {
        return this.hasVariables;
    }
    
    public void addActsFor(Principal p1, Principal p2) {
        ph.add(p1, p2);
    }
    
    public void addAssertionLE(Label L1, Label L2) {
        // don't bother adding the assertion if we already know 
        // L1 is less than L2,
        if (!(this.leq(L1, L2, false))) {
            assertions.add(new LabelLeAssertion_c(L1, L2));
            if (!this.hasVariables && (((LabelImpl)L1).hasVariables() || ((LabelImpl)L2).hasVariables())) {
                // at least one assertion in this label env has a variable.
                this.hasVariables = true;
            }
        }
    }
    
    public LabelEnv copy() {
        return new LabelEnv_c(ph.copy(), new LinkedList(assertions), hasVariables);
    }
    
    public boolean leq(Label L1, Label L2) {
        return leq(L1, L2, true);
    }
    
    /** Indirect through the TS so extensions can support new label types.
     * Label.leq does not handle non-singleton or non-enumerable labels. */
    protected boolean leq(Label Lb1, Label Lb2, boolean useAssertions
    /*boolean boundVars*/) {
        // simplify the two labels
        LabelImpl L1 = (LabelImpl)((LabelImpl)Lb1).simplify();
        LabelImpl L2 = (LabelImpl)((LabelImpl)Lb2).simplify();
        
        if (L1.isSingleton()) L1 = (LabelImpl)L1.singletonComponent();
        if (L2.isSingleton()) L2 = (LabelImpl)L2.singletonComponent();
        
        if (! L1.isComparable() || ! L2.isComparable()) {
            throw new InternalCompilerError("Cannot compare " + L1 +
                                            " with " + L2 + ".");
        }
        
        // L1 <= L2 if there for all components of L1, there is one component
        // of L2 that is greater.  We need to filter out all L1, and L2
        // that are not enumerable.
        
        if (L1.isBottom()) return true;
        //if (L2.isBottom()) return false;
        
        if (L2.isTop()) return true;
        if (L1.isTop()) return false;
        
        if (L2 instanceof RuntimeLabel) return L1.isRuntimeRepresentable();
        if (L1 instanceof RuntimeLabel) return false; // <= RT and TOP only.
                
        if (L1.equals(L2)) return true;
        
        if (! L1.isEnumerable()) return L1.leq_(L2, this);
        
        if (! L1.isEnumerable() || ! L2.isEnumerable()) {
            throw new InternalCompilerError("Cannot compare " + L1 +
                                            " <= " + L2);
        }
        
        if (L2.isSingleton()) {
            L2 = (LabelImpl)L2.singletonComponent();
            boolean result = L1.leq_(L2, this);
            
            if (result == true || !useAssertions) 
                return result;
            
            // try to use assertions
            for (Iterator i = assertions.iterator(); i.hasNext();) { 
                LabelLeAssertion c = (LabelLeAssertion) i.next();
                // FIXME: keep check of the visited constraints to avoid
                // infinite loops.
                LabelImpl cLHS = (LabelImpl)c.lhs();
                if (cLHS.hasVariables()) 
                    cLHS = (LabelImpl)this.solver.applyBoundsTo(c.lhs());
                LabelImpl cRHS = (LabelImpl)c.rhs();
                if (cRHS.hasVariables()) 
                    cRHS = (LabelImpl)this.solver.applyBoundsTo(c.rhs());
                
                if (leq(L1, cLHS, false) && leq(cRHS, L2, false)) {
                    return true;
                }
            }
            
            return false;
        }
        
        if (L1.isSingleton()) {
            // if the components of L2 are connected by joins, 
            // then L1 <= L2 if there exists a component cj of L2 
            // such that L1 <= cj
            for (Iterator j = L2.components().iterator(); j.hasNext(); ) {
                Label cj = (Label) j.next();
                if (leq(L1, cj, useAssertions)) {
                    return true;
                }
            }
            
            // haven't been able to prove it yet.
            // Try to use the constraints to show that L1 <= L2, even
            // though L2 is not a singleton. This is useful in cases 
            // where the constraints do not have singleton RHS, e.g.
            // trying to prove {L1} <= {L2;L3} when the environment 
            // contains {L1} <= {L2;L3}.
            for (Iterator i = assertions.iterator(); i.hasNext();) { 
                LabelLeAssertion c = (LabelLeAssertion)i.next();
                LabelImpl cLHS = (LabelImpl)c.lhs();
                if (cLHS.hasVariables()) 
                    cLHS = (LabelImpl)this.solver.applyBoundsTo(c.lhs());
                LabelImpl cRHS = (LabelImpl)c.rhs();
                if (cRHS.hasVariables()) 
                    cRHS = (LabelImpl)this.solver.applyBoundsTo(c.rhs());
                // FIXME: keep check of the visited constraints to avoid
                // infinite loops.
                //if (!(c.rhs() instanceof VarLabel) && leq(L1, c.lhs(), false)) {
                if (leq(L1, cLHS, false) && leq(cRHS, L2, false)) {
                    return true;
                }
            }
            return false;
        }
        else {
            // L1 is not a singleton, and neither is L2. 
            // We need to break L1 down...
            // if the components of L1 are connected by joins, 
            // then L1 <= L2 if for all components ci of L1 
            // we have ci <= L2
            for (Iterator i = L1.components().iterator(); i.hasNext(); ) {
                Label ci = (Label) i.next();
                if (!leq(ci, L2, useAssertions)) {
                    return false;
                }
            }	
            return true;
        }            
    }
    
    /**
     * Is this enviornment empty, or does is contain some constraints?
     */
    public boolean isEmpty() {
        return assertions.isEmpty() && ph.isEmpty();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (Iterator i = assertions.iterator(); i.hasNext(); ) {
            LabelLeAssertion c = (LabelLeAssertion) i.next();
            sb.append(c.lhs());
            sb.append(" <= ");
            sb.append(c.rhs());
            if (i.hasNext())
                sb.append(", ");
        }
        if (!ph().isEmpty()) {
            if (!assertions.isEmpty()) {
                sb.append(", ");
            }
            sb.append(ph().actsForString());
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Returns a Map of Strings to List[String]s which is the descriptions of any 
     * components that appear in the environment. This map is used for verbose 
     * output to the user, to help explain the meaning of constraints and 
     * labels.
     * 
     * Seen components is a Set of Labels whose definitions will not be 
     * displayed.
     */
    public Map definitions(VarMap bounds, Set seenComponents) {
        Map defns = new LinkedHashMap();
        
        Set labelComponents = new LinkedHashSet();
        for (Iterator iter = assertions.iterator(); iter.hasNext(); ) {
            LabelLeAssertion c = (LabelLeAssertion) iter.next();
            LabelImpl bound = (LabelImpl)bounds.applyTo(c.lhs());
            if (bound.isEnumerable() && !bound.components().isEmpty()) {                
                for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                    Label l = (Label)i.next();
                    labelComponents.add(l);
                }
            }
            else {
                labelComponents.add(bound);                
            }
            
            bound = (LabelImpl)bounds.applyTo(c.rhs());
            if (bound.isEnumerable() && !bound.components().isEmpty()) {                
                for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                    Label l = (Label)i.next();
                    labelComponents.add(l);
                }
            }            
            else {
                labelComponents.add(bound);                
            }
        }
        
        labelComponents.removeAll(seenComponents);
        
        for (Iterator iter = labelComponents.iterator(); iter.hasNext(); ) {
            LabelImpl l = (LabelImpl)iter.next();
            if (l.description() != null) {
                String s = l.componentString();
                if (s.length() == 0)
                    s = l.toString();
                defns.put(s, Collections.singletonList(l.description()));
            }
        } 
        
        return defns;
    }    
}
