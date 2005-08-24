package jif.translate;

import java.util.*;

import jif.ast.Jif;
import jif.ast.JifNodeFactory;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ext.jl.qq.QQ;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Source;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.*;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** Visitor which performs rewriting on the AST. */
public class JifToJavaRewriter extends ContextVisitor
{
    private ExtensionInfo java_ext;
    private JifTypeSystem jif_ts;
    private JifNodeFactory jif_nf;
    private Job job;
    private QQ qq;
    
    private Collection additionalClassDecls;
    private Collection newSourceFiles;
    
    private List initializations;

    public JifToJavaRewriter(Job job,
                             JifTypeSystem jif_ts,
                             JifNodeFactory jif_nf,
                             ExtensionInfo java_ext) {
        //super(jif_ts);
        super(job, jif_ts, java_ext.nodeFactory());
        this.job = job;
        this.jif_ts = jif_ts;
        this.jif_nf = jif_nf;
        this.java_ext = java_ext;
        this.qq = new QQ(java_ext);
        this.additionalClassDecls = new LinkedList();
        this.newSourceFiles = new LinkedList();
        this.initializations = new ArrayList();
    }

    public void finish(Node ast) {
        if (ast instanceof SourceCollection) {
            SourceCollection c = (SourceCollection) ast;
            for (Iterator iter = c.sources().iterator(); iter.hasNext(); ) {
                SourceFile sf = (SourceFile)iter.next();
                java_ext.scheduler().addJob(sf.source(), sf);
                
            }
        }
        else {
            java_ext.scheduler().addJob(job.source(), ast);
        }

        
        // now add any additional source files, which should all be public.
        for (Iterator iter = newSourceFiles.iterator(); iter.hasNext(); ) {
            SourceFile sf = (SourceFile)iter.next();
            java_ext.scheduler().addJob(sf.source(), sf);
        }
        newSourceFiles.clear();
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

    public ErrorQueue errorQueue() {
        return job.compiler().errorQueue();
    }

    public NodeVisitor enterCall(Node n) {
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

    public Node leaveCall(Node old, Node n, NodeVisitor v) {
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

    public Expr paramToJava(Param param) throws SemanticException {
        if (param instanceof Label) {
            return labelToJava((Label)param);
        }
        if (param instanceof Principal) {
            return principalToJava((Principal)param);
        }
        throw new SemanticException("Unexpected param " + param);
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

    private ClassType currentClass;
    private boolean inConstructor;

    public ClassType currentClass() {
        return this.currentClass;
    }

    public void enteringClass(ClassType t) {
        this.currentClass = t;
    }
    public void leavingClass() {
        this.currentClass = null;
    }
    public void addInitializer(Stmt s) {
        this.initializations.add(s);
    }

    public List getInitializations() {
        return this.initializations;
    }

    public void addAdditionalClassDecl(ClassDecl cd) {
        this.additionalClassDecls.add(cd);
    }

    /**
     * Take any additional class declarations that can fit into the source file,
     * i.e., non-public class decls.
     */
    public Node leavingSourceFile(SourceFile n) {
        List l = new ArrayList(n.decls().size() + additionalClassDecls.size());
        l.addAll(n.decls());
        for (Iterator iter = this.additionalClassDecls.iterator(); iter.hasNext(); ) {
            ClassDecl cd = (ClassDecl)iter.next();
            if (cd.flags().isPublic()) {
                // cd is public, we will put it in it's own source file.
                SourceFile sf = java_nf().SourceFile(Position.COMPILER_GENERATED, 
                                                     n.package_(), 
                                                     Collections.EMPTY_LIST,
                                                     Collections.singletonList(cd));
                Source s = new Source(cd.name() + ".jif",
                                      n.source().path(),
                                      n.source().lastModified());
                sf = sf.source(s);
                this.newSourceFiles.add(sf);
            }
            else {
                // cd is not public; it's ok to put the class decl in the source file.
                l.add(cd);
            }
        }    
        
        this.additionalClassDecls.clear();
        return n.decls(l);
    }

    public boolean inConstructor() {
        return this.inConstructor;
    }

    public void inConstructor(boolean flag) {
        this.inConstructor = flag;
    }

}
