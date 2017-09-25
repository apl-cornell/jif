# Polyglot-Jif-Fabric Stack
Installation configurations that work on Mac OSX. 

[TL;DR Skip Down to Installation instructions](https://github.com/K33TY/Polyglot-Jif-Fabric-Stack/blob/master/README.md#install)

### Information about this Stack

 * [Polyglot](https://www.cs.cornell.edu/projects/polyglot/)
 * [Jif](https://www.cs.cornell.edu/jif/)
 * [Fabric](https://www.cs.cornell.edu/projects/fabric/)

### Installation Backstory

I previously attempted to install this environment on Mac OS Yosemite 10.10.5, but I ran into multiple configuration issues. I could not run Polyglot on the commandline due to [Java Toolprovider](https://docs.oracle.com/javase/7/docs/api/javax/tools/ToolProvider.html) continuously returning null regarding there being a compiler, even when my classpaths and paths were correctly configured and a javac was in the bin directory. I managed to run Polyglot for some reason on Eclipse, but I could not figure out why this was operating differently than from the commandline. However, in attempting to install Jif, Eclipse was no longer recognizing that Polyglot had been built and could access the Toolprovider. 

---

# Install

## Prerequisites

The current specifications used for this installation. (It is probable that different operating systems and earlier/later versions of Java JDK will work, but I managed with the following settings.)

* Mac OS Sierra 10.12.6
* Oracle Java JDK v 1.8.0_101-b13
* [Apache Ant](http://ant.apache.org/) : Install as per provided instructions

## Edit ~/.bash_profile

This ensures that the necessary environment variables, paths, and classpaths are set (Note, you need to change \<path\> to the actual paths to the respective directories):

```
~/.bash_profile 
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8.0_101)
export ANT_HOME=<path>/apache-ant-1.10.1
export POLYGLOT=<path>/polyglot
export JIF=<path>/jif
export FABRIC=<path>/fabric
export PATH="$PATH:/usr/local/bin:${ANT_HOME}/bin:${POLYGLOT}/bin:${JAVA_HOME}/bin"
export CLASSPATH=${CLASSPATH}:${POLYGLOT}/classes:${POLYGLOT}/bin:${POLYGLOT}/lib/java_cup.jar:${POLYGLOT}/lib/polyglot.jar:${POLYGLOT}/lib/jflex.jar:${JAVA_HOME}
```

## Polyglot

  1. Ensure javac is in your path and that JFlex.jar is in your classpath or in the Polyglot lib directory
  2. `cd $POLYGLOT`
  3. `ant`
  4. If you run `jlc`, then you will be able to tell if it is running correctly. (I had no issues once upgrading to Sierra)

## Jif

  1. `cd $JIF`
  2. `cp config.properties.in config.properties`
  3. Edit the config.properties file (ensure that you put the full path to the polyglot directory):
      
```
# Jif configuration properties.

# Base directory of Polyglot installation.
#
#   This is optional if the Polyglot JARs are in the lib directory.
#   Must be an absolute path.
#
polyglot.home=<path>/polyglot


# JDK installation directory.
#
jdk.home=${java.home} 

# The following may work better if you have the JAVA_HOME environment variable
# defined:
# jdk.home=${env.JAVA_HOME}

# You may need something like the following on Mac OS
jif-runtime-native.java-include-dir=/Library/Java/JavaVirtualMachines/jdk1.8.0_101.jdk/Contents/Home/include/darwin
```
      
  4. Edit build.xml line 465 to be:
```xml
<javac source="1.7" target="1.7" srcdir="${rt-src}" destdir="${rt-classes}" encoding="UTF-8" debug="on" includes="**" includeantruntime="false">
```
  5. Edit line 1017 to be:
```xml
<javac source="1.7" target="1.7" srcdir="${ext.basedir}/src" destdir="${classes}" encoding="UTF-8" debug="on" includes="${ext}/**" includeantruntime="false">
```
  6. `ant configure`
  7. `ant`

## Fabric

  1. **Check $JAVA_HOME/lib/security** for **local_policy.jar** and **US_export_policy.jar** (or possibly **jce.jar** if running a different version of Java)
    * If this directory does not exist, then create it
    * Download the JCE package matching your version of Java: [JCE.jar 8](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
  2. `cp config.properties.in config.properties`
  3. Edit the config.properties file (ensure that you put the full path to the respective directories)
  
```
# Fabric configuration properties.


# JDK installation directory
#
# This is optional. If not set, then the following values are used, in
# order of precedence:
#
#   1. The jdk.home property from config.properties in Jif's source
#      tree. (This does not exist when using Jif from a jar file.)
#
#   2. The JAVA_HOME environment variable.
#
#   3. The output of the following BASH command (if successful):
#
#          readlink -f $(which javac) \
#            | sed '/\/bin\/javac$/{s|/bin/javac$||;q}; q1'
#
#      (The above sed expression is the same as 's|/bin/javac$||',
#      except it returns with exit code 1 if the substitution is
#      unsuccessful.)
#
#   4. Ant's built-in java.home property (on some systems, this points
#      to the JRE, and not the JDK).
#
# The commented example below uses Ant's built-in java.home property.

jdk.home=${env.JAVA_HOME}


# Jif installation directory
#
# This is optional. By default, the copy of Jif included in the Fabric
# distribution is used.

jif.home=<path>/jif


# Polyglot installation directory
#
# This is optional. By default, the copy of Polyglot included in the
# Fabric distribution is used.

polyglot.home=<path>/polyglot
```
     
  4. `ant configure`
  5. `ant`
  
