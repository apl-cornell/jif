package jif;

import static java.io.File.pathSeparator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.tools.JavaFileManager.Location;

import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.main.UsageError;
import polyglot.util.InternalCompilerError;

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
     public String sigcp = null;

     /**
      * Additional classpath entries for Jif signatures.
      */
     public List<String> addSigcp = new ArrayList<String>();
     
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

    /**
     * Set default values for options
     */
    @Override
    public void setDefaultValues() {
        super.setDefaultValues();
        solveGlobally = false;
        explainErrors = false;
        nonRobustness = false;
        trustedProviders = true;
        dependencyGraph = false;
    }

    /**
     * Parse a command
     * @return the next index to process. That is, if calling this method
     *         processes two commands, then the return value should be index+2
     */
    @Override
    protected int parseCommand(String args[], int index, Set<String> source) throws UsageError {
        if (args[index].equals("-globalsolve")) {
            index++;
            System.err.println("Will use a single solver to infer labels");
            solveGlobally = true;
        }
        else if (args[index].equals("-explain") || args[index].equals("-e")) {
            index++;
            explainErrors = true;
        }
        else if (args[index].equals("-nonrobust")) {
            index++;
            nonRobustness = true;
        }
        else if (args[index].equals("-fail-on-exception")) {
            index++;
            fatalExceptions = true;
        }
        else if (args[index].equals("-robust")) {
            index++;
            nonRobustness = false;
        }
        else if (args[index].equals("-sigcp")) {
            index++;
            this.sigcp = args[index++];
        }
        else if (args[index].equals("-addsigcp")) {
            index++;
            this.addSigcp.add(args[index++]);
        }
        else if (args[index].equals("-debug")) {
            index++;
            int level=0;
            try {
                level = Integer.parseInt(args[index]);
            } 
            catch (NumberFormatException e) {
            }
            Report.addTopic("debug", level);
            index++;
        }
        else if (args[index].equals("-trusted-providers")) {
            index++;
            trustedProviders = true;
        }
        else if (args[index].equals("-untrusted-providers")) {
            index++;
            trustedProviders = false;
        }
        else {
            int i = super.parseCommand(args, index, source);
            return i;
        }
        return index;
    }

    /**
     * Print usage information
     */
    @Override
    public void usage(PrintStream out) {
        super.usage(out);
        usageForFlag(out, "-e -explain", "provide more detailed " +
                                         "explanations of failed label checking.");
        usageForFlag(out, "-robust", "enable checking of robustness conditions for downgrading (use -nonrobust to disable).");
        usageForFlag(out, "-debug <n>", "set debug level to n. Prints more information about labels.");
        usageForFlag(out, "-stop_constraint <n>", "halt when the nth constraint is added");
        usageForFlag(out, "-globalsolve", "infer label variables globally (default: per class)");
        usageForFlag(out, "-sigcp <path>", "path for Jif signatures (e.g. for java.lang.Object)");
        usageForFlag(out, "-addsigcp <path>", "additional path for Jif signatures; prepended to sigcp");
        usageForFlag(out, "-fail-on-exception", "fail on uncaught and undeclared runtime exceptions");
        usageForFlag(out, "-trusted-providers", "set the providers of the sources being compiled to be trusted (use -untrusted-providers to disable)");
    }
    
    public String constructOutputExtClasspath() {
        return constructFullClasspath();
    }

    @Override
    public String constructPostCompilerClasspath() {
        String cp = super.constructPostCompilerClasspath() + pathSeparator
                + constructFullClasspath();
        return cp;
    }

}
