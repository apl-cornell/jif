package jif.types;

import jif.types.LabelConstraint.Kind;


/** 
 * A <code>LabelConstraintMessage</code> provides error messages for 
 * label constraints.
 */
public class LabelConstraintMessage
{
    /**
     * A message to display if the constraint is violated. This message should
     * be short, and explain without using typing rules what this constraint
     * represents. It should not refer to the names of labels (i.e., names
     * for <code>NamedLabel</code>s.
     */
    public String msg() {
        return null;
    }
    
    /**
     * A detailed message to display if the constraint is violated.
     * This message may consist of several sentences, and may refer to the
     * names of the labels, if <code>NamedLabel</code>s are used.
     */
    public String detailMsg() {
        return msg();
    }

    /**
     * A technical message to display if the constraint is violated. This
     * message can refer to typing rules to explain what the constraint 
     * represents, and to names of labels, if <code>NamedLabel</code>s are used.
     */
    public String technicalMsg() {
        return msg();
    }        
    
    private LabelConstraint constraint;
    public void setConstraint(LabelConstraint c) {
        this.constraint = c;
    }
    
    public NamedLabel namedLhs() {
        return constraint.namedLhs();
    }
    public NamedLabel namedRhs() {
        return constraint.namedRhs();        
    }
    public Kind kind() {
        return constraint.kind();        
    }
}
