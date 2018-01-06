//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import codex.model.Def;
import codex.model.Kind;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import scaled.*;
import scaled.prococol.Session;
import scaled.prococol.SubProcess;
import scaled.util.BufferBuilder;
import scaled.util.Close;
import scaled.util.Errors;

public abstract class JUnitTester extends JavaTester {

  public static void addComponent (Project project, JavaComponent java) {
    // if the classpath contains junit, add its tester
    if (java.buildClasspath().exists(p -> jUnitPat.matcher(p.getFileName().toString()).matches())) {
      project.addComponent(Tester.class, new JUnitTester(project) {
        public SeqV<Path> testSourceDirs () { return project.sources().dirs(); }
        public Path testOutputDir () { return java.outputDir(); }
        public SeqV<Path> testClasspath () { return java.buildClasspath(); }
      });
    }
  }

  private static Pattern jUnitPat = Pattern.compile("junit(.*)\\.jar");

  private String jrSource = "git:https://github.com/scaled/junit-runner.git";
  private String jrMain = "scaled.junit.Main";
  private String jrCP = project.metaSvc().service(PackageService.class).classpath(jrSource).
    mkString(System.getProperty("path.separator"));

  // TODO: allow specification of opts to JUnit JVM
  private Close.Box<Session> session = new Close.Box<Session>(project.toClose()) {
    public Session create () {
      try {
        return new Session(project.metaSvc().exec().ui(), new SubProcess.Config() {
          public String[] command () {
            return new String[] { "java", "-ea", "-classpath", jrCP, jrMain };
          }
          public File cwd () { return project.root().path().toFile(); }
        });
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  };

  /** The directory that contains our compiled test classes. */
  public abstract Path testOutputDir ();
  /** The test classpath. This should contain [[testOutputDir]]. */
  public abstract SeqV<Path> testClasspath ();

  /** Tests whether `className` represents a test class. Project can customize.
    * (TODO: provide more than classname?) */
  public boolean isTest (String className) {
    return className.endsWith("Test");
  }

  public JUnitTester (Project proj) {
    super(proj);
  }

  @Override public void describeSelf (BufferBuilder bb) {
    bb.addSection("Test Sources:");
    testSourceDirs().foreach(p -> bb.add(p.toString()));
  }

  @Override public void abort () {
    try {
      session.get().forceClose();
      session.close();
    } catch (IOException ioe) {
      ioe.printStackTrace(System.err);
    }
  }

  @Override public boolean runAllTests (Window win, boolean interact) {
    long start = System.currentTimeMillis();
    return run(win, interact, start, findTestClasses((src, fqcl) -> true), "").isDefined();
  }

  @Override public boolean runTests (Window win, boolean interact, Path file, SeqV<Def> types) {
    long start = System.currentTimeMillis();
    String source = file.getFileName().toString();
    // this is not perfectly accurate because one may have multiple test compilation units with the
    // same file name, but it gets the job done until we can bring more powerful tools to bear
    SeqV<String> tclasses = findTestClasses((src, fqcl) -> src.equals(source));
    return run(win, interact, start, tclasses, "").isDefined();
  }

  @Override public Future<Tester> runTest (Window win, Path file, Def elem) {
    long start = System.currentTimeMillis();
    String source = file.getFileName().toString();
    SeqV<String> tclasses = findTestClasses((src, fqcl) -> src.equals(source));
    return run(win, true, start, tclasses, elem.name).getOrElse(() -> { throw Errors.feedback(
      "No test class could be found for '" + elem.name + "'."); });
  }

  private SeqV<String> findTestClasses (BiPredicate<String,String> filter) {
    SeqBuffer<String> classes = SeqBuffer.withCapacity(16);
    if (Files.exists(testOutputDir())) {
      ByteCodex codex = ByteCodex.forDir(project.name(), testOutputDir());
      codex.visit(new ByteCodex.Visitor() {
        public void visit (Kind kind, String name, List<String> path, int flags,
                           String source) {
          if (kind == Kind.TYPE) {
            String fqClassName = path.reverse().tail().mkString(".") + "." + name;
            if (isTest(fqClassName) && filter.test(source, fqClassName)) {
              classes.append(fqClassName);
            }
          }
        }
      });
    }
    return classes;
  }

  private Option<Future<Tester>> run (Window win, boolean interact, long start,
                                      SeqV<String> classes, String filter) {
    if (classes.isEmpty()) return Option.none();
    else {
      if (interact) win.emitStatus(
        "Running " + classes.size() + " test(s) in " + project.name() + "...", false);
      Promise<Tester> result = new Promise<>();
      Buffer buf = resultsBuffer();
      buf.replace(buf.start(), buf.end(),
                  Line.fromTextNL("Tests started at " + new Date() + "..."));

      Map<String, String> args = ImmutableMap.of(
        "classpath", testClasspath().mkString("\t"),
        "classes", classes.mkString("\t"),
        "filter", filter);
      session.get().interact("test", args, new Session.Interactor() {
        private SeqBuffer<Failure> fails = SeqBuffer.withCapacity(16);
        public boolean onMessage (String name, Map<String,String> data) {
          switch (name) {
          case "done":
            result.succeed(JUnitTester.this);
            return true; // session is done

          case "between":
            String out = data.get("output");
            if (out.length() > 0) buf.append(Line.fromTextNL(out));
            return false;

          case "results":
            long duration = System.currentTimeMillis() - start;
            String durstr = (duration < 1000) ? duration + " ms" : (duration / 1000) + " s";
            buf.append(Line.fromTextNL("Completed in " + durstr + ", at " + new Date() + "."));
            int ran = Integer.parseInt(data.get("ran"));
            // val ignored = data.get("ignored").toInt
            int failed = Integer.parseInt(data.get("failed"));
            noteResults(win, interact, ran-failed, toVisits(fails));
            return false;

          case "started":
            buf.append(Line.fromTextNL("- Started " + data.get("class") + " " + data.get("method")));
            return false;

          case "failure":
            String tclass = data.get("class"), tmeth = data.get("method"), trace = data.get("trace");
            buf.append(Line.fromTextNL("- Failure " + tclass + " " + tmeth));
            buf.append(Line.fromTextNL(trace));
            extractFailure(trace, tclass, tmeth, fails);
            return false;

          default:
            buf.append(Line.fromTextNL("- Unknown message: " + name));
            buf.append(Line.fromTextNL(scaled.Map.view(data).mkString("\n")));
            return false;
          }
        }
      });
      return Option.some(result);
    }
  }
}
