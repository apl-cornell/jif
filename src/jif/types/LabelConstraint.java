package jif.types;

import java.util.*;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.label.Label;
import jif.types.label.VarLabel;

import polyglot.util.Enum;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** 
 * A <code>LabelConstraint</code> represents a constraint on labels, which 
 * may either be an inequality or equality constraint. 
 * <code>LabelConstraint</code>s are generated during type checking.
 * <code>LabelConstraint</code>s in turn produce {@link Equation Equations}
 * which are what the {@link Solver Solver} will use to find a satisfying 
 * assignment for {@link VarLabel VarLabels}.
 * 
 * A <code>LabelConstraint</code> is not the same as a {@link jif.types.LabelConstraint
 * LabelConstraint}, which by contrast is assumed to be satisfied.
 * 
 */
public class LabelConstraint
{
    /** Kinds of constraint, either equality or inequality. */
    public static class Kind extends Enum {
        private Kind(String name) { super(name); } 
    }

    /**
     * An equality kind of constraint. That is, the constraint requires that 
     * lhs &lt;= rhs and rhs &lt;= lhs. 
     */
    public static final Kind EQUAL = new Kind(" == ");
    
    /**
     * An inequality kind of constraint. That is, the constraint requires that
     * lhs &lt;= rhs.
     */
    public static final Kind LEQ = new Kind(" <= ");

    private final Label lhs;
    private final Kind kind;
    private final Label rhs;
    
    /**
     * The environment under which this constraint needs to be satisfied.
     */
    private final LabelEnv env;

    /**
     * Names for the LHS
     */
    private  NamedLabel namedLHS;
    
    /**
     * Names for the RHS
     */
    private  NamedLabel namedRHS;

    private final Position pos;
    
    /**
     * Do we want to report a violation of this constraint, or report the
     * error for a different constraint?
     */
    private final boolean report;

    public LabelConstraint(NamedLabel lhs, Kind kind, NamedLabel rhs, LabelEnv env,
               Position pos) {
        this(lhs, kind, rhs, env, pos, true);
    }

   public LabelConstraint(NamedLabel lhs, Kind kind, NamedLabel rhs, LabelEnv env,
              Position pos, boolean report) {
        this.lhs = lhs.label;
        this.kind = kind;
        this.rhs = rhs.label;
        this.env = env;
        this.pos = pos;
        this.namedLHS = lhs;
        this.namedRHS = rhs;
        this.report = report;
    }
    
    public Label lhs() {
	return lhs;
    }
    
    public Kind kind() {
        return kind;
    }
    
    public Label rhs() {
	return rhs;
    }
    
    public NamedLabel namedLhs() {
        return namedLHS;
    }
    
    public NamedLabel namedRhs() {
        return namedRHS;
    }
    
    public LabelEnv env() {
	return env;
    }
    
    public PrincipalHierarchy ph() {
	return env.ph();
    }
    
    public Position position() {
        return pos;
    }

    public boolean report() {
        return report;
    }

    /**
     * A message to display if this constraint is violated. This message should
     * be short, and explain without using typing rules what this constraint
     * represents. It should not refer to the names of labels (i.e., names
     * for <code>NamedLabel</code>s.
     */
    public String msg() {
        return null;
    }
    
    /**
     * A detailed message to display if this constraint is violated.
     * This message may consist of several sentences, and may refer to the
     * names of the labels, if <code>NamedLabel</code>s are used.
     */
    public String detailMsg() {
        return msg();
    }

    /**
     * A technical message to display if this constraint is violated. This
     * message can refer to typing rules to explain what the constraint 
     * represents, and to names of labels, if <code>NamedLabel</code>s are used.
     */
    public String technicalMsg() {
        return msg();
    }
        
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(lhs);
        sb.append(kind);
        sb.append(rhs);
        sb.append(" in environment ");
        sb.append(env);        
	
