package jif.types;

import java.util.Collection;
import java.util.Set;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Branch;
import polyglot.types.Context;
import polyglot.types.SemanticException;

/** 
 * The context for Jif type checking. <code>JifContext</code>
 * extends <code>Context</code> with contextual information needed for
 * checking labels, and also with some utility methods to assist in the
 * implementation of label checking. The <code>JifContext</code> object contains
 * a {@link jif.types.hierarchy.LabelEnv label environment}, which contains a 
 * {@link jif.types.hierarchy.PrincipalHierarchy principal hierarchy}; most access to
 * these objects should be through the <code>JifContext</code> object, for 
 * example, the methods <code>addActsFor</code>, <code>actsFor</code>, 
 * <code>addContraintLE</code>.
 */
public interface JifContext extends Context {
    /* ************************************************
     * Label envirnoment methods
     */
    LabelEnv labelEnv();
    
    /**
     * Add a less than or equal assertion to the label environment.
     */
    void addAssertionLE(Label L1, Label L2);

    /**
     * Adds the assertion to this context, and all outer contexts up to
     * the method/constructor/initializer level
     * @param L1
     * @param L2
     */
    void addDefinitionalAssertionLE(Label L1, Label L2);

    /* ************************************************
     * Prinicpal Hierarchy methods
     */
    PrincipalHierarchy ph();
    
    /**
     * Add an actsfor relation to the principal hierarchy. 
     */
    void addActsFor(Principal p1, Principal p2);
    /**
     * Adds the assertion to this context, and all outer contexts up to
     * the method/constructor/initializer level
     */
    void addDefinitionalActsFor(Principal p1, Principal p2);
    /**
     * Test an actsfor relation, using the principal hierarchy. 
     */
    boolean actsFor(Principal p1, Principal p2);
    /**
     * Test an actsfor relation, using the principal hierarchy. 
     */
    boolean actsFor(Collection actorGrp, Collection grantorGrp);
    /**
     * Clears the principal hierarchy of all actsfor relations. 
     */
    void clearPH();
     
     
    /* ************************************************
     * PC and Authority methods
     */
    Label pc();
    void setPc(Label label);

    /**
     * The entry PC is the upper bound on the PC of the caller of the current
     * code.
     */
    Label entryPC();
    void setEntryPC(Label label);

    /**
     * Retrieve the <code>Label</code> associated with branching to the 
     * location <code>label</code>, with the branch kind <code>kind</code>.
     */
    Label gotoLabel(Branch.Kind kind, String label) throws SemanticException;
    /**
     * Record the <code>Label</code> associated with branching to the 
     * location <code>label</code>, with the branch kind <code>kind</code>.
     */
    void gotoLabel(Branch.Kind kind, String label, Label L);

    /**
     * The authority of a class or a procedure is the set of principals
     * who have authorized that code.
     */    
    Set authority();
    void setAuthority(Set authority);
    /**
     * Get the authority of the current code, represented as a privacy label.
     */    
    Label authLabel();



    /* ************************************************
     * Miscellaneous contextual information for label checking
     */
     
    /**
     * Indicates if we are currently checking the initializers within a 
     * constructor. If we are, then more permissive label checking can 
     * be used for field assignments.
     */
    boolean checkingInits();
    
    /**
     * Set whether we are currently checking the initializers within a 
     * constructor.
     */
    void setCheckingInits(boolean checkingInits);

    /**
     * If the current code is a constructor, returns the return label of that
     * constructor. The value returned is only valid if <code>checkingInits</code>
     * is true, and is used for more permissive label checking for field
     * assignments.
     */
    Label constructorReturnLabel();
    void setConstructorReturnLabel(Label Lr);

    /**
     * TODO: DOCO
     * @return
     */
    Context pushConstructorCall();
    
    boolean inConstructorCall();
    
}
