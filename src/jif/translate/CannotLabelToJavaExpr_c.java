package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.types.label.Label;
import polyglot.visit.*;

public class CannotLabelToJavaExpr_c extends LabelToJavaExpr_c {
    public Expr toJava(Label L, JifToJavaRewriter rw) throws SemanticException {
        throw new InternalCompilerError(L.position(),
                                        "Cannot translate " + L + " to Java.");
    }
}
