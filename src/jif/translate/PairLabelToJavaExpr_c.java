package jif.translate;

import java.util.Iterator;
import java.util.LinkedList;

import jif.types.label.ConfPolicy;
import jif.types.label.IntegPolicy;
import jif.types.label.JoinPolicy_c;
import jif.types.label.Label;
import jif.types.label.MeetPolicy_c;
import jif.types.label.PairLabel;
import jif.types.label.Policy;
import jif.types.label.ReaderPolicy;
import jif.types.label.WriterPolicy;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class PairLabelToJavaExpr_c extends LabelToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Label label, JifToJavaRewriter rw)
            throws SemanticException {
        PairLabel pl = (PairLabel) label;
        if (pl.confPolicy().isBottomConfidentiality()
                && pl.integPolicy().isTopIntegrity()) {
            return rw.qq().parseExpr(rw.runtimeLabelUtil() + ".noComponents()");
        }
        Expr cexp = policyToJava(pl.confPolicy(), rw);
        Expr iexp = policyToJava(pl.integPolicy(), rw);
        return rw.qq().parseExpr(rw.runtimeLabelUtil() + ".toLabel(%E, %E)",
                cexp, iexp);
    }

    public Expr policyToJava(Policy p, JifToJavaRewriter rw)
            throws SemanticException {
        if (p instanceof ConfPolicy
                && ((ConfPolicy) p).isBottomConfidentiality()) {
            return rw.qq().parseExpr(rw.runtimeLabelUtil() + ".bottomConf()");
        }
        if (p instanceof IntegPolicy && ((IntegPolicy) p).isTopIntegrity()) {
            return rw.qq().parseExpr(rw.runtimeLabelUtil() + ".topInteg()");
        }
        if (p instanceof WriterPolicy) {
            WriterPolicy policy = (WriterPolicy) p;
            Expr owner = rw.principalToJava(policy.owner());
            Expr writer = rw.principalToJava(policy.writer());
            return rw.qq().parseExpr(
                    rw.runtimeLabelUtil() + ".writerPolicy(%E, %E)", owner,
                    writer);
        }

        if (p instanceof ReaderPolicy) {
            ReaderPolicy policy = (ReaderPolicy) p;
            Expr owner = rw.principalToJava(policy.owner());
            Expr reader = rw.principalToJava(policy.reader());
            return (Expr) rw.qq()
                    .parseExpr(rw.runtimeLabelUtil() + ".readerPolicy(%E, %E)",
                            owner, reader)
                    .position(Position.compilerGenerated(
                            p.toString() + ":" + p.position().toString()));
        }

        if (p instanceof JoinPolicy_c) {
            @SuppressWarnings("unchecked")
            JoinPolicy_c<Policy> jp = (JoinPolicy_c<Policy>) p;
            LinkedList<Policy> l = new LinkedList<Policy>(jp.joinComponents());
            Iterator<Policy> iter = l.iterator();
            Policy head = iter.next();
            Expr e = policyToJava(head, rw);
            while (iter.hasNext()) {
                head = iter.next();
                Expr f = policyToJava(head, rw);
                e = rw.qq().parseExpr("%E.join(%E)", e, f);
            }
            return e;
        }

        if (p instanceof MeetPolicy_c) {
            @SuppressWarnings("unchecked")
            MeetPolicy_c<Policy> mp = (MeetPolicy_c<Policy>) p;
            LinkedList<Policy> l = new LinkedList<Policy>(mp.meetComponents());
            Iterator<Policy> iter = l.iterator();
            Policy head = iter.next();
            Expr e = policyToJava(head, rw);
            while (iter.hasNext()) {
                head = iter.next();
                Expr f = policyToJava(head, rw);
                e = rw.qq().parseExpr("%E.meet(%E)", e, f);
            }
            return e;
        }

        throw new InternalCompilerError("Cannot translate policy " + p);
    }

}
