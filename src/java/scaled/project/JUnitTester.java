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
        public SeqV<Path> testSourceDirs () { return project.sourceDirs(); }
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
    return Option.some(Future.success(this));
  //   else {
  //     if (interact) win.emitStatus(s"Running ${classes.size} test(s) in ${proj.name}...")
  //     val result = Promise[Unit]()
  //     val buf = proj.logBuffer
  //     buf.replace(buf.start, buf.end, Line.fromTextNL(s"Tests started at ${new Date}..."))

  //     def patharg (elems :SeqV[AnyRef]) = elems.mkString("\t")
  //     val args = ImmutableMap.of("classpath", patharg(testClasspath),
  //                                "classes", patharg(classes),
  //                                "filter", filter)
  //     session.get.interact("test", args, new Session.Interactor() {
  //       val fails = SeqBuffer[Failure]()
  //       def onMessage (name :String, data :JMap[String,String]) = name match {
  //         case "done" =>
  //           result.succeed(())
  //           true // session is done

  //         case "between" =>
  //           val out = data.get("output")
  //           if (out.length > 0) buf.append(Line.fromTextNL(out))
  //           false

  //         case "results" =>
  //           val duration = System.currentTimeMillis - start
  //           val durstr = if (duration < 1000) s"$duration ms" else s"${duration / 1000} s"
  //           buf.append(Line.fromTextNL(s"Completed in $durstr, at ${new Date}."))
  //           val ran = data.get("ran").toInt
  //           // val ignored = data.get("ignored").toInt
  //           val failed = data.get("failed").toInt
  //           noteResults(win, interact, ran-failed, toVisits(fails))
  //           false

  //         case "started" =>
  //           buf.append(Line.fromTextNL(s"- Started ${data.get("class")} ${data.get("method")}"))
  //           false

  //         case "failure" =>
  //           val (tclass, tmeth, trace) = (data.get("class"), data.get("method"), data.get("trace"))
  //           buf.append(Line.fromTextNL(s"- Failure $tclass $tmeth"))
  //           buf.append(Line.fromTextNL(trace))
  //           extractFailure(trace, tclass, tmeth, fails)
  //           false

  //         case _ =>
  //           buf.append(Line.fromTextNL(s"- Unknown message: $name"))
  //           buf.append(Line.fromTextNL(data.toMapV.mkString("\n")))
  //           false
  //       }
  //     })
  //     Some(result)
    }
}
