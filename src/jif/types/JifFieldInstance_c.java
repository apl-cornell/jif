package jif.types;

import jif.types.label.Label;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifFieldInstance</code> interface.
 */
public class JifFieldInstance_c extends FieldInstance_c
                               implements JifFieldInstance
{
    protected Label label;
    protected boolean hasInitializer;
    
    public JifFieldInstance_c(JifTypeSystem ts, Position pos,
	ReferenceType container, Flags flags,
	Type type, String name) {

	super(ts, pos, container, flags, type, name);
    }

    @Override
    public void subst(VarMap bounds) {
        this.setLabel(bounds.applyTo(label));
        this.setType(bounds.applyTo(type));
    }

    @Override
    public Label label() {
        return label;
    }

    @Override
    public void setLabel(Label label) {
        this.label = label;
    }

    @Override
    public boolean hasInitializer() {
        return hasInitializer;
    }

    @Override
    public void setHasInitializer(boolean hasInitializer) {
        this.hasInitializer = hasInitializer;        
    }

    private FieldInstance findOrigFieldInstance() {
        if (this.container() instanceof JifSubstType) {
            JifSubstType jst = (JifSubstType)this.container();
            if (jst.base() instanceof ParsedClassType) {
                return ((ParsedClassType) jst.base()).fieldNamed(this.name());
            }
            else {
                throw new InternalCompilerError("Unexpected base type");
            }
        }
        return this;        
    }
    @Override
    public boolean isConstant() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            return orig.isConstant();        
        }
        return super.isConstant();
    }
    @Override
    public Object constantValue() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            return orig.constantValue();        
        }
        return super.constantValue();
    }
    @Override
    public boolean constantValueSet() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            return orig.constantValueSet();        
        }
        return super.constantValueSet();
    }
    @Override
    public FieldInstance constantValue(Object constantValue) {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            throw new InternalCompilerError("Cant modify constant value on a copy");            
        }
        return super.constantValue(constantValue);
    }
    @Override
    public FieldInstance notConstant() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            throw new InternalCompilerError("Cant modify constant value on a copy");            
        }
        return super.notConstant();
    }
    @Override
    public void setConstantValue(Object constantValue) {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            throw new InternalCompilerError("Cant modify constant value on a copy");            
        }
        super.setConstantValue(constantValue);
    }
    @Override
    public String toString() {
        return super.toString() + " label = " + label;
    }
    
}
