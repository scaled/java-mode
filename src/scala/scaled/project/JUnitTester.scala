//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import codex.model.{Def, Kind}
import com.google.common.collect.ImmutableMap
import java.nio.file.{Files, Path}
import java.util.regex.Pattern
import java.util.{Date, Map => JMap}
import scaled._
import scaled.prococol.{Session, SubProcess}
import scaled.util.{BufferBuilder, Close, Errors}

object JUnitTester {

  def addComponent (project :Project, java :JavaComponent) {
    // if the classpath contains junit, add its tester
    if (java.buildClasspath.exists { p => JUnitPat.matcher(p.getFileName.toString).matches }) {
      project.addComponent(classOf[Tester], new JUnitTester(project) {
        override def testSourceDirs = project.sourceDirs
        override def testOutputDir = java.outputDir
        override def testClasspath = java.buildClasspath
      })
    }
  }

  private val JUnitPat = Pattern.compile("""junit(.*)\.jar""")
}

abstract class JUnitTester (proj :Project) extends JavaTester(proj) {

  private val jrSource = "git:https://github.com/scaled/junit-runner.git"
  private val jrMain = "scaled.junit.Main"
  private val jrCP = proj.metaSvc.service[PackageService].classpath(jrSource).mkString(
    System.getProperty("path.separator"))

  val log = proj.metaSvc.log
  // TODO: allow specification of opts to JUnit JVM
  val session = new Close.Box[Session](proj.toClose) {
    override def create = new Session(proj.metaSvc.exec.ui, new SubProcess.Config() {
      override def command = Array("java", "-ea", "-classpath", jrCP, jrMain)
      override def cwd = proj.root.path.toFile
    })
  }

  /** The directory that contains our compiled test classes. */
  def testOutputDir :Path
  /** The test classpath. This should contain [[testOutputDir]]. */
  def testClasspath :SeqV[Path]

  /** Tests whether `className` represents a test class. Project can customize. (TODO: provide more
    * than classname?) */
  def isTest (className :String) = className.endsWith("Test")

  override def describeSelf (bb :BufferBuilder) {
    bb.addSection("Test Sources:")
    testSourceDirs foreach { p => bb.add(p.toString) }
  }

  override def abort () {
    session.get.forceClose()
    session.close()
  }

  override def runAllTests (win :Window, interact :Boolean) = {
    val start = System.currentTimeMillis
    run(win, interact, start, findTestClasses((_,_) => true)).isDefined
  }

  override def runTests (win :Window, interact :Boolean, file :Path, types :SeqV[Def]) = {
    val start = System.currentTimeMillis
    val source = file.getFileName.toString
    // this is not perfectly accurate because one may have multiple test compilation units with the
    // same file name, but it gets the job done until we can bring more powerful tools to bear
    run(win, interact, start, findTestClasses((src,_) => src == source)).isDefined
  }

  override def runTest (win :Window, file :Path, elem :Def) = {
    val start = System.currentTimeMillis
    val source = file.getFileName.toString
    val result = run(win, true, start, findTestClasses((src,_) => src == source), elem.name)
    result || { throw Errors.feedback(s"No test class could be found for '${elem.name}'.") }
  }

  private def findTestClasses (filter :(String,String) => Boolean) :SeqV[String] = {
    val classes = SeqBuffer[String]()
    if (Files.exists(testOutputDir)) {
      val codex = ByteCodex.forDir(proj.name, testOutputDir)
      codex.visit(new ByteCodex.Visitor() {
        def visit (kind :Kind, name :String, path :List[String],
                   flags :ByteCodex.Flags, source :String) {
          if (kind == Kind.TYPE) {
            val fqClassName = path.reverse.tail.mkString(".") + "." + name
            if (isTest(fqClassName) && filter(source, fqClassName)) classes += fqClassName
          }
        }
      })
    }
    classes
  }

  private def run (win :Window, interact :Boolean, start :Long, classes :SeqV[String],
                   filter :String = "") :Option[Future[Unit]] =
    if (classes.isEmpty) None
    else {
      if (interact) win.emitStatus(s"Running ${classes.size} test(s) in ${proj.name}...")
      val result = Promise[Unit]()
      val buf = proj.logBuffer
      buf.replace(buf.start, buf.end, Line.fromTextNL(s"Tests started at ${new Date}..."))

      def patharg (elems :SeqV[AnyRef]) = elems.mkString("\t")
      val args = ImmutableMap.of("classpath", patharg(testClasspath),
                                 "classes", patharg(classes),
                                 "filter", filter)
      session.get.interact("test", args, new Session.Interactor() {
        val fails = SeqBuffer[Failure]()
        def onMessage (name :String, data :JMap[String,String]) = name match {
          case "done" =>
            result.succeed(())
            true // session is done

          case "between" =>
            val out = data.get("output")
            if (out.length > 0) buf.append(Line.fromTextNL(out))
            false

          case "results" =>
            val duration = System.currentTimeMillis - start
            val durstr = if (duration < 1000) s"$duration ms" else s"${duration / 1000} s"
            buf.append(Line.fromTextNL(s"Completed in $durstr, at ${new Date}."))
            val ran = data.get("ran").toInt
            // val ignored = data.get("ignored").toInt
            val failed = data.get("failed").toInt
            noteResults(win, interact, ran-failed, toVisits(fails))
            false

          case "started" =>
            buf.append(Line.fromTextNL(s"- Started ${data.get("class")} ${data.get("method")}"))
            false

          case "failure" =>
            val (tclass, tmeth, trace) = (data.get("class"), data.get("method"), data.get("trace"))
            buf.append(Line.fromTextNL(s"- Failure $tclass $tmeth"))
            buf.append(Line.fromTextNL(trace))
            extractFailure(trace, tclass, tmeth, fails)
            false

          case _ =>
            buf.append(Line.fromTextNL(s"- Unknown message: $name"))
            buf.append(Line.fromTextNL(data.toMapV.mkString("\n")))
            false
        }
      })
      Some(result)
    }
}
