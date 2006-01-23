package jif.extension;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.*;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

/**
 * Contains some common utility code to type check dynamic labels and principals
 */
public class LabelTypeCheckUtil {
    
    /**
     * Check the type of any access path contained in a dynamic principal. All such access paths should have type
     * Principal. 
     * @param tc
     * @param principal
     * @throws SemanticException
     */
    public static void typeCheckPrincipal(TypeChecker tc, Principal principal) throws SemanticException {
        if (principal instanceof DynamicPrincipal) {
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
            DynamicPrincipal dp = (DynamicPrincipal)principal;
            
            // Make sure that the access path is set correctly
            // check also that all field accesses are final, and that
            // the type of the expression is principal
            AccessPath path = dp.path();
            try {
                path.verify((JifContext)tc.context());                
            }
            catch (SemanticException e) {
                throw new SemanticException(e.getMessage(), principal.position());
            }
            
            if (!ts.isImplicitCastValid(dp.path().type(), ts.Principal())) {
                throw new SemanticDetailedException("The type of a dynamic principal must be \"principal\".", 
                                            "The type of a dynamic principal must be " +
                                            "\"principal\". The type of the expression " + 
                                            dp.path().exprString() + " is " + 
                                            dp.path().type() + ".",
                                            principal.position());
            }
        } 
        if (principal instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal p = (ConjunctivePrincipal)principal;
            for (Iterator iter = p.conjuncts().iterator(); iter.hasNext(); ) {
                Principal q = (Principal)iter.next();
                typeCheckPrincipal(tc, q);
            }
        }
        if (principal instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal p = (DisjunctivePrincipal)principal;
            for (Iterator iter = p.disjuncts().iterator(); iter.hasNext(); ) {
                Principal q = (Principal)iter.next();
                typeCheckPrincipal(tc, q);
            }
        }
    }
    
    /**
     * Check that all access paths occurring in label Lbl have the appropriate type.
     * @param tc
     * @param Lbl
     * @throws SemanticException
     */
    public static void typeCheckLabel(TypeChecker tc, Label Lbl) throws SemanticException {
        Collection components = labelComponents(Lbl);
        for (Iterator comps = components.iterator(); comps.hasNext(); ) {
            Label l = (Label)comps.next();
            if (l instanceof DynamicLabel) {
                JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
                DynamicLabel dl = (DynamicLabel)l;
                
                // Make sure that the access path is set correctly
                // check also that all field accesses are final, and that
                // the type of the expression is label
                AccessPath path = dl.path();
                try {
                    path.verify((JifContext)tc.context());                
                }
                catch (SemanticException e) {
                    throw new SemanticException(e.getMessage(), dl.position());
                }
                
                if (!ts.isLabel(dl.path().type())) {
                    throw new SemanticDetailedException("The type of a dynamic label must be \"label\".",
                                     "The type of a dynamic label must be " +
                                     "\"label\". The type of the expression " + 
                                     dl.path().exprString() + " is " + 
                                     dl.path().type() + ".",
                                                        dl.position());
                }
            }        
            else if (l instanceof PairLabel) {
               PairLabel pl = (PairLabel)l;
               typeCheckPolicy(tc, pl.confPolicy());
               typeCheckPolicy(tc, pl.integPolicy());
            }
        }
        
    }

    public static Collection labelComponents(Label L) {
        if (L instanceof JoinLabel) {
            return ((JoinLabel)L).joinComponents();
        }
        else if (L instanceof MeetLabel) {
            return ((MeetLabel)L).meetComponents();
        }
        else {
            return Collections.singleton(L);
        }        
    }
    
    public static void typeCheckPolicy(TypeChecker tc, Policy p) throws SemanticException {
        if (p instanceof JoinPolicy_c) {
            JoinPolicy_c jp = (JoinPolicy_c)p;
            for (Iterator iter = jp.joinComponents().iterator(); iter.hasNext();) {
                Policy pol = (Policy)iter.next();
                typeCheckPolicy(tc, pol);
            }
        }
        else if (p instanceof MeetPolicy_c) {
            MeetPolicy_c mp = (MeetPolicy_c)p;
            for (Iterator iter = mp.meetComponents().iterator(); iter.hasNext();) {
                Policy pol = (Policy)iter.next();
                typeCheckPolicy(tc, pol);
            }
        } 
        else if (p instanceof ReaderPolicy) {
            ReaderPolicy pol = (ReaderPolicy)p;
            typeCheckPrincipal(tc, pol.owner());
            typeCheckPrincipal(tc, pol.reader());
        }
        else if (p instanceof WriterPolicy) {
            WriterPolicy pol = (WriterPolicy)p;
            typeCheckPrincipal(tc, pol.owner());
            typeCheckPrincipal(tc, pol.writer());
        }        
        else {
            throw new InternalCompilerError("Unexpected policy " + p);
        }
    }
    
    public static void typeCheckType(TypeChecker tc, Type t) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();

        t = ts.unlabel(t);
        