	return sb.toString();
    }
    
    /**
     * Return a map from Strings to Labels, which are the named elements of
     * the left and right hand sides.
     */
    protected Map namedLabels() {
        Map ne = new LinkedHashMap();
        if (namedLHS != null) {
            ne.putAll(namedLHS.nameToLabels);
        }
        if (namedRHS != null) {
            ne.putAll(namedRHS.nameToLabels);
        }
        return ne;
    }

    /**
     * Return a map from Strings to Strings, which are the descriptions of
     * names in the left and right hand sides.
     */
    protected Map namedDescrips() {
        Map ne = new LinkedHashMap();
        if (namedLHS != null) {
            ne.putAll(namedLHS.nameToDescrip);
        }
        if (namedRHS != null) {
            ne.putAll(namedRHS.nameToDescrip);
        }
        return ne;
    }

    /**
     * Returns a Map of Strings to List[String]s which is the definitions/bounds
     * of the NamedLabels, and the description of any components that
     * appear in the NamedLabels. This map is used for verbose output to the
     * user, to help explain the meaning of this constraint.
     */
    public Map definitions(VarMap bounds) {
        Map defns = new LinkedHashMap();

        Set labelComponents = new LinkedHashSet();
        Map namedLabels = this.namedLabels();
        Map namedDescrips = this.namedDescrips();
        for (Iterator iter = namedLabels.keySet().iterator(); 
             iter.hasNext(); ) {
            String s = (String) iter.next();
            List l = new ArrayList(2);
            defns.put(s, l);            

            if (namedDescrips.get(s) != null) {
                l.add(namedDescrips.get(s));
            }
            Label bound = bounds.applyTo((Label)namedLabels.get(s));
            l.add(bound.toString());
            
            if (bound.isEnumerable() && !bound.components().isEmpty()) {                
                for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                    Label lb = (Label)i.next();
                    labelComponents.add(lb);
                }
            }
            else {
                labelComponents.add(bound);                
            }
        }
        
        // in case there are no named labels, add all components of the lhs and
        // rhs bounds.
        Label bound = bounds.applyTo(lhs);
        if (bound.isEnumerable() && !bound.components().isEmpty()) {                
            for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                Label l = (Label)i.next();
                labelComponents.add(l);
            }
        }
        else {
            labelComponents.add(bound);                
        }
        bound = bounds.applyTo(rhs);
        if (bound.isEnumerable() && !bound.components().isEmpty()) {                
            for (Iterator i = bound.components().iterator(); i.hasNext(); ) {
                Label l = (Label)i.next();
                labelComponents.add(l);
            }
        }
        else {
            labelComponents.add(bound);                
        }        

        // get definitions for the label components.
        for (Iterator iter = labelComponents.iterator(); iter.hasNext(); ) {
            Label l = (Label)iter.next();
            if (l.description() != null) {
                String s = l.componentString();
                if (s.length() == 0)
                    s = l.toString();
                defns.put(s, Collections.singletonList(l.description()));
            }
        } 
        
        defns.putAll(env.definitions(bounds, labelComponents));
            
        return defns;
    }

    /**
     * Produce a <code>Collection</code> of {@link Equation Equations} for this
     * constraint.
     */
    public Collection getEquations() {
        Collection eqns = new LinkedList();
        
        if (kind == LEQ) {
            getLEQEqns(eqns, lhs, rhs);
        }
        else if (kind == EQUAL) {
            getLEQEqns(eqns, lhs, rhs);
            getLEQEqns(eqns, rhs, lhs);
        }
        else {
            throw new InternalCompilerError("Unknown kind of equation: " + kind);
        }
        
        return eqns;
        
    }
    
    /**
     * Produce equations that require <code>left</code> to be less than or 
     * equal to <code>right</code>, and add them to <code>eqns</code>.
     */
    protected void getLEQEqns(Collection eqns, Label left, Label right) {
        left = left.simplify();
        for (Iterator i = left.components().iterator(); i.hasNext(); ) {
            Label jc = (Label) i.next();
            
            if (! jc.isSingleton()) {
                throw new InternalCompilerError(
                        "Non-singleton in component list.");
            }
            
            Equation eqn = new Equation(jc, right, this);

                
            eqns.add(eqn);
        }
    } 
}
