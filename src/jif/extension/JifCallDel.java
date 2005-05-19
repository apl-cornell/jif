package jif.extension;

import java.util.*;

import jif.ast.JifInstantiator;
import jif.types.*;
import jif.types.label.VarLabel;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Call</code> node. 
 * 
 *  @see polyglot.ext.jl.ast.Call_c
 */
public class JifCallDel extends JifJL_c
{
    public JifCallDel() { }

    /**
     * This flag records whether the target of a method call is never
     * null. This flag is by default false, but may be set to true by the
     * dataflow analysis performed by jif.visit.NotNullChecker
     */
    private boolean isTargetNeverNull = false;
    
    public void setTargetIsNeverNull() {
        isTargetNeverNull = true;
    }

    public boolean targetIsNeverNull() {
        Receiver r = ((Call)node()).target();
        return (r instanceof Special 
                || isTargetNeverNull 
                || r instanceof CanonicalTypeNode);
    }

    /** 
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in Call_c in that it does not
     * throw a null pointer exception if the receiver is guaranteed to be 
     * non-null
     */
    public List throwTypes(TypeSystem ts) {
        MethodInstance mi = ((Call)node()).methodInstance();
        if (mi == null) {
            throw new InternalCompilerError(
                node().position(),
                "Null method instance after type " + "check.");
        }

        List l = new LinkedList();

        l.addAll(mi.throwTypes());

        // We may throw a null pointer exception except when the target
        // is "this" or "super", or the receiver is guaranteed to be non-null
        if (!targetIsNeverNull()) {
            l.add(ts.NullPointerException());
        }

        // if the method instance is static, and the target type is a 
        // parameterized class, we may need to evaluate some parameters
        // at runtime, and need to account for them here.
        if (mi.flags().isStatic()) {
            if (mi.container() instanceof JifClassType) {
                l.addAll(LabelTypeCheckUtil.throwTypes((JifClassType)mi.container(), 
                                                       (JifTypeSystem)ts));
            }            
        }
        return l;
    }
    
    protected VarLabel receiverVarLabel;
    protected List argVarLabels; // list of var labels for the actual args
    protected List paramVarLabels; // list of var labels for the actual parameters of the type.

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        Call c = (Call)super.typeCheck(tc);
        
        // we need to instantiate the return type correctly during type checking,
        // to allow, for example, the following code to correctly type checked
        //      C[{}] x = b?null:foo(new label{});
        //  where
        //    C[lbl] foo(label lbl);
        //
        // The problem is that the type of the ternary conditional operator
        // ends up being the type of the call, so the type of the call
        // must be correct after the type checking pass.
        //
        // We use var labels, which are later bound to the correct labels 
        // during label checking.
        JifMethodInstance mi = (JifMethodInstance)c.methodInstance();
        JifContext A = (JifContext)tc.context();
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        
        JifCallDel del = (JifCallDel)c.del();
        del.receiverVarLabel = null;
        Expr receiverExpr = null;
        if (c.target() instanceof Expr) {
            receiverExpr = (Expr)c.target();
            del.receiverVarLabel = ts.freshLabelVariable(c.position(), 
                                                      "receiver",
                                                      "label of receiver of call " + c.toString());
        }
        del.argVarLabels = new ArrayList(c.arguments().size());
        for (int i = 0; i < c.arguments().size(); i++) {
            Expr arg = (Expr)c.arguments().get(i);
            VarLabel argLbl =  ts.freshLabelVariable(arg.position(), 
                                                     "arg"+(i+1)+"label",
                                                     "label of arg " + (i+1) + " of call " + c.toString());
            del.argVarLabels.add(argLbl);
        }

        Type t = mi.returnType();
        if (t instanceof JifSubstType) {            
            JifSubstType jst = (JifSubstType)t;
            del.paramVarLabels = new ArrayList(jst.subst().substitutions().size());
            
            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Param param = (Param)e.getValue();
                VarLabel paramLbl =  ts.freshLabelVariable(param.position(), 
                                                         "param_"+param+"_label",
                                                         "label of param " + param + " of call " + c.toString());
                del.paramVarLabels.add(paramLbl);
            }
        }
        else {
            del.paramVarLabels = Collections.EMPTY_LIST;
        }
        
        Type retType =  JifInstantiator.instantiate(t, A, 
                                                    receiverExpr, 
                                                    mi.container(), 
                                                    del.receiverVarLabel,
                                                    CallHelper.getArgLabelsFromFormalTypes(mi.formalTypes(), ts),
                                                    del.argVarLabels,
                                                    c.arguments(),
                                                    del.paramVarLabels);

        c = (Call)c.type(retType);                
        return c;
    }
    
}
