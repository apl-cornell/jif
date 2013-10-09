package jif.types;

import java.util.List;

import jif.types.label.ProviderLabel;
import jif.types.label.ThisLabel;
import jif.types.principal.Principal;
import polyglot.types.ClassType;
import polyglot.util.Position;

/**
 * Jif class type.
 */
public interface JifClassType extends ClassType {

    @Override
    JifTypeSystem typeSystem();

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

    ThisLabel thisLabel(Position p);

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
    ProviderLabel provider();

    /**
     * Returns whether this class has been label-checked.
     */
    boolean isUnsafe();
}
