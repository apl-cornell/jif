package jif.types;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jif.extension.LabelTypeCheckUtil;
import jif.translate.LabelToJavaExpr;
import jif.translate.PrincipalToJavaExpr;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.AccessPath;
import jif.types.label.ArgLabel;
import jif.types.label.ConfPolicy;
import jif.types.label.CovariantParamLabel;
import jif.types.label.DynamicLabel;
import jif.types.label.IntegPolicy;
import jif.types.label.Label;
import jif.types.label.PairLabel;
import jif.types.label.ParamLabel;
import jif.types.label.Policy;
import jif.types.label.ProviderLabel;
import jif.types.label.ReaderPolicy;
import jif.types.label.ThisLabel;
import jif.types.label.UnknownLabel;
import jif.types.label.VarLabel;
import jif.types.label.WriterPolicy;
import jif.types.label.WritersToReadersLabel;
import jif.types.principal.BottomPrincipal;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import jif.types.principal.TopPrincipal;
import jif.types.principal.UnknownPrincipal;
import jif.types.principal.VarPrincipal;
import polyglot.ast.Expr;
import polyglot.ext.param.types.PClass;
import polyglot.ext.param.types.ParamTypeSystem;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LocalInstance;
import polyglot.types.MemberInstance;
import polyglot.types.PrimitiveType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.VarInstance;
import polyglot.util.Position;

/** Jif type system.
 */
public interface JifTypeSystem extends ParamTypeSystem<ParamInstance, Param> {
    @Override
    JifContext createContext();

    // Type constructors

    /** Returns the "label" type. */
    PrimitiveType Label();

    /**
     * Returns the name of the "principal" type. In Jif, this is
     * "jif.lang.Principal". In languages that extend Jif, this may be different.
     */
    String PrincipalClassName();

    /**
     * Returns the name of the PrincipalUtil class. In Jif, this is
     * "jif.lang.PrincipalUtil". In languages that extend Jif, this may be
     * different.
     */
    String PrincipalUtilClassName();

    /**
     * Returns the name of the "label" type. In Jif, this is
     * "jif.lang.Label". In languages that extend Jif, this may be different.
     */
    String LabelClassName();

    /**
     * Returns the name of the LabelUtil class. In Jif, this is
     * "jif.lang.LabelUtil". In languages that extend Jif, this may be different.
     */
    String LabelUtilClassName();

    /**
     * Returns the name of the Jif runtime package. In Jif, this is
     * "jif.runtime". In languages that extend Jif, this may be different.
     */
    String RuntimePackageName();

    /** Returns the "principal" type. */
    PrimitiveType Principal();

    /** Returns the class jif.lang.Principal. */
    Type PrincipalClass();

    /** Returns a labeled type, type{label}. */
    LabeledType labeledType(Position pos, Type type, Label label);

    ClassType nullInstantiate(Position pos, PClass<ParamInstance, Param> pc);

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

    ConstArrayType constArrayOf(Position position, Type type, int dims,
            boolean castableToNonConst);

    ConstArrayType constArrayOf(Position position, Type type, int dims,
            boolean castableToNonConst, boolean recurseIntoBaseType);

    JifMethodInstance jifMethodInstance(Position pos, ReferenceType container,
            Flags flags, Type returnType, String name, Label startLabel,
            boolean isDefaultStartLabel, List<? extends Type> formalTypes,
            List<Label> formalArgLabels, Label endLabel,
            boolean isDefaultEndLabel, List<? extends Type> excTypes,
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

    Principal conjunctivePrincipal(Position pos, Principal conjunctLeft,
            Principal conjunctRight);

    Principal conjunctivePrincipal(Position pos,
            Collection<Principal> principals);

    Principal disjunctivePrincipal(Position pos, Principal disjunctLeft,
            Principal disjunctRight);

    Principal disjunctivePrincipal(Position pos,
            Collection<Principal> principals);

    Principal pathToPrincipal(Position pos, AccessPath path);

    VarPrincipal freshPrincipalVariable(Position pos, String s,
            String description);

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

    ReaderPolicy readerPolicy(Position pos, Principal owner,
            Collection<Principal> readers);

    WriterPolicy writerPolicy(Position pos, Principal owner, Principal writer);

    WriterPolicy writerPolicy(Position pos, Principal owner,
            Collection<Principal> writers);

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
     * Returns true if the type is signature for a Java class.
     */
    boolean isSignature(Type t);

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

    /**
     * @return object for translating label parameters to Java expressions
     */
    LabelToJavaExpr paramLabelTranslator();

    /**
     * @return object for translating principal parameters to Java expressions
     */
    PrincipalToJavaExpr paramPrincipalTranslator();

    ClassType fatalException();

    /**
     * Create an AccessPath for the expression <code>e</code> in context <code>context</code>.
     */
    AccessPath exprToAccessPath(Expr e, JifContext context)
            throws SemanticException;

    /**
     * Create an AccessPath for the expression <code>e</code> with expected type
     *  <code>expectedType</code> in context <code>context</code>.
     */
    AccessPath exprToAccessPath(Expr e, Type expectedType, JifContext context)
            throws SemanticException;

//    /**
//     * Returns the "effective expression" for expr. That is, it strips
//     * away casts and downgrade expressions.
//     */
//    Expr effectiveExpr(Expr expr);

    String accessPathDescrip(AccessPath path, String kind);

    Principal exprToPrincipal(JifTypeSystem ts, Expr e, JifContext context)
            throws SemanticException;

    Label exprToLabel(JifTypeSystem ts, Expr e, JifContext context)
            throws SemanticException;

    boolean isFinalAccessExpr(Expr e);

    boolean isFinalAccessExprOrConst(Expr e, Type expectedType);

    boolean isFinalAccessExprOrConst(Expr e);

    void processFAP(VarInstance fi, AccessPath path, JifContext A)
            throws SemanticException;

    void processFAP(ReferenceType rt, AccessPath path, JifContext A)
            throws SemanticException;

    AccessPath varInstanceToAccessPath(VarInstance vi, String name,
            Position pos) throws SemanticException;

    AccessPath varInstanceToAccessPath(VarInstance vi, Position pos)
            throws SemanticException;

    /**
     * Returns true if the class has runtime methods for cast and instanceof
     */
    boolean needsDynamicTypeMethods(Type ct);

    /**
     * Returns true if the class has runtime methods for cast and instanceof
     */
    boolean needsImplClass(Type ct);

}
