package jif.types.label;

import java.util.Collection;
import java.util.Set;

import jif.translate.JifToJavaRewriter;
import jif.types.hierarchy.LabelEnv;
import polyglot.ast.Expr;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;

/** 
 * TODO: doco
 * provides label moethods for the type system, translators, etc. to manipulate.
 */
public interface LabelImpl extends Label
{
    
    /**
     * Are the components of this label enumerable?
     * <p>
     * For example, Singletons are enumerable, JoinLabels are enumerable, RuntimeLabel
     * (the label of all runtime representable components) is not enumerable. 
     * 
     * NOTE: The components of a label are not neccessarily stuck together
     * with a join operation. For example, the MeetLabel uses the meet 
     * operation between its components.
     */
    boolean isEnumerable();
    
    /**
     * Retrieve the collection of components. This method should only
     * be called if isEnumerable returns true.
     * 
     * This collection should not be modified.
     */
    Collection components();
    
    /**
     * Does this label represent only a single label?
     * <p>
     * For example, a JoinLabel with more than one component returns false, a MeetLabel
     * with more than one component returns false, most other Labels return 
     * true.
     */
    boolean isSingleton();
    
    /**
     * Retrieve the singleton component that this label represents. Should only
     * be called is isSingleton returns true.
     */
    Label singletonComponent();

    Label simplify();
    
    String componentString();
    
    /**
     * Does the label contain any variables, that is, labels of type 
     * VarLabel?
     */
    boolean hasVariables();
    
    /**
     * The set of variables that this contains, i.e., labels of type 
     * VarLabel?
     */
    Set variables();

    /** 
     * Implementation of leq, should only be called by 
     * JifTypeSystem 
     * 
     * @param L the label to determine if this label is leq to. This label always satisfies !this.equals(L) 
     * @param H the label environment (including principal hierarchy). 
     * Will always be non-null.
     */
    boolean leq_(Label L, LabelEnv H);

    boolean isRuntimeRepresentable();
    
    void translate(Resolver c, CodeWriter w);
    Expr toJava(JifToJavaRewriter rw) throws SemanticException;
}
