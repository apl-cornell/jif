package jif.types;

import java.util.List;

import jif.types.label.Label;
import jif.types.label.ThisLabel;
import jif.types.principal.Principal;
import polyglot.types.ClassType;

/**
 * Jif class type.
 */
public interface JifClassType extends ClassType {
    /**
     * The principals that grant authority to objects of this class, that is,
     * the principals listed in the authority clause.
     */
    List<Principal> authority();
    /**
     * Constraints on the principal hierarchy.
     */
    List<Assertion> constraints();

    /**
     * The principals whose authority is required by the context that creates a
     * new instance of this class. More precisely, this is (possibly
     * instantiated) parameter principals that are in the authority list. See
     * Andrew's thesis, 4.6.2.
     */
    List<Principal> constructorCallAuthority();

    /**
     * Returns this class's "this" label.
     * 
     * @see ThisLabel
     */
    ThisLabel thisLabel();

    /**
     * Returns a list of <code>Param</code>s, being the parameters with which
     * this class is instantiated.
     * 
     * @return List of Param
     */
    List<Param> actuals();
    
    /**
     * Returns the provider label of the implementation of this class.
     */
    Label provider();
}