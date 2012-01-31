package jif.types;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jif.extension.LabelTypeCheckUtil;
import jif.translate.PrincipalToJavaExpr;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.*;
import jif.types.principal.*;
import polyglot.ext.param.types.PClass;
import polyglot.ext.param.types.ParamTypeSystem;
import polyglot.types.*;
import polyglot.util.Position;

/** Jif type system.
 */
public interface JifTypeSystem extends ParamTypeSystem
{
    ////////////////////////////////////////////////////////////////////////////
    // Constants related to Jif runtime                                       //
    ////////////////////////////////////////////////////////////////////////////

    // Principal
    
    /** Returns the "principal" type. */
    PrimitiveType Principal();

    /** Returns the Principal class's name ("jif.lang.Principal" in Jif). */
    String PrincipalClassName();

    /** Returns the Principal class's type */
    Type   PrincipalClassType();

    // Label
    
    /** Returns the "label" type. */
    PrimitiveType Label();
    
    /** Returns the Label class's name ("jif.lang.Label" in Jif). */
    String LabelClassName();
    
    /** Returns the Label class's type */
    Type   LabelClassType();

    // PrincipalUtil
    
    /** Returns the PrincipalUtil class's name ("jif.lang.PrincipalUtil" in Jif). */
    String PrincipalUtilClassName();

    /** Returns the PrincipalUtil class's type */
    Type   PrincipalUtilClassType();
    
    /** Returns the type of the static method used to perform principal ≽ principal comparisons. */
    JifMethodInstance actsForMethod();
    
    /** Returns the type of the static method used to perform principal equiv principal tests. */
    JifMethodInstance principalEquivMethod();
    
    // LabelUtil
    
    /** Returns the LabelUtil class's name ("jif.lang.LabelUtil" in Jif) */
    String LabelUtilClassName();

    /** Returns the LabelUtil class's type */
    Type   LabelUtilClassType();
    
    /** Returns the type of the static method used to perform label ⊑ label comparisons. */
    JifMethodInstance relabelsToMethod();
    
    /** Returns the type of the static method used to perform principal ≽ label comparisons. */
    JifMethodInstance enforcesMethod();
    
    /** Returns the type of the static method used to perform label ≽ principal comparisons. */
    JifMethodInstance authorizesMethod();
    
    /** Returns the type of the static method used to perform label equiv label tests. */
    JifMethodInstance labelEquivMethod();
    
    // Runtime
    
    /** Returns the Runtime package's name ("jif.runtime" in Jif). */
    String RuntimePackageName();

    ////////////////////////////////////////////////////////////////////////////
    // Type constructors                                                      //
    ////////////////////////////////////////////////////////////////////////////
    
    
    /** Returns a labeled type, type{label}. */
    LabeledType labeledType(Position pos, Type type, Label label);

    ClassType nullInstantiate(Position pos, PClass pc);
    
    /** Constructs a parameter instance for a class parameter declaration */
    ParamInstance paramInstance(Position pos, JifClassType container,
				ParamInstance.Kind kind, String name);

    /** Constructs a principal instance for an external principal. */
    PrincipalInstance principalInstance(Position pos,
	                                ExternalPrincipal principal);

    /* constant array constructors */
    ConstArrayType constArrayOf(Type type);

    ConstArrayType constArrayOf(Position pos, Type type);

    ConstArrayType constArrayOf(Type type, int dims);

    ConstArrayType constArrayOf(Position pos, Type type, int dims);
    ConstArrayType constArrayOf(Position position, Type type, int dims, boolean castableToNonConst);
    ConstArrayType constArrayOf(Position position, Type type, int dims, boolean castableToNonConst, boolean recurseIntoBaseType);
    
