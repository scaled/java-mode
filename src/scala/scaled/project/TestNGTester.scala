//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import codex.model.{Def, Kind}
import com.google.common.collect.ImmutableMap
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, FileVisitResult, Path, Paths, SimpleFileVisitor}
import java.util.HashMap
import java.util.regex.Pattern
import java.util.{Date, Map => JMap}
import scaled._
// import scaled.prococol.{Session, SubProcess}
import scaled.util.{BufferBuilder, Close, Errors, SubProcess}

object TestNGTester {

  def addComponent (project :Project, java :JavaComponent) {
    // if the classpath contains test-ng, add its tester
    if (java.buildClasspath.exists { p => TestNGPat.matcher(p.getFileName.toString).matches }) {
      project.addComponent(classOf[Tester], new TestNGTester(project) {
        override def testSourceDirs = project.sources.dirs
        override def testOutputDir = java.outputDir
        override def testClasspath = java.buildClasspath
      })
    }
  }

  private val TestNGPat = Pattern.compile("""testng(.*)\.jar""")
}

abstract class TestNGTester (proj :Project) extends Tester {

  // private val jrSource = "git:https://github.com/scaled/junit-runner.git"
  // private val jrMain = "scaled.junit.Main"
  // private val jrCP = proj.metaSvc.service[PackageService].classpath(jrSource).mkString(
  //   System.getProperty("path.separator"))

  val log = proj.metaSvc.log
  // // TODO: allow specification of opts to JUnit JVM
  // val session = new Close.Box[Session](proj.toClose) {
  //   override def create = new Session(proj.metaSvc.exec.ui, new SubProcess.Config() {
  //     override def command = Array("java", "-ea", "-classpath", jrCP, jrMain)
  //     override def cwd = proj.root.path.toFile
  //   })
  // }

  /** The directory that contains our test source files. */
  def testSourceDirs :SeqV[Path]
  /** The directory that contains our compiled test classes. */
  def testOutputDir :Path
  /** The test classpath. This should contain [[testOutputDir]]. */
  def testClasspath :SeqV[Path]

  /** Tests whether `className` represents a test class. Project can customize. (TODO: provide more
    * than classname?) */
  def isTest (className :String) = className.endsWith("Test") || className.startsWith("Test")

  override def describeSelf (bb :BufferBuilder) {
    bb.addSection("Test Sources:")
    testSourceDirs foreach { p => bb.add(p.toString) }
  }

  override def isTestFunc (df :Def) = super.isTestFunc(df) && {
    val sig = df.sig ; sig.isPresent && (sig.get.text.contains("@Test") ||
                                         sig.get.text.contains("@org.testng.annotations.Test"))
  }

  override def abort () {
    // session.get.forceClose()
    // session.close()
  }

  override def findTestFile (file :Path) = {
    def basename (name :String) = name.lastIndexOf(".") match {
      case -1 => name
      case ii => name.substring(0, ii)
    }
    val fbase = basename(file.getFileName.toString)
    if (isTest(fbase)) Some(file)
    else {
      val files = SeqBuffer[Path]()
      val testName = fbase + "Test"
      onTestSources { file =>
        if (basename(file.getFileName.toString) == testName) files += file
        true
      }
      // TODO: prefer foo/bar/BazTest over foo/qux/BazTest for foo/bar/Baz
      files.headOption
    }
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
                   filter :String = "") :Option[Future[Tester]] =
    if (classes.isEmpty) None
    else {
      if (interact) win.emitStatus(s"Running ${classes.size} test(s) in ${proj.name}...")
      val result = Promise[Tester]()
      val buf = proj.logBuffer
      buf.replace(buf.start, buf.end, Line.fromTextNL(s"Tests started at ${new Date}..."))

      // TEMP: for now we just generate a TestNG XML file with our desired bits and then fork a
      // JVM to run the tests, some day we may be more sophisticated

      val xmlPre = Seq(
        """<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd" >""",
        s"""<suite name="${proj.name}-suite" verbose="1" >""",
        s"""  <test name="${proj.name}-test" >""",
        """    <classes>""")
      val xmlClasses = classes.map { name => s"""      <class name="$name" />""" }
      val xmlPost = Seq(
        """    </classes>""",
        """  </test>""",
        """</suite>""")

      val xmlTemp = Files.createTempFile(s"testng-${proj.name}", ".xml")
      try {
        result.onComplete { _ => Files.delete(xmlTemp) }
        Files.write(xmlTemp, xmlPre ++ xmlClasses ++ xmlPost)

        val cmd = Seq("java", "-classpath", testClasspath.mkString(":"),
                      "org.testng.TestNG", "-usedefaultlisteners", "false",
                      xmlTemp.toString())
        val config = SubProcess.Config(cmd.toArray, cwd=proj.root.path)

        // now fork a Java process and send output to the test output buffer
        SubProcess(config, proj.metaSvc.exec, buf, { success =>
          if (success) result.succeed(this)
          else result.fail(new Exception("TestNG process returned failure."))
        })

        // TODO: extract test success/failure from the buffer? that seems complicated, probably
        // better to just throw all this away and integrate TestNG "properly" like JUnit once we
        // have more time to spend on it

      } catch {
        case err :Throwable => result.fail(err)

      }

      Some(result)
    }

  private def onTestSources (fn :(Path => Boolean)) {
    testSourceDirs foreach { dir =>
      if (Files.exists(dir)) Files.walkFileTree(dir, new SimpleFileVisitor[Path]() {
        override def visitFile (file :Path, attrs :BasicFileAttributes) = {
          if (fn(file)) FileVisitResult.CONTINUE else FileVisitResult.TERMINATE
        }
      })
    }
  }
}
