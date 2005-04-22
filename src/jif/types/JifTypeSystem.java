package jif.types;

import java.util.Collection;
import java.util.List;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv_c;
import jif.types.label.*;
import jif.types.principal.*;
import polyglot.ext.param.types.ParamTypeSystem;
import polyglot.types.*;
import polyglot.util.Position;

/** Jif type system. 
 */
public interface JifTypeSystem extends ParamTypeSystem
{
    static final LabelEnv emptyLabelEnv = new LabelEnv_c();
    
    // Type constructors

    /** Returns the "label" type. */
    PrimitiveType Label();

    /** Returns the "principal" type. */
    PrimitiveType Principal();

    /** Returns a labeled type, type{label}. */
    LabeledType labeledType(Position pos, Type type, Label label);

    /** Constructs a parameter instance for a class parameter declaration */
    ParamInstance paramInstance(Position pos, JifClassType container,
				ParamInstance.Kind kind, String name);    

    /** Constructs a principal instance for an external principal. */
    PrincipalInstance principalInstance(Position pos,
	                                ExternalPrincipal principal);

    /** Tests if the type is "principal". */
    boolean isPrincipal(Type t);

    /** Tests if the type is "label". */
    boolean isLabel(Type t);

    // Path and path map constructors

    PathMap pathMap();
    PathMap pathMap(Path path, Label L);
    ExceptionPath exceptionPath(Type type);
    Path gotoPath(polyglot.ast.Branch.Kind kind, String target);

    // Param constructors

    Param unknownParam(Position pos);

    // Principal constructors

    ParamPrincipal principalParam(Position pos, ParamInstance pi);
    DynamicPrincipal dynamicPrincipal(Position pos, AccessPath path);
    ExternalPrincipal externalPrincipal(Position pos, String name);
    UnknownPrincipal unknownPrincipal(Position pos);

    // Label constructors
    VarLabel freshLabelVariable(Position pos, String s, String description);

    Label topLabel(Position pos);
    Label bottomLabel(Position pos);
    Label notTaken(Position pos);
    Label runtimeLabel(Position pos);

    Label topLabel();
    Label bottomLabel();
    Label notTaken();
    Label runtimeLabel();

    CovariantParamLabel covariantLabel(Position pos, ParamInstance pi);
    ParamLabel paramLabel(Position pos, ParamInstance pi);
    PolicyLabel policyLabel(Position pos, Principal owner, Collection readers);
    Label joinLabel(Position pos, Collection components);
    DynamicLabel dynamicLabel(Position pos, AccessPath path);
    ArgLabel argLabel(Position pos, VarInstance li);
    ThisLabel thisLabel(Position pos, JifClassType ct);
    ThisLabel thisLabel(JifClassType ct);
    UnknownLabel unknownLabel(Position pos);

    /** Returns the join of L1 and L2. */
    Label join(Label L1, Label L2);

    /** Returns true iff L1 <= L2 in <code>ph</code>. */
    boolean leq(Label L1, Label L2, LabelEnv env);

    boolean leq(Label L1, Label L2);

    /** Construct an acts-for constraint. */
    ActsForConstraint actsForConstraint(Position pos, Principal actor, Principal granter);

    /** Construct an authority constraint. */
    AuthConstraint authConstraint(Position pos, List principals);

    /** Construct a caller constraint. */
    CallerConstraint callerConstraint(Position pos, List principals);

    /** Get the label of the field, folding in the PC if appropriate. */
    Label labelOfField(FieldInstance vi, Label pc);

    /** Get the label of the local, folding in the PC if appropriate. */
    Label labelOfLocal(LocalInstance vi, Label pc);

    /** Get the label of the type, or bottom if unlabeled */
    Label labelOfType(Type type);

    /** Get the label of the type, or <code>defaultLabel</code> if unlabeled. */
    Label labelOfType(Type type, Label defaultLabel);

    /** Remove the label from a type, if any. */
    Type unlabel(Type type);

    /** Returns true if the type is labeled. */
    boolean isLabeled(Type type);
    
    /** 
     * Returns true if the type is a Jif class (will return false if the type
     * is just a jif signature for a java class).
     */
    boolean isJifClass(Type t);


    /**
     * Check if the class has an untrusted non-jif ancestor.
     * 
     * An untrusted non-jif ancestor is any non-jif
     * ancestor that is not one of java.lang.Object, java.lang.Throwable,        
     * java.lang.Error, java.lang.Exception, java.lang.IllegalArgumentException,
     * java.lang.IllegalStateException, java.lang.IndexOutOfBoundsException,
     * java.lang.RuntimeException or java.lang.SecurityException.
    
     * 
     * @param t Type to check
     * @return null if ct has no untrusted non-Jif ancestor, and the 
     *  ClassType of an untrusted non-Jif ancestor otherwise. 
     * 
     */
    ClassType hasUntrustedAncestor(Type t);

    /** Returns a new label constraint system solver. */
    Solver solver();

    DefaultSignature defaultSignature();
}