        if (t instanceof JifSubstType) {            
            JifSubstType jst = (JifSubstType)t;
            
            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                if (arg instanceof Label) {
                    Label L = (Label)arg;
                    typeCheckLabel(tc, L);
                }
                else if (arg instanceof Principal) {
                    Principal p = (Principal)arg;
                    typeCheckPrincipal(tc, p);
                }
                else {
                    throw new InternalCompilerError(
                                                    "Unexpected type for entry: "
                                                    + arg.getClass().getName());
                }
            }            
        }
    }

    public static PathMap labelCheckType(Type t, LabelChecker lc, List throwTypes, Position pos) throws SemanticException {
        JifContext A = lc.context();
        JifTypeSystem ts = lc.typeSystem();
        PathMap X = ts.pathMap().N(A.pc());            

        List Xparams = labelCheckTypeParams(t, lc, throwTypes, pos);

        for (Iterator iter = Xparams.iterator(); iter.hasNext(); ) {
            PathMap Xj = (PathMap)iter.next();
            X = X.join(Xj);
        }        
        return X;                
    }
    /**
     * 
     * @param t
     * @param lc
     * @return List of <code>PathMap</code>s, one for each parameter of the subst type.
     * @throws SemanticException
     */
    public static List labelCheckTypeParams(Type t, LabelChecker lc, List throwTypes, Position pos) throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        t = ts.unlabel(t);
        List Xparams;
        
        if (t instanceof JifSubstType) {            
            JifContext A = lc.context();
            PathMap X = ts.pathMap().N(A.pc());            
            JifSubstType jst = (JifSubstType)t;
            Xparams = new ArrayList(jst.subst().substitutions().size());
            
            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                if (arg instanceof Label) {
                    Label L = (Label)arg;
                    A = (JifContext)A.pushBlock();
                    
                    if (ts.isParamsRuntimeRep(t)) {
	                    // make sure the label is runtime representable
	                    lc.constrain(new LabelConstraint(new NamedLabel("label_in_type", 
	                                                                    L), 
	                                                                    LabelConstraint.LEQ, 
	                                                                    new NamedLabel("RUNTIME_REPRESENTABLE", 
	                                                                                   ts.runtimeLabel()),
	                                                                                   A.labelEnv(),
	                                                                                   pos) {
	                        public String msg() {
	                            return "A label used in a type examined at runtime must be representable at runtime.";
	                        }
	                        public String detailMsg() {
	                            return "If a type is used in an instanceof, " +
	                            "cast, constructor call, or static method call, " +
	                            "all parameters of the type must be runtime " +
	                            "representable. Arg labels are not represented at runtime.";
	                        }
	                    });
                    }
                    
                    A.setPc(X.N());    
                    PathMap Xj = L.labelCheck(A, lc);
                    throwTypes.removeAll(L.throwTypes(ts));
                    Xparams.add(Xj);
                    X = X.join(Xj);
                    A = (JifContext)A.pop();
                }
                else if (arg instanceof Principal) {
                    Principal p = (Principal)arg;
                    A = (JifContext)A.pushBlock();
                    if (ts.isParamsRuntimeRep(t) && !p.isRuntimeRepresentable()) {
                        throw new SemanticDetailedException("A principal used in a " +
                            "type examined at runtime must be " +
                            "representable at runtime.",
                            "If a type is used in an instanceof, " +
                            "cast, constructor call, or static method call, " +
                            "all parameters of the type must be runtime " +
                            "representable. The principal " + p + " is not " +
                            "represented at runtime.",
                                                    pos);
                    }
                    
                    
                    A.setPc(X.N());            
                    PathMap Xj = p.labelCheck(A, lc);
                    throwTypes.removeAll(p.throwTypes(ts));
                    Xparams.add(Xj);
                    X = X.join(Xj);
                    A = (JifContext)A.pop();
                }
                else {
                    throw new InternalCompilerError("Unexpected type for entry: "
                                                    + arg.getClass().getName());
                }
            }            
        }
        else {
            Xparams = Collections.EMPTY_LIST;
        }
        return Xparams;
    }

    /**
     * Return the types that may be thrown by a runtime evalution
     * of the type <code>type</code>.
     * 
     * @param type
     * @param ts
     * @return the types that may be thrown by a runtime evalution
     * of the type <code>type</code>.
     */
    public static List throwTypes(JifClassType type, JifTypeSystem ts) {
        Type t = ts.unlabel(type);
        if (t instanceof JifSubstType && ts.isParamsRuntimeRep(t)) {            
            JifSubstType jst = (JifSubstType)t;
            List exc = new ArrayList();
            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                if (arg instanceof Label) {
                    exc.addAll(((Label)arg).throwTypes(ts));
                }
                else if (arg instanceof Principal) {
                    exc.addAll(((Principal)arg).throwTypes(ts));
                }
                else {
                    throw new InternalCompilerError("Unexpected type for entry: "
                                                    + arg.getClass().getName());
                }
            }            
            return exc;
        }
        else {
            return Collections.EMPTY_LIST;
        }
    }
    /**
     * Returns a set of local instances that are used in the type.
     */
    public static Set localInstancesUsed(JifClassType type, JifTypeSystem ts) {
        Type t = ts.unlabel(type);
        if (t instanceof JifSubstType) {            
            JifSubstType jst = (JifSubstType)t;
            Set lis = new LinkedHashSet();
            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                AccessPath p = null;
                if (arg instanceof DynamicLabel) {
                    p = ((DynamicLabel)arg).path();
                }
                else if (arg instanceof DynamicPrincipal) {
                    p = ((DynamicPrincipal)arg).path();
                }
                while (p != null && p instanceof AccessPathField) {
                    p = ((AccessPathField)p).path();
                }
                if (p instanceof AccessPathLocal) {
                    lis.add(((AccessPathLocal)p).localInstance());
                }
            }            
            return lis;
        }
        else {
            return Collections.EMPTY_SET;
        }        
    }
    
}
