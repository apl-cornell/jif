package jif.types;

import jif.ast.JifProcedureDecl;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.TypeNode;
import polyglot.types.Type;
import polyglot.util.Position;

public class FixedSignature implements DefaultSignature {
    JifTypeSystem ts;

    public FixedSignature(JifTypeSystem ts) {
        this.ts = ts;
    }

    @Override
    public Label defaultPCBound(Position pos, String methodName) {
        Label ret = ts.topLabel(pos);
        ret.setDescription("default pc label");
        return ret;
    }

    @Override
    public Label defaultArgBound(Formal f) {
        Label ret = ts.topLabel(f.position());
        ret.setDescription("default arg bound");
        return ret;
    }

    @Override
    public Label defaultReturnLabel(ProcedureDecl pd) {
        Label Lr = ts.bottomLabel();

        for (TypeNode tn : pd.throwTypes()) {
            Label excLabel = ts.labelOfType(tn.type(), ts.bottomLabel());
            Lr = ts.join(Lr, excLabel);
        }

        return Lr;
    }

    @Override
    public Label defaultReturnValueLabel(ProcedureDecl pd) {
        JifProcedureDecl jpd = (JifProcedureDecl) pd;

        Label Lrv;
        if (jpd.returnLabel() != null)
            Lrv = jpd.returnLabel().label();
        else Lrv = defaultReturnLabel(pd);

        JifProcedureInstance pi = (JifProcedureInstance) pd.procedureInstance();
        JifTypeSystem jts = (JifTypeSystem) pi.typeSystem();

        for (Type t : pi.formalTypes()) {
            ArgLabel a = (ArgLabel) jts.labelOfType(t);
            Lrv = ts.join(Lrv, a);
        }
        return Lrv;
    }

    @Override
    public Label defaultFieldLabel(FieldDecl fd) {
        Label ret = ts.noComponentsLabel(fd.position());
        ret.setDescription("default field label");
        return ts.noComponentsLabel(fd.position());
    }

    @Override
    public Label defaultArrayBaseLabel(Type baseType) {
        if (baseType.isArray()) {
            // default label is the same label as the ultimate base
            if (ts.isLabeled(baseType.toArray().ultimateBase())) {
                return ts.labelOfType(baseType.toArray().ultimateBase());
            }
        }
        Label l = ts.noComponentsLabel(baseType.position());
        l.setDescription("default array base label");
        return l;
    }

}
