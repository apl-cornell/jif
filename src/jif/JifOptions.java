package jif;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.tools.JavaFileManager.Location;

import polyglot.main.OptFlag;
import polyglot.main.OptFlag.Arg;
import polyglot.main.OptFlag.IntFlag;
import polyglot.main.OptFlag.PathFlag;
import polyglot.main.OptFlag.Switch;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.main.UsageError;

/**
 * This object encapsulates various polyglot options.
 */
public class JifOptions extends Options {
    /*
     * Fields for storing values for options.
     */
    /**
     * Should the checking for the robustness condition be disabled?
     */
    public boolean nonRobustness;

    /**
     * Should uncaught exceptions be made fatal?
     */
    public boolean fatalExceptions;

    /**
     * Should we skip label checks entirely?
     */
    public boolean skipLabelChecking;

    /**
     * Use a single Solver to infer labels globally, or solve on a class
     * by class basis.
     */
    public boolean solveGlobally;

    /**
     * Provide more detailed explanation of solver error messages?
     */
    public boolean explainErrors;

    /**
     * Whether the providers of the sources being compiled are trusted.
     */
    public boolean trustedProviders;

    /**
     * The classpath for the Jif signatures of java.lang objects.
     */
    public final List<File> sigcp;

    public enum JifLocations implements Location {
        SIGNATURE_PATH(false);
        private final boolean isOutput;

        private JifLocations(boolean isOutput) {
            this.isOutput = isOutput;
        }

        @Override
        public boolean isOutputLocation() {
            return isOutput;
        }

        @Override
        public String getName() {
            return name();
        }
    }

    /**
     * Output a dependency graph to help the diagnosing of type error?
     */
    public boolean dependencyGraph;

    /**
     * Suppress compile-time warnings, such as those for fail-on-exception.
     */
    protected boolean noWarnings;

    /**
     * Whether to use the provider label for authorization checks when downgrading policies.
     */
    protected boolean authFromProvider;

    /**
     * Constructor
     */
    public JifOptions(ExtensionInfo extension) {
        super(extension);
        sigcp = new ArrayList<File>();
    }

    @Override
    protected void populateFlags(Set<OptFlag<?>> flags) {
        flags.add(new Switch("-globalsolve",
                "infer label variables globally (default: per class)"));
        flags.add(new Switch(new String[] { "-explain", "-e" },
                "provide more detailed "
                        + "explanations of failed label checking"));
        flags.add(new Switch("-nonrobust", "skip robustness checks.", false));
        flags.add(new Switch("-fail-on-exception",
                "re-throw uncaught and undeclared runtime exceptions as fatal errors."));
        flags.add(new Switch("-robust", "force robustness checks", true, true));
        flags.add(new PathFlag<File>("-sigcp", "<path>",
                "path for Jif signatures (e.g. for java.lang.Object)") {
            @Override
            public File handlePathEntry(String entry) {
                File f = new File(entry);
                if (f.exists())
                    return f;
                else return null;
            }
        });
        flags.add(new PathFlag<File>("-addsigcp", "<path>",
                "append <path> to Jif signature path") {
            @Override
            public File handlePathEntry(String entry) {
                File f = new File(entry);
                if (f.exists())
                    return f;
                else return null;
            }
        });
        flags.add(new IntFlag("-debug", "<num>",
                "set debug level to n. Prints more information about labels"));
        flags.add(new Switch("-untrusted-providers",
                "set the providers of the sources being compiled to be untrusted"));
        flags.add(new Switch("-auth-from-provider",
                "Use the provider label to determine authority."));
        flags.add(new Switch("-no-warnings", "suppress compile-time warnings"));
        flags.add(new Switch("-skip-label-checks", "Skip label checking."));
        super.populateFlags(flags);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleArg(Arg<?> arg) throws UsageError {
        if (arg.flag().ids().contains("-globalsolve")) {
            if ((Boolean) arg.value())
                System.err.println("Will use a single solver to infer labels");
            solveGlobally = (Boolean) arg.value();
        } else if (arg.flag().ids().contains("-explain")
                || arg.flag().ids().contains("-e")) {
            explainErrors = (Boolean) arg.value();
        } else if (arg.flag().ids().contains("-skip-label-checks")) {
            skipLabelChecking = (Boolean) arg.value();
        } else if (arg.flag().ids().contains("-nonrobust")) {
            nonRobustness = !(Boolean) arg.value();
        } else if (arg.flag().ids().contains("-fail-on-exception")) {
            fatalExceptions = (Boolean) arg.value();
        } else if (arg.flag().ids().contains("-robust")) {
            nonRobustness = !(Boolean) arg.value();
        } else if (arg.flag().ids().contains("-sigcp")) {
            this.sigcp.clear();
            this.sigcp.addAll((List<File>) arg.value());
        } else if (arg.flag().ids().contains("-addsigcp")) {
            this.sigcp.addAll((List<File>) arg.value());
        } else if (arg.flag().ids().contains("-debug")) {
            Report.addTopic("debug", (Integer) arg.value());
        } else if (arg.flag().ids().contains("-untrusted-providers")) {
            trustedProviders = !(Boolean) arg.value();
        } else if (arg.flag().ids().contains("-auth-from-provider")) {
            authFromProvider = (Boolean) arg.value();
        } else if (arg.flag().ids().contains("-no-warnings")) {
            noWarnings = (Boolean) arg.value();
        } else super.handleArg(arg);
    }

    public boolean noWarnings() {
        return noWarnings;
    }

    public boolean authFromProvider() {
        return authFromProvider;
    }

}
