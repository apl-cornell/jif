package jif.translate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import jif.ast.JifExt;
import jif.ast.JifNodeFactory;
import jif.ast.JifUtil;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Block;
import polyglot.ast.ClassDecl;
import polyglot.ast.Expr;
import polyglot.ast.Import;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceCollection;
import polyglot.ast.SourceFile;
import polyglot.ast.Stmt;
import polyglot.ast.TopLevelDecl;
import polyglot.ast.TypeNode;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Source;
import polyglot.qq.QQ;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** Visitor which performs rewriting on the AST. */
public class JifToJavaRewriter extends ContextVisitor {
    protected ExtensionInfo java_ext;
    protected JifTypeSystem jif_ts;
    protected JifNodeFactory jif_nf;
    protected QQ qq;

    protected Collection<ClassDecl> additionalClassDecls;
    protected Collection<SourceFile> newSourceFiles;

    protected List<Stmt> initializations;
    protected List<Block> staticInitializations;

    public JifToJavaRewriter(Job job, JifTypeSystem jif_ts,
            JifNodeFactory jif_nf, ExtensionInfo java_ext) {
        //super(jif_ts);
        super(job, jif_ts, java_ext.nodeFactory());
        this.job = job;
        this.jif_ts = jif_ts;
        this.jif_nf = jif_nf;
        this.java_ext = java_ext;
        this.qq = new QQ(java_ext);
        this.additionalClassDecls = new LinkedList<ClassDecl>();
        this.newSourceFiles = new LinkedList<SourceFile>();
        this.initializations = new ArrayList<Stmt>();
        this.staticInitializations = new ArrayList<Block>();
        this.haveThisCall = new Cell<>();
    }

    @Override
    public JifToJavaRewriter copy() {
        JifToJavaRewriter rw = (JifToJavaRewriter) super.copy();

        // If we're in a constructor, the copy will share the same haveThisCall
        // value as the original to ensure this information doesn't get lost
        // when we pop back up. Otherwise, if we are not in a constructor, we
        // make a copy of the haveThisCall value.
        if (!inConstructor) {
            rw.haveThisCall = new Cell<>(haveThisCall.value);
        }
        return rw;
    }

    @Override
    public void finish(Node ast) {
        if (ast instanceof SourceCollection) {
            SourceCollection c = (SourceCollection) ast;
            List<SourceFile> sources = c.sources();
            for (SourceFile sf : sources) {
                java_ext.scheduler().addJob(sf.source(), sf);

            }
        } else {
            java_ext.scheduler().addJob(job.source(), ast);
        }

        // now add any additional source files, which should all be public.
        for (SourceFile sf : newSourceFiles) {
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

    @Override
    public ErrorQueue errorQueue() {
        return job.compiler().errorQueue();
    }

    @Override
    public NodeVisitor enterCall(Node n) {
        try {
            JifExt ext = JifUtil.jifExt(n);
            return ext.toJava().toJavaEnter(this);
        } catch (SemanticException e) {
            Position position = e.position();

            if (position == null) {
                position = n.position();
            }

            errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(),
                    position);

            return this;
        }
    }

    @Override
    public Node leaveCall(Node old, Node n, NodeVisitor v) {
        try {
            JifExt ext = JifUtil.jifExt(n);

            Node m = ext.toJava().toJava(this, v);
            if (m.del() instanceof JifExt)
                throw new InternalCompilerError(m + " is still a Jif node.");
            return m;
        } catch (SemanticException e) {
            Position position = e.position();

            if (position == null) {
                position = n.position();
            }

            errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(),
                    position);

            return n;
        }
    }

    public Expr paramToJava(Param param) throws SemanticException {
        return paramToJava(param, qq().parseExpr("this"));
    }

    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     */
    public Expr paramToJava(Param param, Expr thisQualifier)
            throws SemanticException {
        if (param instanceof Label) {
            return labelToJava((Label) param, thisQualifier);
        }
        if (param instanceof Principal) {
            return principalToJava((Principal) param, thisQualifier);
        }
        throw new InternalCompilerError("Unexpected param " + param);
    }

    public Expr labelToJava(Label label) throws SemanticException {
        return labelToJava(label, qq().parseExpr("this"));
    }

    /**
     * @param simplify
     *          whether to attempt to simplify the label when it's constructed
     *          at run time.
     */
    public Expr labelToJava(Label label, boolean simplify)
            throws SemanticException {
        return labelToJava(label, qq().parseExpr("this"), simplify);
    }

    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     */
    public Expr labelToJava(Label label, Expr thisQualifier)
            throws SemanticException {
        return labelToJava(label, thisQualifier, true);
    }

    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     * @param simplify
     *          whether to attempt to simplify the label when it's constructed
     *          at run time.
     */
    public Expr labelToJava(Label label, Expr thisQualifier, boolean simplify)
            throws SemanticException {
        return label.toJava(this, thisQualifier, simplify);
    }

