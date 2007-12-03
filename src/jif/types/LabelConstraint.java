package jif.types;

import java.util.*;

import jif.extension.LabelTypeCheckUtil;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.*;
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
        protected Kind(String name) { super(name); } 
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

    protected final Label lhs;
    protected final Kind kind;
    protected final Label rhs;
    
    /**
     * The environment under which this constraint needs to be satisfied.
     */
    protected final LabelEnv env;

    /**
     * Names for the LHS
     */
    protected final NamedLabel namedLHS;
    
    /**
     * Names for the RHS
     */
    protected final NamedLabel namedRHS;

    protected final Position pos;
    
    /**
     * Do we want to report a violation of this constraint, or report the
     * error for a different constraint?
     */
    protected final boolean report;
    
    /**
     * Error messages
     */
    protected final LabelConstraintMessage messages;

    public LabelConstraint(NamedLabel lhs, Kind kind, NamedLabel rhs, LabelEnv env,
              Position pos, LabelConstraintMessage msg, boolean report) {
        this.lhs = lhs.label;
        this.kind = kind;
        this.rhs = rhs.label;
        this.env = env;
        this.pos = pos;
        this.namedLHS = lhs;
        this.namedRHS = rhs;
        this.messages = msg;
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
    
//    public PrincipalHierarchy ph() {
//	return env.ph();
//    }
    
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
        return messages.msg();
    }
    
    /**
     * A detailed message to display if this constraint is violated.
     * This message may consist of several sentences, and may refer to the
     * names of the labels, if <code>NamedLabel</code>s are used.
     */
    public String detailMsg() {
        return messages.detailMsg();
    }

    /**
     * A technical message to display if this constraint is violated. This
     * message can refer to typing rules to explain what the constraint 
     * represents, and to names of labels, if <code>NamedLabel</code>s are used.
     */
    public String technicalMsg() {
        return messages.technicalMsg();
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
        LabelTypeCheckUtil ltcu = ((JifTypeSystem)lhs.typeSystem()).labelTypeCheckUtil();
        
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
            
            Collection components = ltcu.labelComponents(bound);
            for (Iterator i = components.iterator(); i.hasNext(); ) {
                Label lb = (Label)i.next();
                labelComponents.add(lb);
            }
        }
        
        // in case there are no named labels, add all components of the lhs and
        // rhs bounds.
        Label bound = bounds.applyTo(lhs);
        Collection components = ltcu.labelComponents(bound);
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label lb = (Label)i.next();
            labelComponents.add(lb);
        }

        bound = bounds.applyTo(rhs);
        components = ltcu.labelComponents(bound);
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label l = (Label)i.next();
            labelComponents.add(l);
        }

        // get definitions for the label components.
        for (Iterator iter = labelComponents.iterator(); iter.hasNext(); ) {
            Label l = (Label)iter.next();
            if (l.description() != null) {
                String s = l.componentString();
                if (s.length() == 0)
                    s = l.toString();
                List list = new ArrayList(2); 
                list.add(l.description());
                defns.put(s, list);
                if (l instanceof WritersToReadersLabel) {
                    // add the transform of the writersToReaders label
                    list.add(env.triggerTransforms(l).toString());                    
                }
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
            addLEQEqns(eqns, lhs, rhs);
        }
        else if (kind == EQUAL) {
            addLEQEqns(eqns, lhs, rhs);
            addLEQEqns(eqns, rhs, lhs);
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
    protected void addLEQEqns(Collection eqns, Label left, Label right) {
        left = left.simplify();
        right = right.simplify();
        if (left instanceof JoinLabel) {
            for (Iterator i = ((JoinLabel)left).joinComponents().iterator(); i.hasNext(); ) {
                Label jc = (Label) i.next();
                addLEQEqns(eqns, jc, right);                
            }            
        }
        else if (right instanceof MeetLabel) {
            for (Iterator i = ((MeetLabel)right).meetComponents().iterator(); i.hasNext(); ) {
                Label mc = (Label) i.next();
                addLEQEqns(eqns, left, mc);                
            }                        
        }
        else {
            Equation eqn = new Equation(left, right, this);
            eqns.add(eqn);            
        }
    } 
}
