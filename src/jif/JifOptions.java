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
    public List<File> sigcp = new ArrayList<File>();


    public Location signature_path = new Location() {
        @Override
        public String getName() {
            return "SIGNATURE_PATH";
        }

        @Override
        public boolean isOutputLocation() {
            return false;
        }
    };

    /**
     * Output a dependency graph to help the diagnosing of type error?
     */
    public boolean dependencyGraph;

    /**
     * Constructor
     */
    public JifOptions(ExtensionInfo extension) {
        super(extension);
    }

    @Override
    protected void populateFlags(Set<OptFlag<?>> flags) {
        flags.add(new Switch("-globalsolve", "infer label variables globally (default: per class)"));
        flags.add(new Switch(new String[]{"-explain", "-e"}, "provide more detailed " +
                "explanations of failed label checking"));
        flags.add(new Switch("-nonrobust", "infer label variables globally (default: per class)"));
        flags.add(new Switch("-fail-on-exception", "infer label variables globally (default: per class)"));
        flags.add(new Switch("-robust", "infer label variables globally (default: per class)"));
        flags.add(new PathFlag<File>("-sigcp", "<path>", "path for Jif signatures (e.g. for java.lang.Object)") {
            @Override
            public File handlePathEntry(String entry) {
                File f = new File(entry);
                if (f.exists())
                    return f;
                else return null;
            }
        });
        flags.add(new PathFlag<File>("-addsigcp", "<path>", "append <path> to Jif signature path") {
            @Override
            public File handlePathEntry(String entry) {
                File f = new File(entry);
                if (f.exists())
                    return f;
                else return null;
            }
        });
        flags.add(new IntFlag("-debug", "<num>", "set debug level to n. Prints more information about labels"));
        //flags.add(new Switch("-trusted-providers", "set the providers of the sources being compiled to be trusted"));
        flags.add(new Switch("-untrusted-providers", "set the providers of the sources being compiled to be untrusted"));
        super.populateFlags(flags);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleArg(Arg<?> arg) throws UsageError {
        if (arg.flag().ids().contains("-globalsolve")) {
            if ((Boolean) arg.value())
                System.err.println("Will use a single solver to infer labels");
            solveGlobally = (Boolean) arg.value();
        }
        else if (arg.flag().ids().contains("-explain") || arg.flag().ids().contains("-e")) {
            explainErrors = (Boolean) arg.value();
        }
        else if (arg.flag().ids().contains("-nonrobust")) {
            nonRobustness = (Boolean) arg.value();
        }
        else if (arg.flag().ids().contains("-fail-on-exception")) {
            fatalExceptions = (Boolean) arg.value();
        }
        else if (arg.flag().ids().contains("-robust")) {
            nonRobustness = !(Boolean) arg.value();
        }
        else if (arg.flag().ids().contains("-sigcp")) {
            this.sigcp.clear();
            this.sigcp.addAll((List<File>) arg.value());
        }
        else if (arg.flag().ids().contains("-addsigcp")) {
            this.sigcp.addAll((List<File>) arg.value());
        }
        else if (arg.flag().ids().contains("-debug")) {
            Report.addTopic("debug", (Integer) arg.value());
        }
        else if (arg.flag().ids().contains("-trusted-providers")) {
            trustedProviders = (Boolean) arg.value();
        }
        else if (arg.flag().ids().contains("-untrusted-providers")) {
            trustedProviders = !(Boolean) arg.value();
        }
        else super.handleArg(arg);
    }
}
