package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import jif.ast.*;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.frontend.Job;
import polyglot.frontend.Pass;
import polyglot.frontend.ExtensionInfo;
import polyglot.types.Package;
import polyglot.ext.jl.qq.QQ;
import java.util.*;

/** Visitor which performs type checking on the AST. */
public class JifToJavaRewriter extends HaltingVisitor
{
    ExtensionInfo java_ext;
    JifTypeSystem jif_ts;
    JifNodeFactory jif_nf;
    Job job;
    QQ qq;

    public JifToJavaRewriter(Job job,
                             JifTypeSystem jif_ts,
                             JifNodeFactory jif_nf,
                             ExtensionInfo java_ext) {
        this.job = job;
        this.jif_ts = jif_ts;
        this.jif_nf = jif_nf;
        this.java_ext = java_ext;
        this.qq = new QQ(java_ext);
    }

    public void finish(Node ast) {
        // System.out.println("-- finished with " + job);

        if (ast instanceof SourceCollection) {
            SourceCollection c = (SourceCollection) ast;

            if (c.sources().isEmpty()) {
              // System.out.println("-- discarding ast");
              return;
            }
            else if (c.sources().size() == 1) {
              ast = (SourceFile) new ArrayList(c.sources()).get(0);
            }
            else {
                throw new InternalCompilerError("Unimplemented: cannot " +
                                                "hand off an AST to JL with " +
                                                "more than one source file.");
            }
        }

        // System.out.println("-- handing off ast " + ast);

        // Start a new JL job to finish the translation.
        java_ext.scheduler().addJob(job.source(), ast);
    }

    public JifTypeSystem jif_ts() {
        return jif_ts;
    }

    public JifNodeFactory jif_nf() {
        return jif_nf;
    }

    public TypeSystem java_ts() {
        return java_ext.typeSystem();
    }

    public NodeFactory java_nf() {
        return java_ext.nodeFactory();
    }

    public ExtensionInfo java_ext() {
        return java_ext;
    }

    public ErrorQueue errorQueue() {
        return job.compiler().errorQueue();
    }

    public NodeVisitor enter(Node n) {
        try {
            Jif ext = (Jif) n.ext();
            return ext.del().toJava().toJavaEnter(this);
        }
        catch (SemanticException e) {
            Position position = e.position();

            if (position == null) {
                position = n.position();
            }

            errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                 e.getMessage(), position);

            return this;
        }
    }

    public Node leave(Node old, Node n, NodeVisitor v) {
        try {
            Jif ext = (Jif) n.ext();
            Node m = ext.del().toJava().toJava(this);
            if (m.del() instanceof Jif)
                throw new InternalCompilerError(m + " is still a Jif node.");
            return m;
        }
        catch (SemanticException e) {
            Position position = e.position();

            if (position == null) {
                position = n.position();
            }

            errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                 e.getMessage(), position);

            return n;
        }
    }

    public Expr labelToJava(Label label) throws SemanticException {
        return label.toJava(this);
    }

    public Expr principalToJava(Principal principal) throws SemanticException {
        return principal.toJava(this);
    }

    public QQ qq() {
        return qq;
    }

    ClassType currentClass;
    boolean inConstructor;

    public ClassType currentClass() {
        return this.currentClass;
    }

    public void currentClass(ClassType t) {
        this.currentClass = t;
    }

    public boolean inConstructor() {
        return this.inConstructor;
    }

    public void inConstructor(boolean flag) {
        this.inConstructor = flag;
    }
}
