package jif.ast;

import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.*;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.TypeNode_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>LabeledTypeNode</code> interface. 
 */
public class LabeledTypeNode_c extends TypeNode_c implements LabeledTypeNode, Ambiguous
{
    protected TypeNode typePart;
    protected LabelNode labelPart;

    public LabeledTypeNode_c(Position pos, TypeNode typePart, LabelNode labelPart) {
	super(pos);
	this.typePart = typePart;
	this.labelPart = labelPart;
    }

    public TypeNode typePart() {
	return this.typePart;
    }

    public LabeledTypeNode typePart(TypeNode typePart) {
	LabeledTypeNode_c n = (LabeledTypeNode_c) copy();
	n.typePart = typePart;
	return n;
    }

    public LabelNode labelPart() {
	return this.labelPart;
    }

    public LabeledTypeNode labelPart(LabelNode labelPart) {
	LabeledTypeNode_c n = (LabeledTypeNode_c) copy();
	n.labelPart = labelPart;
	return n;
    }

    protected LabeledTypeNode_c reconstruct(TypeNode typePart, LabelNode labelPart) {
	if (typePart != this.typePart || labelPart != this.labelPart) {
	    LabeledTypeNode_c n = (LabeledTypeNode_c) copy();
	    n.typePart = typePart;
	    n.labelPart = labelPart;
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	TypeNode typePart = (TypeNode) visitChild(this.typePart, v);
	LabelNode labelPart = (LabelNode) visitChild(this.labelPart, v);
	return reconstruct(typePart, labelPart);
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
          if (type == null) {
              return type(typePart.type());
          }

          return this;
    }

    public boolean isDisambiguated() {
        return typePart.isDisambiguated() && labelPart.isDisambiguated();
    }

    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        if (!this.typePart.isDisambiguated() || !this.labelPart.isDisambiguated()) {
            // the children haven't been disambiguated yet
            return this;
        }

        JifTypeSystem jts = (JifTypeSystem) sc.typeSystem();

	Type t = typePart.type();
	Label L = labelPart.label();
    
	if (t.isVoid()) {
	    throw new SemanticException("The void type cannot be labeled.",
		position());
	}

	if (t instanceof JifClassType) {
	    JifClassType ct = (JifClassType) t;

	    if (ct.isInvariant()) {
		t = ct.setInvariantThis(L);
	    }
	}

	return sc.nodeFactory().CanonicalTypeNode(position(), 
	    jts.labeledType(position(), t, L));
    }

    public String toString() {
	return typePart.toString() + labelPart.toString();
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
	throw new InternalCompilerError(position(),
	    "Cannot type check ambiguous node " + this + ".");
    } 

    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
	throw new InternalCompilerError(position(),
	    "Cannot exception check ambiguous node " + this + ".");
    } 

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(typePart, w, tr);
        w.write("{");
        print(labelPart, w, tr);
        w.write("}");
    }

    public void translate(CodeWriter w, Translator tr) {
	throw new InternalCompilerError(position(),
	    "Cannot translate ambiguous node " + this + ".");
    }
}
