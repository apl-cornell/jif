package jif.types.label;

import java.util.Collection;
import java.util.Set;

import jif.translate.JifToJavaRewriter;
import jif.types.LabelSubstitution;
import jif.types.Param;
import jif.types.hierarchy.LabelEnv;
import polyglot.ast.Expr;
import polyglot.types.*;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;

/** 
 * This class represents the Jif security label.
 */
public interface Label extends Param
{
    /**
     * Is this label equivalent to bottom?
     * <p>
     * For example, a JoinLabel with no components would return true for this 
     * method.
     */
    boolean isBottom();
    
    /**
     * Is this label equivalent to top?
     * <p>
     *  For example, a JoinLabel with two components, one of which is Top, would 
     *  return true for this method.
     */
    boolean isTop(); 
    
    /**
     * Is this label invariant?
     */
    boolean isInvariant();

    /**
     * Is this label covariant?
     */
    boolean isCovariant();

    /** 
     * Returns the join of this label and L. 
     */
    Label join(Label L);
    
    /**
     * Is this label comparable to other labels?
     * <p>
     * For example, an UnknownLabel is not comparable to others, neither is a VarLabel.
     * Most other labels are. 
     */
    boolean isComparable();
    
    String description();
    
    void  setDescription(String d);
    
    Label subst(LocalInstance arg, Label l);
    Label subst(AccessPathRoot r, AccessPath e);

    /**
     * @param labelSubst
     * @return
     * @throws SemanticException
     */
    Label subst(LabelSubstitution labelSubst) throws SemanticException;


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
     * Are the components of this label all disambiguated?
     */
    boolean isDisambiguated();

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
    
    String componentString();    
    String componentString(Set printedLabels);    
    String toString(Set printedLabels);
}