    public Expr principalToJava(Principal principal) throws SemanticException {
        return principalToJava(principal, qq().parseExpr("this"));
    }

    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     */
    public Expr principalToJava(Principal principal, Expr thisQualifier)
            throws SemanticException {
        return principal.toJava(this, thisQualifier);
    }

    public TypeNode typeToJava(Type t, Position pos) throws SemanticException {
        NodeFactory nf = this.java_nf();
        TypeSystem ts = this.java_ts();
        JifTypeSystem jifts = this.jif_ts();

        if (t.isNull()) return canonical(nf, ts.Null(), pos);
        if (t.isVoid()) return canonical(nf, ts.Void(), pos);
        if (t.isBoolean()) return canonical(nf, ts.Boolean(), pos);
        if (t.isByte()) return canonical(nf, ts.Byte(), pos);
        if (t.isChar()) return canonical(nf, ts.Char(), pos);
        if (t.isShort()) return canonical(nf, ts.Short(), pos);
        if (t.isInt()) return canonical(nf, ts.Int(), pos);
        if (t.isLong()) return canonical(nf, ts.Long(), pos);
        if (t.isFloat()) return canonical(nf, ts.Float(), pos);
        if (t.isDouble()) return canonical(nf, ts.Double(), pos);

        if (jifts.isLabel(t)) {
            return nf.TypeNodeFromQualifiedName(pos, jifts.LabelClassName());
        }

        if (jifts.isPrincipal(t)) {
            return nf.TypeNodeFromQualifiedName(pos,
                    jifts.PrincipalClassName());
        }

        if (t.isArray()) {
            return nf.ArrayTypeNode(pos, typeToJava(t.toArray().base(), pos));
        }

        if (t.isClass()) {
            return nf.TypeNodeFromQualifiedName(pos, t.toClass().fullName());
        }

        throw new InternalCompilerError("Cannot translate type " + t + ".");
    }

    protected TypeNode canonical(NodeFactory nf, Type t, Position pos) {
        return nf.CanonicalTypeNode(pos, t);
    }

    public QQ qq() {
        return qq;
    }

    private ClassType currentClass;
    private boolean inConstructor;

    /**
     * When we're in a constructor, this will (eventually) be populated with a
     * boolean indicating whether the constructor calls {@code this(...)}.
     */
    private Cell<Boolean> haveThisCall;

    public ClassType currentClass() {
        return this.currentClass;
    }

    public void enteringClass(ClassType t) {
        this.currentClass = t;
    }

    public void leavingClass() {
        this.currentClass = null;
    }

    public void addInitializer(Block s) {
        this.initializations.add(s);
    }

    /**
     * @throws SemanticException
     */
    public void addInitializer(FieldInstance fi, Expr init)
            throws SemanticException {
        Stmt s = qq().parseStmt(fi.name() + " = %E;", init);
        this.initializations.add(s);
    }

    public List<Stmt> getInitializations() {
        return this.initializations;
    }

    public void addStaticInitializer(Block s) {
        this.staticInitializations.add(s);
    }

    public List<Block> getStaticInitializations() {
        return this.staticInitializations;
    }

    public void addAdditionalClassDecl(ClassDecl cd) {
        this.additionalClassDecls.add(cd);
    }

    /**
     * Take any additional class declarations that can fit into the source file,
     * i.e., non-public class decls.
     */
    public Node leavingSourceFile(SourceFile n) {
        List<TopLevelDecl> l = new ArrayList<TopLevelDecl>(
                n.decls().size() + additionalClassDecls.size());
        l.addAll(n.decls());
        for (ClassDecl cd : this.additionalClassDecls) {
            if (cd.flags().isPublic()) {
                try {
                    // cd is public, we will put it in its own source file.
                    SourceFile sf = java_nf().SourceFile(
                            Position.compilerGenerated(), n.package_(),
                            Collections.<Import> emptyList(),
                            Collections.singletonList((TopLevelDecl) cd));

                    Location location = java_ext.getOptions().source_output;
                    String pkgName = "";
                    if (sf.package_() != null)
                        pkgName = sf.package_().package_().fullName() + ".";
                    JavaFileObject jfo = java_ext.extFileManager()
                            .getJavaFileForOutput(location, pkgName + cd.name(),
                                    Kind.SOURCE, null);
                    Source s = java_ext.createFileSource(jfo,
                            Source.Kind.COMPILER_GENERATED);
                    sf = sf.source(s);
                    this.newSourceFiles.add(sf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
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
        this.haveThisCall.value = flag ? false : null;
    }

    public Boolean haveThisCall() {
        return haveThisCall.value;
    }

    public void haveThisCall(boolean value) {
        this.haveThisCall.value = value;
    }

    /**
     * The full class path of the runtime label utility.
     */
    public String runtimeLabelUtil() {
        return jif_ts().LabelUtilClassName() + ".singleton()";
    }

    /** A ref cell. */
    private static class Cell<T> {
        T value;

        Cell() {
            this(null);
        }

        Cell(T value) {
            this.value = value;
        }
    }
}
