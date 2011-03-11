package jif.types;

import java.util.*;

import jif.JifOptions;
import jif.extension.LabelTypeCheckUtil;
import jif.translate.*;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv_c;
import jif.types.label.*;
import jif.types.principal.*;
import polyglot.ext.param.types.MuPClass;
import polyglot.ext.param.types.PClass;
import polyglot.ext.param.types.ParamTypeSystem_c;
import polyglot.ext.param.types.Subst;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Source;
import polyglot.main.Options;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLazyClassInitializer;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifTypeSystem</code> interface.
 */
public class JifTypeSystem_c
    extends ParamTypeSystem_c
    implements JifTypeSystem {
    protected final TypeSystem jlts;

    private final LabelEnv emptyLabelEnv = this.createEmptyLabelEnv();

    private final DefaultSignature ds;

    public JifTypeSystem_c(TypeSystem jlts) {
        this.jlts = jlts;
        this.ds = new FixedSignature(this);
    }

    @Override
    public Solver createSolver(String solverName) {
        return new SolverGLB(this, extInfo.compiler(), solverName);
        //return new SolverLUB(this);
    }

    protected LabelEnv createEmptyLabelEnv() {
        return new LabelEnv_c(this, false);
    }

    @Override
    public LabelEnv createLabelEnv() {
        return new LabelEnv_c(this, true);
    }

    @Override
    public MuPClass mutablePClass(Position pos) {
        return new JifMuPClass_c(this, pos);
    }

    @Override
    public LazyClassInitializer defaultClassInitializer() {
        return new JifLazyClassInitializer_c(this);
    }

    /**
     * Initializes the type system and its internal constants.
     */
    @Override
    public void initialize(TopLevelResolver loadedResolver, ExtensionInfo extInfo)
        throws SemanticException {
        super.initialize(loadedResolver, extInfo);

        PRINCIPAL_ = new PrimitiveType_c(this, PRINCIPAL_KIND);
        LABEL_ = new PrimitiveType_c(this, LABEL_KIND);
    }
    
    @Override
    public UnknownType unknownType(Position pos) {
        UnknownType t = super.unknownType(pos);
        return t;
    }

    @Override
    public UnknownQualifier unknownQualifier(Position pos) {
        UnknownQualifier t = super.unknownQualifier(pos);
        return t;
    }

    @Override
    public Package packageForName(Package prefix, String name)
	throws SemanticException
    {
        Package p = super.packageForName(prefix, name);
        return p;
    }

    private static final PrimitiveType.Kind PRINCIPAL_KIND = new PrimitiveType.Kind("principal");
    private static final PrimitiveType.Kind LABEL_KIND = new PrimitiveType.Kind("label");
    protected PrimitiveType PRINCIPAL_;
    protected PrimitiveType LABEL_;
    protected Type PRINCIPAL_CLASS_ = null;

    @Override
    public String PrincipalClassName() { return "jif.lang.Principal"; }
    
    @Override
    public String PrincipalUtilClassName() { return "jif.lang.PrincipalUtil"; }
    
    @Override
    public String LabelClassName() { return "jif.lang.Label"; }
    
    @Override
    public String LabelUtilClassName() { return "jif.lang.LabelUtil"; }
    
    @Override
    public String RuntimePackageName() { return "jif.runtime"; }

    @Override
    public PrimitiveType Principal() {
        return PRINCIPAL_;
    }

    @Override
    public Type PrincipalClass() {
        if (PRINCIPAL_CLASS_ == null) {
            try {
                PRINCIPAL_CLASS_ = typeForName(PrincipalClassName());
            }
            catch (SemanticException e) {
                throw new InternalCompilerError("Cannot find Jif class " + PrincipalClassName(), e);
            } 
        }
        return PRINCIPAL_CLASS_;
    }

    @Override
    public PrimitiveType Label() {
        return LABEL_;
    }

    @Override
    public Context createContext() {
    	return new JifContext_c(this, jlts);
    }
    
    @Override
    public ConstArrayType constArrayOf(Type type) {
        return constArrayOf(type.position(), type);
    }

    @Override
    public ConstArrayType constArrayOf(Position pos, Type type) {
        return constArrayOf(pos, type, false);
    }
    public ConstArrayType constArrayOf(Position pos, Type type, boolean castableToNonConst) {
        return new ConstArrayType_c(this, pos, type, true, castableToNonConst);
    }

    @Override
    public ConstArrayType constArrayOf(Type type, int dims) {
        return constArrayOf(null, type, dims);
    }

    @Override
    public ConstArrayType constArrayOf(Position pos, Type type, int dims) {
        return constArrayOf(pos, type, dims, false);        
    }
    @Override
    public ConstArrayType constArrayOf(Position pos, Type type, int dims, boolean castableToNonConst) {
        return constArrayOf(pos, type, dims, castableToNonConst, false);        
    }
    @Override
    public ConstArrayType constArrayOf(Position pos, Type type, int dims, boolean castableToNonConst, boolean recurseIntoBaseType) {
        if (recurseIntoBaseType && type.isArray()) {
            ArrayType baseArray = type.toArray();
            type = constArrayOf(pos, baseArray.base(), 1, castableToNonConst, recurseIntoBaseType);
        }
        if (dims > 1) {
            return constArrayOf(pos, constArrayOf(pos, type, dims-1, castableToNonConst));
        }
        else if (dims == 1) {
            return constArrayOf(pos, type, castableToNonConst);
        }
        else {
            throw new InternalCompilerError(
            "Must call constArrayOf(type, dims) with dims > 0");
        }
    }
    

    @Override
    protected ArrayType arrayType(Position pos, Type type) {
        if (!isLabeled(type)) {
            type = labeledType(pos, type, defaultSignature().defaultArrayBaseLabel(type));
        }
        return new ConstArrayType_c(this, pos, type, false);
    }

    @Override
    public InitializerInstance initializerInstance(
        Position pos,
        ClassType container,
        Flags flags) {
        InitializerInstance ii =
            super.initializerInstance(pos, container, flags);
        return ii;
    }

    @Override
    public FieldInstance fieldInstance(Position pos,
                                       ReferenceType container,
                                       Flags flags,
                                       Type type,
                                       String name) {
        JifFieldInstance_c fi =
            new JifFieldInstance_c(this, pos, container, flags, type, name);
        return fi;
    }

    @Override
    public LocalInstance localInstance(Position pos, Flags flags, Type type, String name) {
        JifLocalInstance_c li = new JifLocalInstance_c(this, pos, flags, type, name);
        return li;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ConstructorInstance constructorInstance(
        Position pos,
        ClassType container,
        Flags flags,
        List formalTypes,
        List excTypes) {
        return jifConstructorInstance(pos,container,flags,unknownLabel(pos), false,unknownLabel(pos),false,
            formalTypes, Collections.EMPTY_LIST,
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
        List<Type> formalTypes,
        List<Label> formalArgLabels,
        List<Type> excTypes,
        List<Assertion> constraints) {
        JifConstructorInstance ci =
            new JifConstructorInstance_c(
                this,
                pos,
                container,
                flags,
                startLabel, isDefaultStartLabel,
                returnLabel, isDefaultReturnLabel,
                formalTypes, formalArgLabels,
                excTypes,
                constraints);
        return ci;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
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
            formalTypes, Collections.EMPTY_LIST,
            unknownLabel(pos), false,
            excTypes,
            Collections.EMPTY_LIST);
    }

    @Override
    public JifMethodInstance jifMethodInstance(
        Position pos,
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
        List<Assertion> constraints) {

        JifMethodInstance mi =
            new JifMethodInstance_c(
                this,
                pos,
                container,
                flags,
                returnType,
                name,
                startLabel, isDefaultStartLabel,
                formalTypes, formalArgLabels,
                endLabel, isDefaultEndLabel,
                excTypes,
                constraints);
        return mi;
    }

    @Override
    public ParamInstance paramInstance(Position pos, JifClassType container, ParamInstance.Kind kind, String name) {
        ParamInstance pi = new ParamInstance_c(this, pos, container, kind, name);
        return pi;
    }

    @Override
    public PrincipalInstance principalInstance(
        Position pos,
        ExternalPrincipal principal) {
        PrincipalInstance pi = new PrincipalInstance_c(this, pos, principal);
        return pi;
    }

    @Override
    public boolean descendsFrom(Type child, Type ancestor) {
        return super.descendsFrom(strip(child), strip(ancestor));
    }

    @Override
    public boolean isSubtype(Type child, Type ancestor) {
        return super.isSubtype(strip(child), strip(ancestor));
    }

    @Override
    public boolean isCastValid(Type fromType, Type toType) {
        Type strpFromType = strip(fromType);
        Type strpToType = strip(toType);
                
        // can cast from "principal" to any subclass of "jif.lang.Principal"
        if (Principal().equals(strpFromType) && isCastValid(PrincipalClass(), toType)) {
            return true;
        }
        
        // can cast from any subtype of "jif.lang.Principal" to "principal"
        if (Principal().equals(strpToType) && isSubtype(strpFromType, PrincipalClass())) {
            return true;
        }

        return super.isCastValid(strpFromType, strpToType);
    }

    @Override
    public boolean isImplicitCastValid(Type fromType, Type toType) {
        Type strpFromType = strip(fromType);
        Type strpToType = strip(toType);
        
        // can cast from "principal" to "jif.lang.Principal"
        if (Principal().equals(strpFromType) && PrincipalClass().equals(strpToType)) {
            return true;
        }

        // can cast from any subtype of "jif.lang.Principal" to "principal"
        if (Principal().equals(strpToType) && isSubtype(strpFromType, PrincipalClass())) {
            return true;
        }

        return super.isImplicitCastValid(strpFromType, strpToType);
    }

    @Override
    public Type staticTarget(Type t) {
        if (t instanceof JifParsedPolyType) {
                JifParsedPolyType jppt = (JifParsedPolyType)t;
                if (jppt.params().size() > 0) {
                    // return the "null instantiation" of the base type,
                    // to ensure that all TypeNodes contain either
                    // a JifParsedPolyType with zero params, or a
                    // JifSubstClassType
                    return jppt.instantiatedFrom().clazz();
                }
         }
        return super.staticTarget(t);

    }

    @Override
    public boolean equalsNoStrip(TypeObject t1, TypeObject t2) {
        return super.equals(t1, t2);        
    }
    @Override
    public boolean equalsStrip(TypeObject t1, TypeObject t2) {
        if (t1 instanceof Type) {
            t1 = strip((Type)t1);
        }

        if (t2 instanceof Type) {
            t2 = strip((Type)t2);
        }

        return super.equals(t1, t2);        
    }
    
    @Override
    public boolean equals(TypeObject t1, TypeObject t2) {
        return equalsStrip(t1, t2);
    }
    
    @Override
    public boolean typeEquals(Type t1, Type t2) {
        return equals(t1, t2);
    }

    /**
     * Find out if the least common ancestor of subtype and supertype is
     * supertype, given that strip(subtype) is a sub type of strip(supertype).
     * i.e. check their parameters are appropriate.
     * @return supertype if supertype is the least common ancestor of subtype
     *     and supertype, null otherwise.
     */
    protected Type leastCommonAncestorSubtype(Type subtype, Type supertype) {
        while (subtype != null && !equals(subtype, supertype)) {
            subtype = subtype.toClass().superType();
        }
        // subtype is now the same type as supertype, when stripped of their
        // parameters. Now check their parameters.
        Iterator<Param> iter1 = ((JifClassType)subtype).actuals().iterator();
        Iterator<Param> iter2 = ((JifClassType)supertype).actuals().iterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            Param p1 = iter1.next();
            Param p2 = iter2.next();
            if (p1 instanceof Principal && p2 instanceof Principal) {
                if (!((Principal)p1).equals(p2)) {
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
    @Override
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
            Label L1 = labelOfType(base1);
            Label L2 = labelOfType(base2);
            Label arrL = null;
            if (L1 instanceof VarLabel) {
                arrL = L2;
            }
            else if (L2 instanceof VarLabel) {
                arrL = L1;                
            }
            else if (leq(L1, L2) && leq(L2, L1)) { 
                arrL = L1;
            }
            
            if (arrL != null) {
                // Both base types are labelled with the same label.
                // (Either or both types may be unlabelled, in which case
                // we are using the default label).

                return arrayOf(labeledType(base1.position(),
                                          leastCommonAncestor(unlabel(base1),
                                                              unlabel(base2)),
                                          arrL));
            }
            else {
                // the labels of the base types are different.
                return Object();
            }
        }

        if (type1 == type2) {
            return type1;
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

    @Override
    public boolean numericConversionValid(Type t, Object value) {
        return super.numericConversionValid(strip(t), value);
    }

    public Resolver bodyContextResolver(ClassType type, Resolver outer) {
        // Since Jif does not support inner classes, all class
        // contexts are empty.  Just return the outer resolver.
        return outer;
    }

    @Override
    public Resolver classContextResolver(ClassType type) {
        // Since Jif does not support inner classes, all class
        // contexts are empty.  Just return an empty resolver.
        return new TableResolver();
    }

    @Override
    public ParsedClassType createClassType(LazyClassInitializer init,
                                           Source fromSource) {
        if (!init.fromClassFile()) {
            return new JifParsedPolyType_c(this, init, fromSource);
        } else {
            return super.createClassType(init, fromSource);
        }
    }

    @Override
    public List<String> defaultPackageImports() {
        List<String> l = new ArrayList<String>(2);
        l.add("java.lang");
        l.add("jif.lang");
        return l;
    }

	@Override
    public ClassFileLazyClassInitializer classFileLazyClassInitializer(
			ClassFile clazz) {
		throw new UnsupportedOperationException(
				"Raw classfiles are not supported by Jif.");
	}

    /****** Jif specific stuff ******/

    @Override
    public LabeledType labeledType(Position pos, Type type, Label label) {
        if (isLabeled(type)) {
            throw new InternalCompilerError("Trying to label a labeled type");
        }
        return new LabeledType_c(this, pos, type, label);
    }

    @Override
    public PathMap pathMap() {
        return new PathMap(this);
    }

    @Override
    public PathMap pathMap(Path path, Label L) {
        PathMap m = pathMap();
        return m.set(path, L);
    }

    @Override
    public ExceptionPath exceptionPath(Type type) {
        return new ExceptionPath_c(unlabel(type));
    }

    @Override
    public Path gotoPath(polyglot.ast.Branch.Kind kind, String target) {
        return new GotoPath_c(kind, target);
    }

    @Override
    public Param unknownParam(Position pos) {
        return new UnknownParam_c(this, pos);
    }

    @Override
    public ClassType nullInstantiate(Position pos, PClass pc) {
        if (pc.clazz() instanceof JifPolyType) {
            JifPolyType pt = (JifPolyType) pc.clazz();

            Map<ParamInstance, Param> subst =
                    new LinkedHashMap<ParamInstance, Param>();

            Iterator<ParamInstance> i = pt.params().iterator();
            
            // pt.actuals() constructs a list of Params based on the
            // ParamInstances conatained in pt.params().
            // We construct a substitution map from the ParamInstances
            // to their corresponding Params.
            Iterator<Param> j = pt.actuals().iterator();
            while (i.hasNext() && j.hasNext()) {
                ParamInstance param = i.next();
                Param actual = j.next();

                subst.put(param, actual);
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

    @SuppressWarnings("rawtypes")
    @Override
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

    @SuppressWarnings("rawtypes")
    @Override
    public ClassType uncheckedInstantiate(
        Position pos,
        PClass t,
        List actuals) {
        return super.uncheckedInstantiate(pos, t, actuals);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Subst subst(Map substMap, Map cache) {
        return new JifSubst_c(this, substMap, cache);
    }

    ////////////////////////////////////////////////////////////////
    // Code for label manipulation

    @Override
    public VarLabel freshLabelVariable(Position pos, String s, String description) {
        VarLabel t = new VarLabel_c(s, description, this, pos);
        return t;
    }

    @Override
    public VarPrincipal freshPrincipalVariable(Position pos, String s, String description) {
        VarPrincipal t = new VarPrincipal_c(s, description, this, pos);
        return t;
    }

//    public CovariantParamLabel freshCovariantLabel(Position pos, ParamInstance) {
//        CovariantParamLabel t = new CovariantParamLabel_c(this, pos, new UID(s));
//        return t;
//    }

    @Override
    public ParamPrincipal principalParam(Position pos, ParamInstance pi) {
        ParamPrincipal t = new ParamPrincipal_c(pi, this, pos);
        return t;
    }

    @Override
    public DynamicPrincipal dynamicPrincipal(Position pos, AccessPath path) {
        DynamicPrincipal t = new DynamicPrincipal_c(path, this, pos, dynamicPrincipalTranslator());
        return t;
    }
    protected PrincipalToJavaExpr dynamicPrincipalTranslator() {
        return new DynamicPrincipalToJavaExpr_c();
    }

    @Override
    public Principal pathToPrincipal(Position pos, AccessPath path) {
        if (path instanceof AccessPathConstant) {
            AccessPathConstant apc = (AccessPathConstant)path;
            if (!apc.isPrincipalConstant()) {
                throw new InternalCompilerError("Dynamic principal with a constant access path: " + apc);
            }
            return (Principal)apc.constantValue();
        }
        DynamicPrincipal t = new DynamicPrincipal_c(path, this, pos, dynamicPrincipalTranslator());
        return t;
    }

    @Override
    public ExternalPrincipal externalPrincipal(Position pos, String name) {
        ExternalPrincipal t = new ExternalPrincipal_c(name, this, pos);
        return t;
    }

    @Override
    public UnknownPrincipal unknownPrincipal(Position pos) {
        UnknownPrincipal t = new UnknownPrincipal_c(this, pos);
        return t;
    }
    @Override
    public TopPrincipal topPrincipal(Position pos) {
        return new TopPrincipal_c(this, pos);
    }
    @Override
    public BottomPrincipal bottomPrincipal(Position pos) {
        return new BottomPrincipal_c(this, pos);
    }
    @Override
    public Principal conjunctivePrincipal(Position pos, Principal l, Principal r) {
        return conjunctivePrincipal(pos,
                Arrays.asList(new Principal[] { l, r }));
    }
    @Override
    public Principal conjunctivePrincipal(Position pos, Collection<Principal> ps) {
        if (ps.isEmpty()) return bottomPrincipal(pos);
        ps = flattenConjuncts(ps);
        if (ps.size() == 1) return ps.iterator().next();
        return new ConjunctivePrincipal_c(ps, this, pos);
    }
    @Override
    public Principal disjunctivePrincipal(Position pos, Principal l, Principal r) {
        return disjunctivePrincipal(pos,
                Arrays.asList(new Principal[] { l, r }));
    }
    @Override
    public Principal disjunctivePrincipal(Position pos, Collection<Principal> ps) {
        if (ps.isEmpty()) return topPrincipal(pos);
        ps = flattenDisjuncts(ps);
        if (ps.size() == 1) return ps.iterator().next();
        return new DisjunctivePrincipal_c(ps, this, pos);
    }

    private Collection<Principal> flattenConjuncts(Collection<Principal> ps) {
        Set<Principal> newps = new LinkedHashSet<Principal>();
        for (Principal p : ps) {
            if (p instanceof ConjunctivePrincipal) {
                ConjunctivePrincipal cp = (ConjunctivePrincipal)p;
                newps.addAll(cp.conjuncts());
            }
            else {
                newps.add(p);
            }
        }
        Set<Principal> needed = new LinkedHashSet<Principal>();
        for (Principal p : newps) {
            boolean essential = true;
            for (Principal q : needed) {
                if (this.emptyLabelEnv.actsFor(q, p)) {
                    essential = false;
                    break;
    }
            }
            if (essential) needed.add(p);            
        }
        return needed;
    }
    private Collection<Principal> flattenDisjuncts(Collection<Principal> ps) {
        Set<Principal> newps = new LinkedHashSet<Principal>();
        for (Principal p : ps) {
            if (p instanceof DisjunctivePrincipal) {
                DisjunctivePrincipal dp = (DisjunctivePrincipal)p;
                newps.addAll(dp.disjuncts());
            }
            else {
                newps.add(p);
            }
        }
        Set<Principal> needed = new LinkedHashSet<Principal>();
        for (Principal p : newps) {
            boolean essential = true;
            for (Principal q : needed) {
                if (this.emptyLabelEnv.actsFor(p, q)) {
                    essential = false;
                    break;
    }
            }
            if (essential) needed.add(p);            
        }
        return needed;
    }

    private Label top = null;
    private Label bottom = null;
    private Label noComponents = null;
    private Label notTaken = null;
    
    @Override
    public Label topLabel(Position pos) {
        return pairLabel(pos, topConfPolicy(pos), topIntegPolicy(pos));
    }

    @Override
    public Label topLabel() {
        if (top == null)
            top = topLabel(null);
        return top;
    }

    @Override
    public Label bottomLabel(Position pos) {
        return pairLabel(pos, bottomConfPolicy(pos), bottomIntegPolicy(pos));
    }
    
    @Override
    public Label bottomLabel() {
        if (bottom == null)
            bottom = bottomLabel(null);
        return bottom;
    }

    @Override
    public ProviderLabel providerLabel(Position position, JifClassType ct) {
        if (((JifOptions) Options.global).checkProviders) {
            return new ProviderLabel_c(position, ct, providerLabelTranslator());
        } else {
            return null;
        }
    }
    
    protected LabelToJavaExpr providerLabelTranslator() {
        return new ProviderLabelToJavaExpr_c();
    }

    @Override
    public Label noComponentsLabel() {
        if (noComponents == null) {
            noComponents = noComponentsLabel(null);
        }
        return noComponents;
    }
    
    @Override
    public Label noComponentsLabel(Position pos) {
        return pairLabel(pos, bottomConfPolicy(pos), topIntegPolicy(pos));
    }

    @Override
    public Label notTaken(Position pos) {
        Label t = new NotTaken_c(this, pos);
        return t;
    }

    @Override
    public Label notTaken() {
        if (notTaken == null)
            notTaken = notTaken(null);
        return notTaken;
    }

    @Override
    public CovariantParamLabel covariantLabel(Position pos, ParamInstance pi) {
        CovariantParamLabel t = new CovariantParamLabel_c(pi, this, pos);
        return t;
    }

    @Override
    public ParamLabel paramLabel(Position pos, ParamInstance pi) {
        ParamLabel t = new ParamLabel_c(pi, this, pos);
        return t;
    }

    @Override
    public ReaderPolicy readerPolicy(Position pos, Principal owner, Principal reader) {
        ReaderPolicy t = new ReaderPolicy_c(owner, reader, this, pos);
        return t;
    }
    @Override
    public ReaderPolicy readerPolicy(Position pos, Principal owner,
            Collection<Principal> readers) {
        Principal r = disjunctivePrincipal(pos, readers);
        return readerPolicy(pos, owner, r);
    }

    @Override
    public WriterPolicy writerPolicy(Position pos, Principal owner, Principal writer) {
        WriterPolicy t = new WriterPolicy_c(owner, writer, this, pos);
        return t;
    }
    @Override
    public WriterPolicy writerPolicy(Position pos, Principal owner,
            Collection<Principal> writers) {
        Principal w = disjunctivePrincipal(pos, writers);
        return writerPolicy(pos, owner, w);
    }
    @Override
    public ConfPolicy bottomConfPolicy(Position pos) {
        return readerPolicy(pos, bottomPrincipal(pos), bottomPrincipal(pos));
    }
    @Override
    public IntegPolicy bottomIntegPolicy(Position pos) {
        return writerPolicy(pos, topPrincipal(pos), topPrincipal(pos));
    }
    @Override
    public ConfPolicy topConfPolicy(Position pos) {
        return readerPolicy(pos, topPrincipal(pos), topPrincipal(pos));        
    }
    @Override
    public IntegPolicy topIntegPolicy(Position pos) {
        return writerPolicy(pos, bottomPrincipal(pos), bottomPrincipal(pos));        
    }
    
        
    @Override
    public Label joinLabel(Position pos, Set<Label> components) {
        if (components == null) {
            components = Collections.emptySet();
        }
        if (components.isEmpty()) {
            return bottomLabel(pos);
        }
        if (components.size() == 1) {
            return components.iterator().next();
        }
        Label t = new JoinLabel_c(components, this, pos, joinLabelTranslator());
        return t;
    }

    public LabelToJavaExpr joinLabelTranslator() {
        return new JoinLabelToJavaExpr_c();
    }

    @Override
    public Label meetLabel(Position pos, Set<Label> components) {
        if (components == null) {
            components = Collections.emptySet();
        }
        if (components.isEmpty()) {
            return topLabel(pos);
        }
        if (components.size() == 1) {
            return components.iterator().next();
        }
        Label t = new MeetLabel_c(components, this, pos, meetLabelTranslator());
        return t;
    }

    public LabelToJavaExpr meetLabelTranslator() {
        return new MeetLabelToJavaExpr_c();
    }

    @Override
    public DynamicLabel dynamicLabel(Position pos, AccessPath path) {
        DynamicLabel t = new DynamicLabel_c(path, this, pos, dynamicLabelTranslator());
        return t;
    }

    @Override
    public Label pathToLabel(Position pos, AccessPath path) {
        if (path instanceof AccessPathConstant) {
            AccessPathConstant apc = (AccessPathConstant)path;
            if (!apc.isLabelConstant()) {
                throw new InternalCompilerError("Dynamic label with a constant access path: " + apc);
            }
            return (Label)apc.constantValue();
        }
        
        DynamicLabel t = dynamicLabel(pos, path);
        return t;
    }
    
    protected LabelToJavaExpr dynamicLabelTranslator() {
        return new DynamicLabelToJavaExpr_c();
    }


    @Override
    public ArgLabel argLabel(Position pos, LocalInstance vi, CodeInstance ci) {
        ArgLabel t = new ArgLabel_c(this, vi, ci, pos);
        return t;
    }
    @Override
    public ArgLabel argLabel(Position pos, ParamInstance pi) {
        ArgLabel t = new ArgLabel_c(this, pi, null, pos);
        return t;
    }
    
    @Override
    public Label callSitePCLabel(JifProcedureInstance pi) {
        ArgLabel pcLabel = new ArgLabel_c(this, pi, "caller_pc", pi.position());
        pcLabel.setUpperBound(pi.pcBound());
        pcLabel.setDescription("The pc at the call site of this " + 
                               pi.designator() + " (bounded above by " +
                               pi.pcBound() + ")");
        return pcLabel;
    }

    @Override
    public ThisLabel thisLabel(JifClassType ct) {
        return thisLabel(ct.position(), ct);
    }
    @Override
    public ThisLabel thisLabel(ArrayType at) {
        return thisLabel(at.position(), at);
    }

    @Override
    public ThisLabel thisLabel(Position pos, JifClassType ct) {
        return thisLabel(pos, (ReferenceType)ct);
    }
    public ThisLabel thisLabel(Position pos, ReferenceType ct) {
        return new ThisLabel_c(this, ct, pos);
    }

    @Override
    public UnknownLabel unknownLabel(Position pos) {
        UnknownLabel t = new UnknownLabel_c(this, pos);
        return t;
    }
    
    @Override
    public PairLabel pairLabel(Position pos, 
                               ConfPolicy confPol, 
                               IntegPolicy integPol) {
        return new PairLabel_c(this, confPol, integPol, pos, pairLabelTranslator());
    }
    
    protected LabelToJavaExpr pairLabelTranslator() {
        return new PairLabelToJavaExpr_c();
    }

    @Override
    public WritersToReadersLabel writersToReadersLabel(Position pos, Label L) {
        WritersToReadersLabel t = new WritersToReadersLabel_c(L, this, pos);
        return t;
    }

    @Override
    public <Actor extends ActsForParam, Granter extends ActsForParam> ActsForConstraint<Actor, Granter> actsForConstraint(
            Position pos, Actor actor, Granter granter, boolean isEquiv) {
        return new ActsForConstraint_c<Actor, Granter>(this, pos, actor,
                granter, isEquiv);
    }

    @Override
    public LabelLeAssertion labelLeAssertion(Position pos,
                                             Label lhs,
                                             Label rhs) {
        return new LabelLeAssertion_c(this, lhs, rhs, pos);
    }

    @Override
    public AuthConstraint authConstraint(Position pos,
            List<Principal> principals) {
        return new AuthConstraint_c(this, pos, principals);
    }

    @Override
    public AutoEndorseConstraint autoEndorseConstraint(Position pos, Label endorseTo) {
        return new AutoEndorseConstraint_c(this, pos, endorseTo);
    }

    @Override
    public CallerConstraint callerConstraint(Position pos,
            List<Principal> principals) {
        return new CallerConstraint_c(this, pos, principals);
    }

    @Override
    public Label labelOfField(FieldInstance vi, Label pc) {
        // pc is not used in Jif (it is used in Split) -- This is a
        // simplifying generalization so we don't have to override as much
        // stuff later on.
        return ((JifFieldInstance)vi).label();
    }

    @Override
    public Label labelOfLocal(LocalInstance vi, Label pc) {
        // pc is not used in Jif (it is used in Split) -- This is a
        // simplifying generalization so we don't have to override as much
        // stuff later on.
        return ((JifLocalInstance)vi).label();
    }

    @Override
    public Label labelOfType(Type type) {
        return labelOfType(type, bottomLabel(type.position()));
    }

    @Override
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
    
    @Override
    public Type unlabel(Type type) {
        if (type instanceof LabeledType) {
            return ((LabeledType)type).typePart();
        }
        return type;
    }

    @Override
    public boolean isLabel(Type type) {
        return equals(unlabel(type), LABEL_);
    }

    @Override
    public boolean isPrincipal(Type type) {
        return equals(unlabel(type), PRINCIPAL_);
    }

    @Override
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
    @Override
    public boolean isJifClass(Type type) {
        ClassType ct = type.toClass();
        if (ct != null) {
            FieldInstance fi = ct.fieldNamed(JIF_SIG_OF_JAVA_MARKER);
            if (fi != null
                && (fi.flags().isPrivate() || ct.flags().isInterface())
                && fi.flags().isStatic()) {
                return false;
            }
        }

        return true;
    }
    
    @Override
    public boolean isMarkerFieldName(String s) {
        return JIF_SIG_OF_JAVA_MARKER.equals(s) ||
               JIF_PARAMS_RUNTIME_MARKER.equals(s) ||
               JIF_SAFE_CONSTRUCTOR_MARKER.equals(s);
    }
    static String JIF_SIG_OF_JAVA_MARKER = "__JIF_SIG_OF_JAVA_CLASS$20030619";
    static String JIF_PARAMS_RUNTIME_MARKER = "__JIF_PARAMS_RUNTIME_REPRESENTED$20051007";
    static String JIF_SAFE_CONSTRUCTOR_MARKER = "__JIF_SAFE_CONSTRUCTORS$20050907";
    
    @Override
    public boolean isParamsRuntimeRep(Type t) {
        if (isJifClass(t)) {
            return true;
        }

        ClassType ct = t.toClass();
        if (ct != null) {
            FieldInstance fi = ct.fieldNamed(JIF_PARAMS_RUNTIME_MARKER);
            if (fi != null
                && fi.flags().isPrivate()
                && fi.flags().isStatic()) {
                return true;
            }
        }

        return false;        
    }

    /**
     * Check if the class has an untrusted non-jif ancestor.
     * 
     * An untrusted non-jif ancestor is any non-jif ancestor whose constructors
     * may potentially access a final field (which may be on a subclass) before
     * that field has been intitialized.
     * 
     * A special marker field is used in Jif signature files for Java classes to
     * declare that all constructors of the Java file are okay.
     * 
     * @param t Type to check
     * @return null if ct has no untrusted non-Jif ancestor, and the ClassType
     *         of an untrusted non-Jif ancestor otherwise.
     *  
     */
    @Override
    public ClassType hasUntrustedAncestor(Type t) {
        if (t == null || t.toReference() == null) {
            return null;
	}

        Type st = t.toReference().superType();
        if (st == null || st.toClass() == null) {
            return null;
        }

        ClassType ct = st.toClass();
        if (!hasSafeConstructors(ct)) {
            return ct;
        }
        return hasUntrustedAncestor(ct);
    }

    /**
     * Check if the class has safe constructors, that is, if the constructors of
     * the class definitely do not access a final field (possibly on a subclass)
     * before that field has been initalized. All Jif classes are not untrusted;
     * Java classes can be explicitly marked as not by a specially marker field.
     */
    public boolean hasSafeConstructors(ClassType ct) {
        if (isJifClass(ct)) {
            return true;
        }
        if (ct != null) {
            FieldInstance fi = ct.fieldNamed(JIF_SAFE_CONSTRUCTOR_MARKER);
            if (fi != null && fi.flags().isPrivate() && fi.flags().isStatic()) {
                return true;
            }
        }
        return false;
    }


    /**
     * In general, type t can be coerced to a String if t is a String, a
     * primitive, or it has a toString() method.
     */
    @Override
    public boolean canCoerceToString(Type t, Context c) {
        if (this.equalsStrip(t, this.String()) || (t.isPrimitive() && 
                                              !isPrincipal(t) && !isLabel(t))) {
            return true;
        }

        // even if the type t has a toString method, it's not enough, as
        // we do not know how much information is revealed by that string.
        // The programmer must call toString explicitly.
        return false;
    }

    @Override
    public Label join(Label L1, Label L2) {
        if (L1 instanceof NotTaken) {
            return L2.simplify();
        }
        if (L2 instanceof NotTaken) {
            return L1.simplify();
        }
        if (L1.isTop() || L2.isBottom()) {
            return L1.simplify();
        }
        if (L2.isTop() || L1.isBottom()) {
            return L2.simplify();
        }
        
        Set<Label> s = new LinkedHashSet<Label>();
        s.add(L1);
        s.add(L2);
        Position pos = L1.position();
        if (pos == null) pos = L2.position();

        return joinLabel(pos, s).simplify();
    }
    
    @Override
    public Label meet(Label L1, Label L2) {
        if (L1.isTop() || L2.isBottom()) {
            return L2.simplify();
        }
        if (L2.isTop() || L1.isBottom()) {
            return L1.simplify();
        }
        Set<Label> s = new LinkedHashSet<Label>();
        s.add(L1);
        s.add(L2);
        Position pos = L1.position();
        if (pos == null) pos = L2.position();

        return meetLabel(pos, s).simplify();
    }
    

    @Override
    public boolean actsFor(Principal p, Principal q) {
        return emptyLabelEnv.actsFor(p, q);
    }

    @Override
    public boolean leq(Label L1, Label L2) {
        return emptyLabelEnv.leq(L1, L2);
    }

    @Override
    public boolean leq(Policy p1, Policy p2) {
        return emptyLabelEnv.leq(p1, p2);
    }
    @Override
    public ConfPolicy joinConfPolicy(Position pos, Set<ConfPolicy> components) {
        if (components.isEmpty()) {
            return bottomConfPolicy(pos);
        }
        else if (components.size() == 1) {
            return components.iterator().next();
        }
        return (ConfPolicy)new JoinConfPolicy_c(components, this, pos).simplify();
    }
    @Override
    public IntegPolicy joinIntegPolicy(Position pos, Set<IntegPolicy> components) {
        if (components.isEmpty()) {
            return bottomIntegPolicy(pos);
        }
        else if (components.size() == 1) {
            return components.iterator().next();
        }
        return (IntegPolicy)new JoinIntegPolicy_c(components, this, pos).simplify();        
    }
    @Override
    public ConfPolicy meetConfPolicy(Position pos, Set<ConfPolicy> components) {
        if (components.isEmpty()) {
            return topConfPolicy(pos);
        }
        else if (components.size() == 1) {
            return components.iterator().next();
        }
        return (ConfPolicy)new MeetConfPolicy_c(components, this, pos).simplify();        
    }
    @Override
    public IntegPolicy meetIntegPolicy(Position pos, Set<IntegPolicy> components) {
        if (components.isEmpty()) {
            return topIntegPolicy(pos);
        }
        else if (components.size() == 1) {
            return components.iterator().next();
        }
        return (IntegPolicy)new MeetIntegPolicy_c(components, this, pos).simplify();        
    }
    @Override
    public ConfPolicy join(ConfPolicy p1, ConfPolicy p2) {
        if (p1.isTop() || p2.isBottom()) {
            return (ConfPolicy)p1.simplify();
        }
        if (p2.isTop() || p1.isBottom()) {
            return (ConfPolicy)p2.simplify();
        }
        Set<ConfPolicy> s = new HashSet<ConfPolicy>();
        s.add(p1);
        s.add(p2);
        Position pos = p1.position();
        if (pos == null) pos = p2.position();

        return (ConfPolicy)joinConfPolicy(pos, s).simplify();
    }
    @Override
    public IntegPolicy join(IntegPolicy p1, IntegPolicy p2) {
        if (p1.isTop() || p2.isBottom()) {
            return (IntegPolicy)p1.simplify();
        }
        if (p2.isTop() || p1.isBottom()) {
            return (IntegPolicy)p2.simplify();
        }
        Set<IntegPolicy> s = new HashSet<IntegPolicy>();
        s.add(p1);
        s.add(p2);
        Position pos = p1.position();
        if (pos == null) pos = p2.position();

        return (IntegPolicy)joinIntegPolicy(pos, s).simplify();
    }
    @Override
    public ConfPolicy meet(ConfPolicy p1, ConfPolicy p2) {
        if (p1.isTop() || p2.isBottom()) {
            return (ConfPolicy)p2.simplify();
        }
        if (p2.isTop() || p1.isBottom()) {
            return (ConfPolicy)p1.simplify();
        }
        Set<ConfPolicy> s = new HashSet<ConfPolicy>();
        s.add(p1);
        s.add(p2);
        Position pos = p1.position();
        if (pos == null) pos = p2.position();

        return (ConfPolicy)meetConfPolicy(pos, s).simplify();
    }
    @Override
    public IntegPolicy meet(IntegPolicy p1, IntegPolicy p2) {
        if (p1.isTop() || p2.isBottom()) {
            return (IntegPolicy)p2.simplify();
        }
        if (p2.isTop() || p1.isBottom()) {
            return (IntegPolicy)p1.simplify();
        }
        Set<IntegPolicy> s = new HashSet<IntegPolicy>();
        s.add(p1);
        s.add(p2);
        Position pos = p1.position();
        if (pos == null) pos = p2.position();

        return (IntegPolicy)meetIntegPolicy(pos, s).simplify();
    }
    
    @Override
    public ConfPolicy confProjection(Label L) {
        if (L instanceof MeetLabel || L instanceof JoinLabel || L instanceof PairLabel)
            return L.confProjection();
        
        return new ConfProjectionPolicy_c(L, this, L.position());
    }
    @Override
    public IntegPolicy integProjection(Label L) {
        if (L instanceof MeetLabel || L instanceof JoinLabel || L instanceof PairLabel)
            return L.integProjection();

        return new IntegProjectionPolicy_c(L, this, L.position());
    }
    

    @SuppressWarnings("deprecation")
    @Override
    public String translateClass(Resolver c, ClassType t)
    {
        // Fully qualify classes in jif.lang and jif.principal.
        if (t.package_() != null) {
            if (t.package_().equals(createPackage("jif.lang"))
                || t.package_().equals(createPackage("jif.principals"))) {
                return super.translateClass(null, t);
            }
        }

        return super.translateClass(c, t);
    }

    @Override
    public String translatePrimitive(Resolver c, PrimitiveType t) {
        if (isLabel(t)) {
            return LabelClassName();
        } else if (isPrincipal(t)) {
            return PrincipalClassName();
        } else {
            return super.translatePrimitive(c, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ReferenceType> abstractSuperInterfaces(ReferenceType rt) {
        return super.abstractSuperInterfaces(rt);
    }

    @Override
    public boolean isAccessible(MemberInstance mi, ClassType contextClass) {
        return super.isAccessible(mi, contextClass);
    }

    @Override
    public PrimitiveType primitiveForName(String name)
        throws SemanticException {

        if (name.equals("label"))
            return Label();
        if (name.equals("principal"))
            return Principal();
        return super.primitiveForName(name);
    }

    @Override
    public Collection<ClassType> uncheckedExceptions() {
        return Collections.singletonList(Error());
    }

    @Override
    public DefaultSignature defaultSignature() {
        return ds;
    }

    @SuppressWarnings("unchecked")
    @Override
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
                                      Collections.EMPTY_LIST,
                                      Collections.EMPTY_LIST);
    }

    protected LabelTypeCheckUtil ltcu = null;
    @Override
    public LabelTypeCheckUtil labelTypeCheckUtil() {
        if (ltcu == null)
            ltcu = new LabelTypeCheckUtil(this);
        return ltcu;
    }
    
    @Override
    public boolean promoteToFatal(Type t) {
    	return ((JifOptions)extInfo.getOptions()).fatalExceptions
    		&& descendsFrom(t, RuntimeException());
    }

    @Override
    public Label toLabel(Principal p) {
        ConfPolicy toConf =
                topConfPolicy(Position.compilerGenerated(Position.CALLER));
        IntegPolicy toInteg =
                writerPolicy(Position.compilerGenerated(Position.CALLER), p, p);
        return pairLabel(p.position(), toConf, toInteg);
    }
}
