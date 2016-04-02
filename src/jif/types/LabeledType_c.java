package jif.types;

import jif.types.label.Label;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.NullType;
import polyglot.types.PrimitiveType;
import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.Type_c;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>LabeledType</code> interface. 
 */
public class LabeledType_c extends Type_c implements LabeledType {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Type typePart;
    protected Label labelPart;

    public LabeledType_c(JifTypeSystem ts, Position pos, Type typePart,
            Label labelPart) {
        super(ts, pos);
        this.typePart = typePart;
        this.labelPart = labelPart;
        if (typePart == null || labelPart == null) {
            throw new InternalCompilerError(
                    "Null args: " + typePart + " and " + labelPart);
        }
    }

    @Override
    public boolean isCanonical() {
        return typePart.isCanonical() && labelPart.isCanonical();
    }

    @Override
    public Type typePart() {
        return this.typePart;
    }

    @Override
    public LabeledType typePart(Type typePart) {
        LabeledType_c n = (LabeledType_c) copy();
        n.typePart = typePart;
        return n;
    }

    @Override
    public Label labelPart() {
        return this.labelPart;
    }

    @Override
    public LabeledType labelPart(Label labelPart) {
        LabeledType_c n = (LabeledType_c) copy();
        n.labelPart = labelPart;
        return n;
    }

    @Override
    public String toString() {
        return typePart.toString() + labelPart.toString();
    }

    @Override
    public String translate(Resolver c) {
        return typePart.translate(c);
    }

    @Override
    public boolean equalsImpl(TypeObject t) {
        // only return pointer equals. This method may occasionally be called, due to
        // the existence of the JifTypeSystem.equalsNoStrip method.
        return this == t;
        // throw new InternalCompilerError(this + ".equalsImpl(" + t + ") called");
    }

    @Override
    public ClassType toClass() {
        return typePart.toClass();
    }

    @Override
    public NullType toNull() {
        return typePart.toNull();
    }

    @Override
    public ReferenceType toReference() {
        return typePart.toReference();
    }

    @Override
    public PrimitiveType toPrimitive() {
        return typePart.toPrimitive();
    }

    @Override
    public ArrayType toArray() {
        return typePart.toArray();
    }

    @Override
    public boolean isPrimitive() {
        return typePart.isPrimitive();
    }

    @Override
    public boolean isVoid() {
        return typePart.isVoid();
    }

    @Override
    public boolean isBoolean() {
        return typePart.isBoolean();
    }

    @Override
    public boolean isChar() {
        return typePart.isChar();
    }

    @Override
    public boolean isByte() {
        return typePart.isByte();
    }

    @Override
    public boolean isShort() {
        return typePart.isShort();
    }

    @Override
    public boolean isInt() {
        return typePart.isInt();
    }

    @Override
    public boolean isLong() {
        return typePart.isLong();
    }

    @Override
    public boolean isFloat() {
        return typePart.isFloat();
    }

    @Override
    public boolean isDouble() {
        return typePart.isDouble();
    }

    @Override
    public boolean isIntOrLess() {
        return typePart.isIntOrLess();
    }

    @Override
    public boolean isLongOrLess() {
        return typePart.isLongOrLess();
    }

    @Override
    public boolean isNumeric() {
        return typePart.isNumeric();
    }

    @Override
    public boolean isReference() {
        return typePart.isReference();
    }

    @Override
    public boolean isNull() {
        return typePart.isNull();
    }

    @Override
    public boolean isArray() {
        return typePart.isArray();
    }

    @Override
    public boolean isClass() {
        return typePart.isClass();
    }

    @Override
    public boolean isThrowable() {
        return typePart.isThrowable();
    }

    @Override
    public boolean isUncheckedException() {
        return typePart.isUncheckedException();
    }
}
