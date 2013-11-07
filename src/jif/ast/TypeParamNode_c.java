package jif.ast;

import jif.types.Param;
import jif.types.TypeParam;
import jif.types.TypeParam_c;
import polyglot.ast.TypeNode;
import polyglot.ast.TypeNode_c;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.PrettyPrinter;

public class TypeParamNode_c extends TypeNode_c implements TypeParamNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    TypeParam typeParam;

    public TypeParamNode_c(Position pos, TypeNode tn) {
        super(pos);
        // Should this type TypeParam_c be constructed here or by the TypeFactory in the parser?
        this.typeParam = new TypeParam_c(tn);
        super.type(tn.type());
    }

    @Override
    public Param parameter() {
        return typeParam;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(typeParam.toString());
    }
}