    JifMethodInstance jifMethodInstance(Position pos,
            ReferenceType container,
            Flags flags,
            Type returnType,
            String name,
            Label startLabel,
            boolean isDefaultStartLabel,
            List<Type> formalTypes, List<Label> formalArgLabels,
            Label endLabel,
            boolean isDefaultEndLabel,
            List<Type> excTypes,
            List<Assertion> constraints);
    
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
    TopPrincipal topPrincipal(Position pos);
    BottomPrincipal bottomPrincipal(Position pos);
    Principal conjunctivePrincipal(Position pos, Principal conjunctLeft, Principal conjunctRight);
    Principal conjunctivePrincipal(Position pos, Collection<Principal> principals);
    Principal disjunctivePrincipal(Position pos, Principal disjunctLeft, Principal disjunctRight);
    Principal disjunctivePrincipal(Position pos, Collection<Principal> principals);
    Principal pathToPrincipal(Position pos, AccessPath path);

    VarPrincipal freshPrincipalVariable(Position pos, String s, String description);

    // Label constructors
    VarLabel freshLabelVariable(Position pos, String s, String description);

    /**
     * @return the label representing private, untrusted information
     *         ({⊤→⊤;⊥←⊥}).
     */
    Label topLabel(Position pos);

    /**
     * @return the label representing public, trusted information
     *         ({⊥→⊥;⊤←⊤}).
     */
    Label bottomLabel(Position pos);

    /**
     * Constructs a label for the provider of the given class type. This is
     * intended to be used only when initializing JifClassTypes. All other
     * callers should use providerLabel(Position, JifClassType).
     */
    ProviderLabel providerLabel(JifClassType ct);
    
    /**
     * @return a label representing the provider of the given class type.
     */
    ProviderLabel providerLabel(Position position, JifClassType ct);
    
    /**
     * @return the label representing public, untrusted information
     *         ({⊥→⊥;⊥←⊥}).
     */
    Label noComponentsLabel(Position pos);
    
    Label notTaken(Position pos);

    /**
     * @return the label representing private, untrusted information
     *         ({⊤→⊤;⊥←⊥}).
     */
    Label topLabel();

    /**
     * @return the label representing public, trusted information
     *         ({⊥→⊥;⊤←⊤}).
     */
    Label bottomLabel();
    
    /**
     * @return the label representing public, untrusted information
     *         ({⊥→⊥;⊥←⊥}).
     */
    Label noComponentsLabel();
    Label notTaken();

    /* Label methods */
    CovariantParamLabel covariantLabel(Position pos, ParamInstance pi);
    ParamLabel paramLabel(Position pos, ParamInstance pi);
    DynamicLabel dynamicLabel(Position pos, AccessPath path);
    ArgLabel argLabel(Position pos, LocalInstance li, CodeInstance ci);
    ArgLabel argLabel(Position pos, ParamInstance li);
    Label callSitePCLabel(JifProcedureInstance pi);
    ThisLabel thisLabel(Position pos, JifClassType ct);
    ThisLabel thisLabel(JifClassType ct);
    ThisLabel thisLabel(ArrayType ct);
    UnknownLabel unknownLabel(Position pos);
    PairLabel pairLabel(Position pos, ConfPolicy confPol, IntegPolicy integPol);
    WritersToReadersLabel writersToReadersLabel(Position pos, Label L);
    Label pathToLabel(Position pos, AccessPath path);

    ReaderPolicy readerPolicy(Position pos, Principal owner, Principal reader);
    ReaderPolicy readerPolicy(Position pos, Principal owner, Collection<Principal> readers);
    WriterPolicy writerPolicy(Position pos, Principal owner, Principal writer);
    WriterPolicy writerPolicy(Position pos, Principal owner, Collection<Principal> writers);

    /**
     * @return the confidentiality policy representing public information (⊥→⊥).
     */
    ConfPolicy bottomConfPolicy(Position pos);

    /**
     * @return the integrity policy representing trusted information (⊤←⊤).
     */
    IntegPolicy bottomIntegPolicy(Position pos);

    /**
     * @return the confidentiality policy representing private information
     *         (⊤→⊤).
     */
    ConfPolicy topConfPolicy(Position pos);
    
