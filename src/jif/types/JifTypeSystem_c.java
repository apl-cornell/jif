package jif.types;

import java.util.*;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.label.*;
import jif.types.principal.*;
import polyglot.ast.*;
import polyglot.ast.FieldDecl;
import polyglot.ast.ProcedureDecl;
import polyglot.ext.jl.types.PrimitiveType_c;
import polyglot.ext.param.types.*;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Source;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifTypeSystem</code> interface. 
 */
public class JifTypeSystem_c
    extends ParamTypeSystem_c
    implements JifTypeSystem {
    private final TypeSystem jlts;

    private final DefaultSignature ds;
    private final Map uidStore; //pos -> uid

    public JifTypeSystem_c(TypeSystem jlts) {
        this.jlts = jlts;
        this.uidStore = new HashMap();
        this.ds = new FixedSignature(this);
    }

    public Object placeHolder(TypeObject o, Set roots) {
        assert_(o);

        if ((o instanceof ClassType) /*&& !(o instanceof SubstType)*/
            ) {
            ClassType ct = (ClassType)o;

            // This should never happen: anonymous and local types cannot
            // appear in signatures.
            if (ct.isLocal() || ct.isAnonymous()) {
                throw new InternalCompilerError("Cannot serialize " + o + ".");
            }

            return new JifPlaceHolder_c(ct);
        }

        return o;
    }

    public Solver solver() {
        return new SolverLUB(this);
        //return new SolverGLB(this);
    }

    public MuPClass mutablePClass(Position pos) {
        return new JifMuPClass_c(this, pos);
    }

    public LazyClassInitializer defaultClassInitializer() {
        return new JifLazyClassInitializer_c(this);
    }

    /**
     * Initializes the type system and its internal constants.
     */
    public void initialize(LoadedClassResolver loadedResolver, ExtensionInfo extInfo)
        throws SemanticException {
        super.initialize(loadedResolver, extInfo);

        PRINCIPAL_ = new PrimitiveType_c(this, PRINCIPAL_KIND);        
        LABEL_ = new PrimitiveType_c(this, LABEL_KIND);

    }

    public UnknownType unknownType(Position pos) {
        UnknownType t = super.unknownType(pos);
        return t;
    }

    public UnknownQualifier unknownQualifier(Position pos) {
        UnknownQualifier t = super.unknownQualifier(pos);
        return t;
    }

    public Package packageForName(Package prefix, String name)
	throws SemanticException
    {
        Package p = super.packageForName(prefix, name);
        return p;
    }

    public ArrayType arrayOf(Position pos, Type type) {
        ArrayType t = super.arrayOf(pos, type);
        return t;
    }

    private static final PrimitiveType.Kind PRINCIPAL_KIND = new PrimitiveType.Kind("principal");
    private static final PrimitiveType.Kind LABEL_KIND = new PrimitiveType.Kind("label");
    protected PrimitiveType PRINCIPAL_;
    protected PrimitiveType LABEL_;

    public PrimitiveType Principal() {
        return PRINCIPAL_;
    }

    public PrimitiveType Label() {
        return LABEL_;
    }

    public Context createContext() {
        return new JifContext_c(this, jlts);
    }

    public InitializerInstance initializerInstance(
        Position pos,
        ClassType container,
        Flags flags) {
        InitializerInstance ii =
            super.initializerInstance(pos, container, flags);
        return ii;
    }

    public FieldInstance fieldInstance(Position pos,
                                       ReferenceType container,
                                       Flags flags,
                                       Type type,
                                       String name) {
        //UID uid = new UID(name);
        UID uid = (UID)uidStore.get(pos);
        if (uid == null) {
            uid = new UID(name);
            uidStore.put(pos, uid);
        }
        if (!isLabeled(type) && container instanceof JifClassType) {
            JifClassType ct = (JifClassType)container;
            type = labeledType(type.position(), type, ct.thisLabel());
        }
        JifFieldInstance_c fi =
            new JifFieldInstance_c(this,
                                   pos,
                                   container,
                                   flags,
                                   type,
                                   name,
                                   uid,
                                   freshLabelVariable(pos, 
                                                      name, 
                                                      "label of field " + 
                                                        container.toString() +
                                                        "." + name).uid(uid));
        return fi;
    }

    public LocalInstance localInstance(
        Position pos,
        Flags flags,
        Type type,
        String name) {
        //HACK: ensure that every pass creates the same uid.
        UID uid = (UID)uidStore.get(pos);
        if (uid == null) {
            uid = new UID(name);
            uidStore.put(pos, uid);
        }

        JifLocalInstance_c li =
            new JifLocalInstance_c(
                this,
                pos,
                flags,
                type,
                name,
                uid);
        return li;
    }

    public ConstructorInstance constructorInstance(
        Position pos,
        ClassType container,
        Flags flags,
        List formalTypes,
        List excTypes) {
        return jifConstructorInstance(pos,container,flags,unknownLabel(pos), false,unknownLabel(pos),false,
            formalTypes,
            excTypes,
            Collections.EMPTY_LIST); 
    }

    public JifConstructorInstance jifConstructorInstance(
        Position pos,
        ClassType container,
        Flags flags,
        Label startLabel,
        boolean isDefaultStartLabel,
        Label returnLabel,
        boolean isDefaultReturnLabel,
        List formalTypes,
        List excTypes,
        List constraints) {
        JifConstructorInstance ci =
            new JifConstructorInstance_c(
                this,
                pos,
                container,
                flags,
                startLabel, isDefaultStartLabel,
                returnLabel, isDefaultReturnLabel,
                formalTypes,
                excTypes,
                constraints);
        return ci;
    }

    public MethodInstance methodInstance(
        Position pos,
        ReferenceType container,
        Flags flags,
        Type returnType,
        String name,
        List formalTypes,
        List excTypes) {

        return jifMethodInstance(
            pos,
            container,
            flags,
            returnType,
            name,
            unknownLabel(pos), false,
            formalTypes,
            unknownLabel(pos), false,
            excTypes,
            Collections.EMPTY_LIST);
    }

    public JifMethodInstance jifMethodInstance(
        Position pos,
        ReferenceType container,
        Flags flags,
        Type returnType,
        String name,
        Label startLabel,
        boolean isDefaultStartLabel,
        List formalTypes,
        Label endLabel,
        boolean isDefaultEndLabel,
        List excTypes,
        List constraints) {

        JifMethodInstance mi =
            new JifMethodInstance_c(
                this,
                pos,
                container,
                flags,
                returnType,
                name,
                startLabel, isDefaultStartLabel,
                formalTypes,
                endLabel, isDefaultEndLabel,
                excTypes,
                constraints);
        return mi;
    }

    public ParamInstance paramInstance(
        Position pos,
        JifClassType container,
        ParamInstance.Kind kind,
        String name) {
        ParamInstance pi =
            new ParamInstance_c(
                this,
                pos,
                container,
                kind,
                name,
                new UID(name));
        return pi;
    }

    public ParamInstance paramInstance(
        Position pos,
        JifClassType container,
        ParamInstance.Kind kind,
        UID uid) {
        ParamInstance pi =
            new ParamInstance_c(this, pos, container, kind, uid.name(), uid);
        return pi;
    }

    public PrincipalInstance principalInstance(
        Position pos,
        ExternalPrincipal principal) {
        PrincipalInstance pi = new PrincipalInstance_c(this, pos, principal);
        return pi;
    }

    public boolean descendsFrom(Type child, Type ancestor) {
        return super.descendsFrom(strip(child), strip(ancestor));
    }

    public boolean isSubtype(Type child, Type ancestor) {
        return super.isSubtype(strip(child), strip(ancestor));
    }

    public boolean isCastValid(Type fromType, Type toType) {
        return super.isCastValid(strip(fromType), strip(toType));
    }

    public boolean isImplicitCastValid(Type fromType, Type toType) {
        return super.isImplicitCastValid(strip(fromType), strip(toType));
    }

    public boolean equals(TypeObject t1, TypeObject t2) {
        if (t1 instanceof Type) {
            t1 = strip((Type)t1);
        }

        if (t2 instanceof Type) {
            t2 = strip((Type)t2);
        }

        return super.equals(t1, t2);
    }

    /**
     * Find out if the least common ancestor of subtype and supertype is
     * supertype, given that strip(subtype) is a sub type of strip(supertype).
     * i.e. check their parameters are appropriate. 
     * @return supertype if supertype is the least common ancestor of subtype
     *     and supertype, null otherwise.
     *  
     */
    protected Type leastCommonAncestorSubtype(Type subtype, Type supertype) {
        while (subtype != null && !equals(subtype, supertype)) {
            subtype = subtype.toClass().superType();                    
        }
        // subtype is now the same type as supertype, when stripped of their 
        // parameters. Now check their parameters.
        Iterator iter1 = ((JifClassType)subtype).actuals().iterator();
        Iterator iter2 = ((JifClassType)supertype).actuals().iterator();
        if (subtype instanceof JifSubstType) {
            Type base = ((JifSubstType)subtype).base();
        }
        
        Iterator iter3 = ((JifClassType)supertype).actuals().iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            Param p1 = (Param)iter1.next();
            Param p2 = (Param)iter2.next();
            if (p1 instanceof Principal && p2 instanceof Principal) {
                if (!((Principal)p1).uid().equals(((Principal)p2).uid())) {
                    // different UIDs...
                    return null;
                }
                 
            }
            if (p1 instanceof Label && p2 instanceof Label) {
                if (!(leq((Label)p1, (Label)p2) && leq((Label)p2, (Label)p1))) {
                    // the labels are not equivalent
                    return null;
                }
            }
            else if (!p1.equals(p2)) {
                // there are two non equal parameters, so we don't have an 
                // appropriate least common ancestor
                return null;
            }
        }
        if (iter1.hasNext() || iter2.hasNext()) {
            // different number of parameters!
            return null; 
        }
        // all the parameters agreed! we've found the least common ancestor!
        return supertype;
    }
    /**
     * Override the superclass implementation, to handle label and principal 
     * parameters, and array base types correctly.
     **/
    public Type leastCommonAncestor(Type type1, Type type2)
        throws SemanticException
    {
        assert_(type1);
        assert_(type2);
        
        type1 = unlabel(type1);
        type2 = unlabel(type2);

        // if one of them is a numeric type, or is a null type, just hand it 
        // off to the superclass
        if (type1.isNumeric() || type2.isNumeric() || 
            type1.isNull() || type2.isNull()) {
            return super.leastCommonAncestor(strip(type1), strip(type2));                            
        }

        // array types
        if (type1.isArray() && type2.isArray()) {
            Type base1 = type1.toArray().base();
            Type base2 = type2.toArray().base();
            if (leq(labelOfType(base1), labelOfType(base2)) && 
                leq(labelOfType(base2), labelOfType(base1))) {
                // Both base types are labelled with the same label.
                // (Either or both types may be unlabelled, in which case
                // we are using the default label).
                 
                return arrayOf(labeledType(base1.position(), 
                                          leastCommonAncestor(unlabel(base1), 
                                                              unlabel(base2)),
                                          labelOfType(base1)));
            }
            else {
                // the labels of the base types are different.
                return Object();
            }
        }
            
        
        if (type1.isReference() && type2.isReference()) {
            // Don't consider interfaces.
            if (type1.isClass() && type1.toClass().flags().isInterface()) {
                return Object();
            }
    
            if (type2.isClass() && type2.toClass().flags().isInterface()) {
                return Object();
            }
            
            // Check against Object to ensure superType() is not null.
            if (equals(type1, Object())) return type1;
            if (equals(type2, Object())) return type2;

            if (!(type1 instanceof JifClassType) || !(type2 instanceof JifClassType)) {
                // this takes care of the case that one but not both are 
                // arraytypes. Array types should be the only possible
                // non-JifClass reference type.
                return Object();
            }
    
                            
            if (isSubtype(type1, type2)) {
                // type1 is a subtype of type2 when stripped of all their 
                // label and principal parameters.
                Type t = leastCommonAncestorSubtype(type1, type2);
                if (t != null) return t;
            }
            if (isSubtype(type2, type1)) {
                // type2 is a subtype of type2 when stripped of all their 
                // label and principal parameters.
                Type t = leastCommonAncestorSubtype(type2, type1);
                if (t != null) return t;
            }
    
            // Walk up the hierarchy
            Type t1 = leastCommonAncestor(type1.toReference().superType(),
                                      type2);
            Type t2 = leastCommonAncestor(type2.toReference().superType(),
                          type1);
    
            if (equals(t1, t2)) return t1;
    
            return Object();
        }
    
        throw new SemanticException(
           "No least common ancestor found for types \"" + type1 +
           "\" and \"" + type2 + "\".");
    }

    public boolean numericConversionValid(Type t, Object value) {
        return super.numericConversionValid(strip(t), value);
    }

    public Resolver bodyContextResolver(ClassType type, Resolver outer) {
        // Since Jif does not support inner classes, all class
        // contexts are empty.  Just return the outer resolver.
        return outer;
    }

    public Resolver classContextResolver(ClassType type) {
        // Since Jif does not support inner classes, all class
        // contexts are empty.  Just return an empty resolver.
        return new TableResolver();
    }

    public ParsedClassType createClassType(LazyClassInitializer init, 
                                           Source fromSource) {
        if (!init.fromClassFile()) {
            return new JifParsedPolyType_c(this, init, fromSource);
        } else {
            return super.createClassType(init, fromSource);
        }
    }

    public List defaultPackageImports() {
        List l = new ArrayList(2);
        l.add("java.lang");
        l.add("jif.lang");
        return l;
    }

    /****** Jif specific stuff ******/

    public LabeledType labeledType(Position pos, Type type, Label label) {
        return new LabeledType_c(this, pos, type, label);
    }

    public PathMap pathMap() {
        return new PathMap(this);
    }

    public PathMap pathMap(Path path, Label L) {
        PathMap m = pathMap();
        return m.set(path, L);
    }

    public ExceptionPath exceptionPath(Type type) {
        return new ExceptionPath_c(type);
    }

    public Path gotoPath(polyglot.ast.Branch.Kind kind, String target) {
        return new GotoPath_c(kind, target);
    }

    public Param unknownParam(Position pos) {
        return new UnknownParam_c(this, pos);
    }

    public ClassType nullInstantiate(Position pos, PClass pc) {
        // We don't want to instantiate on the directly formals: they're UIDs,
        // not Params. We need to create create the params as appropriate.

        if (pc.clazz() instanceof JifPolyType) {
            JifPolyType pt = (JifPolyType) pc.clazz();
        
            Map subst = new HashMap();

            Iterator i = pt.params().iterator();
            Iterator j = pt.actuals().iterator();  
            while (i.hasNext() && j.hasNext()) {      
                ParamInstance param = (ParamInstance) i.next();
                Object actual = j.next();
            
                subst.put(param.uid(), actual);
            }
        
            if (i.hasNext() || j.hasNext()) {
                throw new InternalCompilerError("Params and actuals had " +
                        "different lengths");
            }

            return (ClassType) subst(pt, subst);
        }
        
        throw new InternalCompilerError("Cannot null instantiate \"" +
                                        pc + "\".");
                                        
    }

    public void checkInstantiation(Position pos, PClass t, List args)
        throws SemanticException {
        super.checkInstantiation(pos, t, args);

        // Check that labels are instantiated with labels and principals
        // with principals.
        Iterator i = args.iterator();
        Iterator j = ((JifPolyType)t.clazz()).params().iterator();

        while (i.hasNext() && j.hasNext()) {
            Param p = (Param)i.next();
            ParamInstance pi = (ParamInstance)j.next();
            if (pi.isLabel() && !(p instanceof Label)) {
                throw new SemanticException(
                    "Cannot use " + p + " as a label.",
                    p.position());
            } else if (pi.isPrincipal() && !(p instanceof Principal)) {
                throw new SemanticException(
                    "Cannot use " + p + " as a principal.",
                    p.position());
            }
        }
    }

    public ClassType uncheckedInstantiate(
        Position pos,
        PClass t,
        List actuals) {
        return super.uncheckedInstantiate(pos, t, actuals);
    }

    public Subst subst(Map substMap, Map cache) {
        return new JifSubst_c(this, substMap, cache);
    }

    ////////////////////////////////////////////////////////////////
    // Code for label manipulation

    public VarLabel freshLabelVariable(Position pos, String s, String description) {
        VarLabel t = new VarLabel_c(this, pos, new UID(s));
        t = (VarLabel)t.description(description);
        return t;
    }

    public CovariantParamLabel freshCovariantLabel(Position pos, String s) {
        CovariantParamLabel t = new CovariantParamLabel_c(this, pos, new UID(s));
        return t;
    }

    public ParamPrincipal principalParam(Position pos, UID uid) {
        ParamPrincipal t = new ParamPrincipal_c(this, pos, uid);
        return t;
    }

    public DynamicPrincipal dynamicPrincipal(
        Position pos,
        UID uid,
        String name,
        Label L) {
        DynamicPrincipal t = new DynamicPrincipal_c(this, pos, uid, name, L);
        return t;
    }

    static Map externalUIDs = new HashMap();

    public ExternalPrincipal externalPrincipal(Position pos, String name) {
        // All external principals with the same name should have the same
        // uid.
        UID uid = (UID)externalUIDs.get(name);

        if (uid == null) {
            uid = new UID(name);
            externalUIDs.put(name, uid);
        }

        ExternalPrincipal t = new ExternalPrincipal_c(this, pos, uid, name);
        return t;
    }

    public ArgPrincipal argPrincipal(
        Position pos,
        UID uid,
        String name,
        Label L,
        int index,
        boolean isSignature) {
        ArgPrincipal t = new ArgPrincipal_c(this, pos, uid, name, L, index, isSignature);
        return t;
    }

    public UnknownPrincipal unknownPrincipal(Position pos) {
        UnknownPrincipal t = new UnknownPrincipal_c(this, pos);
        return t;
    }

    private Label top = null;
    private Label bottom = null;
    private Label notTaken = null;
    private Label runtime = null;

    public Label topLabel(Position pos) {
        Label t = new TopLabel_c(this, pos);
        return t;
    }

    public Label topLabel() {
        if (top == null)
            top = topLabel(null);
        return top;
    }

    public Label bottomLabel(Position pos) {
        Label t = new JoinLabel_c(this, pos, Collections.EMPTY_LIST);
        return t;
    }

    public Label bottomLabel() {
        if (bottom == null)
            bottom = bottomLabel(null);
        return bottom;
    }

    public Label notTaken(Position pos) {
        Label t = new NotTaken_c(this, pos);
        return t;
    }

    public Label notTaken() {
        if (notTaken == null)
            notTaken = notTaken(null);
        return notTaken;
    }

    public Label runtimeLabel(Position pos) {
        Label t = new RuntimeLabel_c(this, pos);
        return t;
    }

    public Label runtimeLabel() {
        if (runtime == null)
            runtime = runtimeLabel(null);
        return runtime;
    }

    public Label labelOfVar(Position pos, VarLabel L) {
        Label t = new LabelOfVar_c(this, pos, L);
        return t;
    }

    public CovariantParamLabel covariantLabel(Position pos, UID uid) {
        CovariantParamLabel t = new CovariantParamLabel_c(this, pos, uid);
        return t;
    }

    public ParamLabel paramLabel(Position pos, UID uid) {
        ParamLabel t = new ParamLabel_c(this, pos, uid);
        return t;
    }

    public VarLabel varLabel(Position pos, UID uid) {
        VarLabel t = new VarLabel_c(this, pos, uid);
        return t;
    }

    public PolicyLabel policyLabel(
        Position pos,
        Principal owner,
        Collection readers) {
        PolicyLabel t = new PolicyLabel_c(this, pos, owner, readers);
        return t;
    }

    public JoinLabel joinLabel(Position pos, Collection components) {
        JoinLabel t = new JoinLabel_c(this, pos, components);
        return t;
    }

    public MeetLabel meetLabel(Position pos, Collection components) {
        MeetLabel t = new MeetLabel_c(this, pos, components);
        return t;
    }

    public DynamicLabel dynamicLabel(
        Position pos,
        UID uid,
        String name,
        Label L) {
        DynamicLabel t = new DynamicLabel_c(this, pos, uid, name, L);
        return t;
    }

    public DynrecLabel dynrecLabel(Position pos, UID uid) {
        DynrecLabel t = new DynrecLabel_c(this, pos, uid);
        return t;
    }

    public ArgLabel argLabel(Position pos, LocalInstance li) {
        ArgLabel t = new ArgLabel_c(this, li, pos);
        return t;
    }
