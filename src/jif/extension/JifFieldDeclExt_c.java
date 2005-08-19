package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import jif.visit.LabelSubstitutionVisitor;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;

/** The Jif extension of the <code>FieldDecl</code> node. 
 * 
 *  @see polyglot.ast.FieldDecl
 */
public class JifFieldDeclExt_c extends Jif_c implements JifFieldDeclExt
{
    public JifFieldDeclExt_c(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();
    
    /** Extracts the declared label of this field. 
     */
    public void labelCheckField(LabelChecker lc, JifClassType ct) throws SemanticException {
	JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
	FieldDecl decl = (FieldDecl) node();
	JifFieldInstance fi = (JifFieldInstance) decl.fieldInstance();
	Label L = fi.label();
	Type t = decl.declType();

	
        if (!ts.isLabeled(t)) {
            // field should always be labeled.
            // See JifFieldDeclDel#disambiguate(AmbiguityRemover)
            throw new InternalCompilerError("Unexpectedly unlabeled field", node().position());
        }
        
	Label declaredLabel = ts.labelOfType(t);

        // error messages for equality constraints aren't displayed, so no
        // need top define error messages.	
        lc.constrain(new LabelConstraint(new NamedLabel("field_label", 
                                                        "inferred label of field " + fi.name(), 
                                                        L), 
                                         LabelConstraint.EQUAL, 
                                         new NamedLabel("declared label of field " + fi.name(), 
                                                        declaredLabel),
                                         A.labelEnv(),
                                         decl.position()));
        

    }

    /** Label check field initializers. 
     * 
     *  the PC of field initializer is the bottom label, because the initializer
     *  is always executed after invoking super constructor and before invoking
     *  the constructor of this class. (like the single path rule)
     */
    public Node labelCheck(LabelChecker lc) throws SemanticException {
	FieldDecl decl = (FieldDecl) node();

        final JifFieldInstance fi = (JifFieldInstance) decl.fieldInstance();
	JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
	A = (JifContext) decl.enterScope(A, null);
	
	//if [final] then invariant(type_part(Tf)) else invariant(type_part(Tf)) and invariant(label_part(Tf))
        {        
            Type fieldType = fi.type();
            LabelSubstitutionVisitor lsv = new InvarianceLabelVisitor(decl.position());

            // use a LabelSubstitutionVisitor to check the type of the field,
            // and make sure that it contains no convariant components.
            // We use the Visitor to ensure that the entire type is traversed,
            // including e.g. actual parameters to polymorphic types, labels
            // of array elements, etc.

            if (decl.flags().isFinal()) {
                lsv.rewriteType(ts.unlabel(fieldType));
            }
            else {
                lsv.rewriteType(fieldType);
            }
        }
    
        // Make sure that static fields do not contain either parameters or 
        // the "this" label
        if (decl.flags().isStatic()) {
            // use a LabelSubstitutionVisitor to check the type of the field,
            // and make sure that it contains no parameters or the "this" label.
            // We use the Visitor to ensure that the entire type is traversed,
            // including e.g. actual parameters to polymorphic types, labels
            // of array elements, etc.
            LabelSubstitutionVisitor lsv = 
                new LabelSubstitutionVisitor(new StaticFieldLabelChecker(decl.position()), true);
            lsv.rewriteType(fi.type());
           
        }

	// There is no PC label at field nodes.
	Label L = fi.label();
	Type t = decl.declType();

        if (!ts.isLabeled(t)) {
            // field should always be labeled.
            // See JifFieldDeclDel#disambiguate(AmbiguityRemover)
            throw new InternalCompilerError("Unexpectedly unlabeled field", node().position());
        }

        Label declaredLabel = ts.labelOfType(t);

        // error messages for equality constraints aren't displayed, so no
        // need top define error messages.  
        lc.constrain(new LabelConstraint(new NamedLabel("field_label", 
                                                        "inferred label of field " + fi.name(), 
                                                        L), 
                                         LabelConstraint.EQUAL, 
                                         new NamedLabel("PC", 
                                                        "Information revealed by program counter being at this program point", 
                                                        A.pc()).
                                             join("declared label of field " + fi.name(), declaredLabel), 
                                         A.labelEnv(),
                                         decl.position()));

	PathMap Xd;

	Expr init = null;

	if (decl.init() != null) {
	    A = (JifContext) A.pushBlock();
	    A.setEntryPC(ts.topLabel());
	    init = (Expr) lc.context(A).labelCheck(decl.init());

            if (init instanceof ArrayInit) {
                ((JifArrayInitExt)(init.ext())).labelCheckElements(lc, decl.type().type()); 
            }

	    PathMap Xe = X(init);
            lc.constrain(new LabelConstraint(new NamedLabel("init.nv", 
                                                            "label of successful evaluation of initializing expression", 
                                                            Xe.NV()), 
                                             LabelConstraint.LEQ, 
                                             new NamedLabel("label of field " + fi.name(), L),
                                             A.labelEnv(),
                                             init.position()) {
                     public String msg() {
                         return "Label of field initializer not less " + 
                                "restrictive than the label for field " + 
                                fi.name();
                     }
                     public String detailMsg() { 
                         return "More information is revealed by the successful " +
                                "evaluation of the intializing expression " +
                                "than is allowed to flow to " +
                                "the field " + fi.name() + ".";
                     }
                     public String technicalMsg() {
                         return "Invalid assignment: NV of initializer is " +
                                "more restrictive than the declared label " +
                                "of field " + fi.name() + ".";
                     }                     
             }
             );

	    Xd = Xe;

	    // Must check that the expression type is a subtype of the
	    // declared type.  Most of this is done in typeCheck, but if
	    // they are instantitation types, we must add constraints for
	    // the labels.
	    subtypeChecker.addSubtypeConstraints(lc, init.position(),
		                                 t, init.type());
	    A = (JifContext) A.pop();
	}
	else {
	    // There is no PC label at field nodes.
	    Xd = ts.pathMap();
	}

	PathMap X = ts.pathMap();
	X = X.N(ts.notTaken()).join(Xd);

	decl = (FieldDecl) X(decl.init(init), X);

	return decl;
    }

    /**
     * Checker to ensure that labels of static fields do not use
     * the This label, or any parameters 
     */    
    protected static class StaticFieldLabelChecker extends LabelSubstitution {
        private Position declPosition;

        StaticFieldLabelChecker(Position declPosition) {
            this.declPosition = declPosition;
        }
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof ThisLabel) {
                throw new SemanticException("The label of a static field " +
                        "cannot use the \"this\" label.", 
                        declPosition);
            }
            if (L instanceof ParamLabel || L instanceof CovariantParamLabel) {
                throw new SemanticException("The label of a static field " +
                        "cannot use the label parameter " + 
                        L.componentString(), 
                        declPosition);
            }
            return L;
        }