    /**
     * @return the integrity policy representing untrusted information (⊥←⊥).
     */
    IntegPolicy topIntegPolicy(Position pos);

    
    /** Returns true iff L1 <= L2 in the empty environment. */
    boolean leq(Label L1, Label L2);

    /** Returns true iff p actsfor q in the empty environment. */
    boolean actsFor(Principal p, Principal q);

    /** Returns the join of L1 and L2. */
    Label join(Label L1, Label L2);
    Label joinLabel(Position pos, Set<Label> components);

    /** Returns the meet of L1 and L2. */
    Label meet(Label L1, Label L2);
    Label meetLabel(Position pos, Set<Label> components);

    /* methods for policies */
    boolean leq(Policy p1, Policy p2);
    ConfPolicy joinConfPolicy(Position pos, Set<ConfPolicy> components);
    IntegPolicy joinIntegPolicy(Position pos, Set<IntegPolicy> components);
    ConfPolicy meetConfPolicy(Position pos, Set<ConfPolicy> components);
    IntegPolicy meetIntegPolicy(Position pos, Set<IntegPolicy> components);
    ConfPolicy join(ConfPolicy p1, ConfPolicy p2);
    ConfPolicy meet(ConfPolicy p1, ConfPolicy p2);
    IntegPolicy join(IntegPolicy p1, IntegPolicy p2);
    IntegPolicy meet(IntegPolicy p1, IntegPolicy p2);
    
    ConfPolicy confProjection(Label L);
    IntegPolicy integProjection(Label L);
    
    /** Construct an acts-for constraint. */
    <Actor extends ActsForParam, Granter extends ActsForParam> ActsForConstraint<Actor, Granter> actsForConstraint(
            Position pos, Actor actor, Granter granter, boolean isEquiv);

    /** Construct an acts-for constraint. */
    LabelLeAssertion labelLeAssertion(Position pos, Label lhs, Label rhs);

    /** Construct an authority constraint. */
    AuthConstraint authConstraint(Position pos, List<Principal> principals);

    /** Construct a caller constraint. */
    CallerConstraint callerConstraint(Position pos, List<Principal> principals);

    /** Construct an autoendorse constraint. */
    AutoEndorseConstraint autoEndorseConstraint(Position pos, Label endorseTo);

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
     * Returns true if the type is a Jif class, or if it is a non-Jif class
     * that represents parameters at runtime.
     */
    boolean isParamsRuntimeRep(Type t);    


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

    /**
     * Exposes utility method of TypeSystem_c
     */
    List<ReferenceType> abstractSuperInterfaces(ReferenceType rt);

    /**
     * Exposes utility method of TypeSystem_c
     */
    @Override
    boolean isAccessible(MemberInstance mi, ClassType contextClass);

    /** Returns a new label constraint system solver. */
    Solver createSolver(String solverName);
    
    LabelEnv createLabelEnv();

    DefaultSignature defaultSignature();

    /**
     * Compares t1 to t2 without stripping off all the parameters and labels
     */
    boolean equalsNoStrip(TypeObject t1, TypeObject t2);
    /**
     * Compares t1 to t2, stripping off all the parameters and labels
     */
    boolean equalsStrip(TypeObject t1, TypeObject t2);

    LabelTypeCheckUtil labelTypeCheckUtil();

    /**
     * Is the string s a special marker field name?
     */
    boolean isMarkerFieldName(String s);

    /**
     *  Should this exception be promoted to a fatal error? 
     */
    boolean promoteToFatal(Type t);

    /**
     * @return the label {⊤→⊤;p←p}, representing the authority of the given
     *         principal. This can then be used to check whether L actsfor p by
     *         checking whether L <= p.
     */
    Label toLabel(Principal p);

    /**
     * @return class for translating conjunctive principals to java expressions
     */
    PrincipalToJavaExpr conjunctivePrincipalTranslator();
    /**
     * @return class for translating disjunctive principals to java expressions
     */
    PrincipalToJavaExpr disjunctivePrincipalTranslator();
}