//    public DynamicArgLabel dynamicArgLabel(
//        Position pos,
//        UID uid,
//        String name,
//        Label L,
//        int index,
//        boolean isSignature) {
//        DynamicArgLabel t =
//            new DynamicArgLabel_c(this, pos, uid, name, L, index, isSignature);
//        return t;
//    }

    public UnknownLabel unknownLabel(Position pos) {
        UnknownLabel t = new UnknownLabel_c(this, pos);
        return t;
    }

    public ActsForConstraint actsForConstraint(
        Position pos,
        Principal actor,
        Principal granter) {
        return new ActsForConstraint_c(this, pos, actor, granter);
    }

    public AuthConstraint authConstraint(Position pos, List principals) {
        return new AuthConstraint_c(this, pos, principals);
    }

    public CallerConstraint callerConstraint(Position pos, List principals) {
        return new CallerConstraint_c(this, pos, principals);
    }

    public Label labelOfField(FieldInstance vi, Label pc) {
        // pc is not used in Jif (it is used in Split) -- This is a
        // simplifying generalization so we don't have to override as much
        // stuff later on.
        return ((JifFieldInstance)vi).label();
    }

    public Label labelOfLocal(LocalInstance vi, Label pc) {
        // pc is not used in Jif (it is used in Split) -- This is a
        // simplifying generalization so we don't have to override as much
        // stuff later on.
        return ((JifLocalInstance)vi).label();
    }

    public Label labelOfType(Type type) {
        return labelOfType(type, bottomLabel(type.position()));
    }

    public Label labelOfType(Type type, Label defaultLabel) {
        if (type instanceof LabeledType) {
            return ((LabeledType)type).labelPart();
        }
        return defaultLabel;
    }

    protected Type strip(Type type) {
        if (type instanceof LabeledType) {
            return strip(((LabeledType)type).typePart());
        }

        if (type instanceof JifSubstType) {
            return strip(((JifSubstType)type).base());
        }

        if (type instanceof ArrayType) {
            ArrayType at = (ArrayType)type;
            return at.base(strip(at.base())); 
        }
        return type;
    }

    public Type unlabel(Type type) {
        if (type instanceof LabeledType) {
            return ((LabeledType)type).typePart();
        }
        return type;
    }

    public boolean isLabel(Type type) {
        return equals(unlabel(type), LABEL_);
    }

    public boolean isPrincipal(Type type) {
        return equals(unlabel(type), PRINCIPAL_);
    }

    public boolean isLabeled(Type type) {
        return type instanceof LabeledType;
    }

    /** 
     * Returns true if the type is a Jif class (will return false if the type
     * is just a jif signature for a java class).
     * 
     * Currently we determine this by assuming that Jif "source code" for Java
     * classes have been given a private static field, named 
     * "__JIF_SIG_OF_JAVA_CLASS$20030619". Ideally, in the future we would
     * have sufficient infrastructure in Polyglot to simply provide a signature
     * for a Java class, and be able to detect this.
     */
    public boolean isJifClass(Type type) {
        ClassType ct = type.toClass();
        if (ct != null) {
            FieldInstance fi =
                ct.fieldNamed("__JIF_SIG_OF_JAVA_CLASS$20030619");
            if (fi != null
                && fi.flags().isPrivate()
                && fi.flags().isStatic()) {
                return false;
            }
        }

        return true;
    }

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
    public ClassType hasUntrustedAncestor(Type t) {
        if (t == null || t.toReference() == null)
            return null;
            
        Type st = t.toReference().superType();
        if (st == null || st.toClass() == null) {
            return null;
        }
        
        ClassType ct = st.toClass();
        
        if (!isJifClass(ct) && !trustedNonJifClassNames.contains(ct.fullName())) {
            return ct;
        }
        return hasUntrustedAncestor(ct);            
    }
    /**
     * Classnames of "trusted" non jif classes.
     */
    private static List trustedNonJifClassNames = 
        Arrays.asList(new String[] {"java.lang.Error",
                                    "java.lang.Exception",
                                    "java.lang.IllegalArgumentException",
                                    "java.lang.IllegalStateException",
                                    "java.lang.IndexOutOfBoundsException",
                                    "java.lang.Object",
                                    "java.lang.RuntimeException",
                                    "java.lang.SecurityException",
                                    "java.lang.Throwable",
        });
        
    /**
     * In general, type t can be coerced to a String if t is a String, a 
     * primitive, or it has a toString() method.
     */
    public boolean canCoerceToString(Type t, Context c) {
        if (t.isPrimitive() || this.equals(t, this.String())) {
            return true;
        }
        
        // check that t has a toString method
        if (t.isClass()) {
            ClassType ct = t.toClass();
            try {
                this.findMethod(ct, "toString", Collections.EMPTY_LIST, c.currentClass());
                // we were succesfully able to find an appropriate method
                return true;                
            }
            catch (NoMemberException e) { 
                // no toString method. 
                // fall through and return false 
            }
            catch (SemanticException e) {
                throw new InternalCompilerError(
                        "Unexpected semantic exception: " + e.getMessage(),
                         e.position());
            }
        }
        return false;
    }

    public Label join(Label L1, Label L2) {
        if (!L1.isCanonical()) {
            return unknownLabel(L1.position());
        }

        if (!L2.isCanonical()) {
            return unknownLabel(L2.position());
        }

        if (L1 instanceof TopLabel || L1.isTop()) {
            return L1;
        }

        if (L2 instanceof TopLabel || L2.isTop()) {
            return L2;
        }

        if (L1 instanceof NotTaken) {
            return L2;
        }

        if (L2 instanceof NotTaken) {
            return L1;
        }

        if (L1 instanceof RuntimeLabel && L2.isRuntimeRepresentable()) {
            return L1;
        }

        if (L2 instanceof RuntimeLabel && L1.isRuntimeRepresentable()) {
            return L2;
        }

        List l = new ArrayList(2);

        l.add(L1);
        l.add(L2);

        Position pos = L1.position();
        if (pos == null)
            pos = L2.position();

        return joinLabel(pos, l).flatten();
    }

    public boolean leq(Label L1, Label L2, LabelEnv env) {
        return env.leq(L1, L2);
    }

    public boolean leq(Label L1, Label L2) {
        return emptyLabelEnv.leq(L1, L2);
    }

    /** Indirect through the TS so extensions can support new label types. */
    public Label meet(Label L1, Label L2, PrincipalHierarchy ph) {
        if (!L1.isMeetable() || !L2.isMeetable()) {
            throw new InternalCompilerError(
                "Cannot meet " + L1 + " with " + L2 + ".");
        }

        L1 = L1.simplify();
        L2 = L2.simplify();

        if (L1 instanceof TopLabel) {
            return L2;
        }

        if (L2 instanceof TopLabel) {
            return L1;
        }

        if (L1 instanceof RuntimeLabel) {
            if (L2.isRuntimeRepresentable()) {
                return L2;
            } else {
                return bottomLabel(L2.position());
            }
        }

        if (L2 instanceof RuntimeLabel) {
            if (L1.isRuntimeRepresentable()) {
                return L1;
            } else {
                return bottomLabel(L1.position());
            }
        }

        if (!L2.isEnumerable()) {
            throw new InternalCompilerError(
                "Cannot compare non-enumerable " + L2);
        }

        Position pos = L1.position();
        if (pos == null)
            pos = L2.position();

        if (L1 instanceof MeetLabel) {
            // If L1 is a MeetLabel, then ph is null, as otherwise, L1
            // had simplify(ph) called on it, which gets rid of the MeetLabel
            MeetLabel mL1 = (MeetLabel)L1;
            HashSet hs = new HashSet(mL1.components());
            hs.add(L2);
            return meetLabel(pos, hs).flatten();
        }

        if (L2 instanceof MeetLabel) {
            // If L2 is a MeetLabel, then ph is null, as otherwise, L2
            // had simplify(ph) called on it, which gets rid of the MeetLabel
            MeetLabel mL2 = (MeetLabel)L2;
            HashSet hs = new HashSet(mL2.components());
            hs.add(L1);
            return meetLabel(pos, hs).flatten();
        }

        Label result = bottomLabel(pos);

        for (Iterator i = L2.components().iterator(); i.hasNext();) {
            Label c = (Label)i.next();
            if (c instanceof MeetLabel) {
                result = result.join(c.meet_(L1, ph));
            } else {
                result = result.join(L1.meet_(c, ph));
            }
        }

        result = result.simplify();

        return result;
    }

    public String translateClass(Resolver c, ClassType t)
    {
        // Fully qualify classes in jif.lang and jif.principal.
        if (t.package_() != null) {
            if (t.package_().equals(createPackage("jif.lang"))
                || t.package_().equals(createPackage("jif.principal"))) {
                return super.translateClass(null, t);
            }
        }

        return super.translateClass(c, t);
    }

    public String translatePrimitive(Resolver c, PrimitiveType t) {
        if (isLabel(t)) {
            return "jif.lang.Label";
        } else if (isPrincipal(t)) {
            return "jif.lang.Principal";
        } else {
            return super.translatePrimitive(c, t);
        }
    }

    public PrimitiveType primitiveForName(String name)
        throws SemanticException {

        if (name.equals("label"))
            return Label();
        if (name.equals("principal"))
            return Principal();
        return super.primitiveForName(name);
    }

    public Collection uncheckedExceptions() {
        return Collections.singletonList(Error());
    }

    public DefaultSignature defaultSignature() {
        return ds;
    }

    public ConstructorInstance defaultConstructor(
        Position pos,
        ClassType container) {
        assert_(container);
        return jifConstructorInstance(pos,
                                      container,
                                      Public(),
                                      topLabel(), true,
                                      bottomLabel(), true,
                                      Collections.EMPTY_LIST,
                                      Collections.EMPTY_LIST,
                                      Collections.EMPTY_LIST);
    }
}