        public Principal substPrincipal(Principal p) throws SemanticException {
            if (p instanceof ParamPrincipal) {
                throw new SemanticException("The label of a static field " +
                        "cannot use the principal parameter " + p.toString(), 
                        declPosition);
            }
            return p;
        }
        
    }
    
    /**
     * Visitor to ensure that labels do not use
     * covariant labels in the wrong places 
     */    
    protected static class InvarianceLabelVisitor extends LabelSubstitutionVisitor {
        /* 
         * Don't check subst types, as the subtype checker will take care of those.
         */
        protected boolean recurseIntoSubstType(JifSubstType type) {
            return false;
        }
        public InvarianceLabelVisitor(Position pos) {
            super(new InvarianceLabelChecker(pos), true);
        }
        
    }
    

    /**
     * Checker to ensure that labels do not use
     * covariant labels in the wrong places
     */    
    protected static class InvarianceLabelChecker extends LabelSubstitution {
        private Position declPosition;

        InvarianceLabelChecker(Position declPosition) {
            this.declPosition = declPosition;
        }
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof ThisLabel) {
                throw new SemanticDetailedException("The label of a non-final field, " +
                        "or a mutable location within a final field can not " +
                        "contain the label \"this\".",
                                            "The label of a non-final field, " +
                        "or a mutable location within a final field (such as " +
                        "the label of elements of an array) can not " +
                        "contain the label \"this\". Otherwise, sensitive " +
                        "information could be written into the location " +
                        "through a sensitive reference to the object, and " +
                        "converted to non-sensitive information by reading " +
                        "the value through a non-sensitive reference.",
                                            declPosition);            
            }
            if (L.isCovariant()) {
                throw new SemanticDetailedException("The label of a non-final field, " +
                    "or a mutable location within a final field can not " +
                    "contain the covariant component " + L,
                                        "The label of a non-final field, " +
                    "or a mutable location within a final field (such as " +
                    "the label of elements of an array) can not " +
                    "contain the covariant component " + L + ". " +
                    "Otherwise, sensitive " +
                    "information could be written into the location " +
                    "through a reference to the object with a sensitive type, " +
                    "and converted to non-sensitive information by reading " +
                    "the value through a reference with a less sensitive type.",
                            declPosition);
            }
            return L;
        }

        /**
         * We do not want to check the labelOf components of fields.
         */
        public boolean recurseIntoLabelOf() {
            return false;
        }

    }
}
