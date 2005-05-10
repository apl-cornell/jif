package jif.extension;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
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
                throw new SemanticException("The type of a dynamic principal must be \"principal\"", principal.position());
            }
        }        
    }
    
    public static void typeCheckLabel(TypeChecker tc, Label Lbl) throws SemanticException {
        for (Iterator comps = Lbl.components().iterator(); comps.hasNext(); ) {
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
                    throw new SemanticException("The type of a dynamic label must be \"label\"", dl.position());
                }
            }        
            else if (l instanceof PolicyLabel) {
                PolicyLabel pl = (PolicyLabel)l;
                typeCheckPrincipal(tc, pl.owner());
                for (Iterator i = pl.readers().iterator(); i.hasNext(); ) {
                    Principal r = (Principal)i.next();
                    typeCheckPrincipal(tc, r);                
                }
            }
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

    public static PathMap labelCheckType(Type t, LabelChecker lc, Position pos) throws SemanticException {
        JifContext A = lc.context();
        JifTypeSystem ts = lc.typeSystem();
        PathMap X = ts.pathMap().N(A.pc());            

        List Xparams = labelCheckTypeParams(t, lc, pos);

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
    public static List labelCheckTypeParams(Type t, LabelChecker lc, Position pos) throws SemanticException {
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
                            return "A label used in a type examined at runtime must be representable at runtime. Arg labels are not represented at runtime.";
                        }
                    });
                    
                    A.setPc(X.N());    
                    PathMap Xj = L.labelCheck(A);
                    Xparams.add(Xj);
                    X = X.join(Xj);
                    A = (JifContext)A.pop();
                }
                else if (arg instanceof Principal) {
                    Principal p = (Principal)arg;
                    A = (JifContext)A.pushBlock();
                    if (!p.isRuntimeRepresentable()) {
                        throw new SemanticException(
                                                    "A principal used in a type examined at runtime must be representable at runtime.",
                                                    pos);
                    }
                    
                    
                    A.setPc(X.N());            
                    PathMap Xj = p.labelCheck(A);
                    Xparams.add(Xj);
                    X = X.join(Xj);
                    A = (JifContext)A.pop();
                }
                else {
                    throw new InternalCompilerError(
                                                    "Unexpected type for entry: "
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
     * @return
     */
    public static List throwTypes(JifClassType type, JifTypeSystem ts) {
        Type t = ts.unlabel(type);
        if (t instanceof JifSubstType) {            
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
}
