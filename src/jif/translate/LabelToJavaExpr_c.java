package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.*;

public abstract class LabelToJavaExpr_c implements LabelToJavaExpr {
    public abstract Expr toJava(Label label, JifToJavaRewriter rw) throws SemanticException;
}
