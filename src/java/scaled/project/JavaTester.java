//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import codex.model.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import scaled.*;

/** Helper functions for Java test runners. */
public abstract class JavaTester extends Tester {

  /** The project whose code we're testing. */
  public final Project project;

  /** The java component of the project we're testing. */
  public final JavaComponent java;

  public JavaTester (Project project, JavaComponent java) {
    super(project);
    this.project = project;
    this.java = java;
  }

  /** The directory that contains our test source files. */
  public SeqV<Path> testSourceDirs () {
    return project.sources().dirs();
  }

  private static String basename (String name) {
    int didx = name.lastIndexOf(".");
    return didx == -1 ? name : name.substring(0, didx);
  }

  @Override public Option<Path> findTestFile (Path sfile) {
    String fbase = basename(sfile.getFileName().toString());
    if (fbase.endsWith("Test") || fbase.endsWith("IT")) return Option.some(sfile);
    else {
      SeqBuffer<Path> files = SeqBuffer.withCapacity(8);
      Set<String> testNames = Std.set(fbase + "Test", fbase + "IT");
      onTestSources(tfile -> {
        if (testNames.apply(basename(tfile.getFileName().toString()))) files.append(tfile);
        return true;
      });
      // TODO: prefer foo/bar/BazTest over foo/qux/BazTest for foo/bar/Baz
      return files.headOption();
    }
  }

  /** Tests whether `className` represents a test class. Project can customize.
    * (TODO: provide more than classname?) */
  public boolean isTestClass (String className) {
    return className.endsWith("Test");
  }

  protected SeqV<String> findTestClasses (BiPredicate<String,String> filter) {
    SeqBuffer<String> classes = SeqBuffer.withCapacity(16);
    java.classes().foreach(dir -> {
      if (Files.exists(dir)) {
        ByteCodex codex = ByteCodex.forDir(project.name(), dir);
        codex.visit(new ByteCodex.Visitor() {
          public void visit (Kind kind, String name, List<String> path, int flags,
                             String source) {
            if (kind == Kind.TYPE) {
              String fqClassName = (path.size() <= 1) ? name :
                path.reverse().tail().mkString(".") + "." + name;
              if (isTestClass(fqClassName) && filter.test(source, fqClassName)) {
                classes.append(fqClassName);
              }
            }
          }
        });
      }
      return null;
    });
    return classes;
  }

  protected void extractFailure (String trace, String tclass, String tmeth,
                                 SeqBuffer<Failure> fails) {
    SeqBuffer<String> info = SeqBuffer.withCapacity(16);
    for (String line : trace.split(LINE_SEP)) {
      Matcher m = STACK_PAT.matcher(line);
      if (!filterFailTrace(line)) info.append(line);
      if (m.matches()) {
        String[] bits = m.group(3).split(":");
        if (bits.length == 2) {
          fails.append(new Failure(info, tclass, tmeth, bits[0], Integer.parseInt(bits[1])));
        } // else TODO: ask the Codex for the location of the test method
        // stop when we see a stack frame that's in our test method
        if (m.group(1).equals(tclass) && m.group(2).equals(tmeth)) return;
      }
    }
  }

  // TODO: more frameworks?
  protected boolean filterFailTrace (String line) {
    return line.contains("org.junit.Assert");
  }

  protected class Failure {
    public SeqV<String> fmsg;
    public String fclass;
    public String fmeth;
    public String ffile;
    public int fline;

    public Failure (SeqV<String> fmsg, String flcass, String fmeth, String ffile, int fline) {
      this.fmsg = fmsg;
      this.fclass = fclass;
      this.fmeth = fmeth;
      this.ffile = ffile;
      this.fline = fline;
    }
  }

  protected Seq<Visit> toVisits (SeqV<Failure> fails) {
    // find a path for all files in seeking
    Set<String> seeking = fails.map(f -> f.ffile).toSet();
    Map<String, Path> fileToPath = new HashMap<>();
    onTestSources(file -> {
      String name = file.getFileName().toString();
      if (seeking.apply(name)) fileToPath.put(name, file);
      return true;
    });
    return fails.foldBuild((b, f) -> {
      Path p = fileToPath.get(f.ffile);
      if (p != null) b.append(new Compiler.Note(Store.apply(p), Loc.apply(f.fline-1, 0), f.fmsg, true));
      return null;
    });
  }

  protected void onTestSources (Function<Path, Boolean> fn) {
    testSourceDirs().foreach(dir -> {
      try {
        if (Files.exists(dir)) Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
          public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {
            return fn.apply(file) ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
          }
        });
      } catch (IOException ioe) {
        ioe.printStackTrace(System.err);
      }
      return null;
    });
  }

  protected final Pattern STACK_PAT = Pattern.compile("\\s+at (\\S+)\\.([^.]+)\\((\\S+)\\)");
  protected final String LINE_SEP  = System.getProperty("line.separator");
}
