package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import polyglot.visit.*;
import java.util.*;

public class DynamicLabelToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException {
        DynamicLabel L = (DynamicLabel) label;
        return rw.qq().parseExpr(L.name());
    }
}
