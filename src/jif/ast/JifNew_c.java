package jif.ast;

import java.util.ArrayList;
import java.util.List;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import jif.visit.JifTypeChecker;
import polyglot.ast.ClassBody;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.New;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

// XXX Should be replaced with extension
@Deprecated
public class JifNew_c extends New_c implements New {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public JifNew_c(Position pos, Expr outer, TypeNode tn, List<Expr> arguments,
            ClassBody body) {
        this(pos, outer, tn, arguments, body, null);
    }

    public JifNew_c(Position pos, Expr outer, TypeNode tn, List<Expr> arguments,
            ClassBody body, Ext ext) {
        super(pos, outer, tn, arguments, body, ext);
    }

    @Override
    public NodeVisitor typeCheckEnter(TypeChecker tc) throws SemanticException {
        JifTypeChecker jtc = (JifTypeChecker) super.typeCheckEnter(tc);
        return jtc.inferClassParameters(true);
    }

    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifNew_c n = (JifNew_c) super.typeCheck(tc);

        Type t = n.objectType.type();
        LabelTypeCheckUtil ltcu =
                ((JifTypeSystem) tc.typeSystem()).labelTypeCheckUtil();
        ltcu.typeCheckType(tc, t);

        n = (JifNew_c) n.type(t);

        return n;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> ex = new ArrayList<Type>(super.throwTypes(ts));
        LabelTypeCheckUtil ltcu = ((JifTypeSystem) ts).labelTypeCheckUtil();

        if (objectType().type() instanceof JifClassType) {
            ex.addAll(ltcu.throwTypes((JifClassType) objectType().type()));
        }
        return ex;
    }
}
