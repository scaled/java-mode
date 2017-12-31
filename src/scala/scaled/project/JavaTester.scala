//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import codex.model.Def
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, FileVisitResult, Path, Paths, SimpleFileVisitor}
import java.util.HashMap
import java.util.regex.Pattern
import scaled._

/** Helper functions for Java test runners. */
abstract class JavaTester (p :Project) extends Tester(p) {

  /** The directory that contains our test source files. */
  def testSourceDirs :SeqV[Path]

  override def isTestFunc (df :Def) = super.isTestFunc(df) && {
    val sig = df.sig ; sig.isPresent && (sig.get.text.contains("@Test") ||
                                         sig.get.text.contains("@org.junit.Test"))
  }

  override def findTestFile (file :Path) = {
    def basename (name :String) = name.lastIndexOf(".") match {
      case -1 => name
      case ii => name.substring(0, ii)
    }
    val fbase = basename(file.getFileName.toString)
    if ((fbase endsWith "Test") || (fbase endsWith "IT")) Some(file)
    else {
      val files = SeqBuffer[Path]()
      val testNames = Set(fbase + "Test", fbase + "IT")
      onTestSources { file =>
        if (testNames(basename(file.getFileName.toString))) files += file
        true
      }
      // TODO: prefer foo/bar/BazTest over foo/qux/BazTest for foo/bar/Baz
      files.headOption
    }
  }

  protected def extractFailure (trace :String, tclass :String, tmeth :String,
                                fails :SeqBuffer[Failure]) {
    var info = SeqBuffer[String]()
    trace.split(LineSep) foreach { line =>
      val m = StackPat.matcher(line)
      if (!filterFailTrace(line)) info += line
      if (m.matches) {
        m.group(3).split(":") match {
          case Array(file, line) =>
            fails += Failure(info, tclass, tmeth, file, line.toInt)
          case _ =>  // TODO: ask the Codex for the location of the test method
        }
        // stop when we see a stack frame that's in our test method
        if (m.group(1) == tclass && m.group(2) == tmeth) return
      }
    }
  }

  // TODO: more frameworks?
  protected def filterFailTrace (line :String) = line.contains("org.junit.Assert")

  protected case class Failure (fmsg :SeqV[String], fclass :String, fmeth :String,
                                ffile :String, fline :Int)

  protected def toVisits (fails :SeqV[Failure]) :Seq[Visit] = {
    // find a path for all files in seeking
    val seeking = fails.map(_.ffile).toSet
    val fileToPath = new HashMap[String,Path]()
    onTestSources { file =>
      val name = file.getFileName.toString
      if (seeking(name)) fileToPath.put(name, file)
      true
    }
    fails.flatMap(f => Option(fileToPath.get(f.ffile)) map { p =>
      Compiler.Note(Store(p), Loc(f.fline-1, 0), f.fmsg, true)
    })
  }

  protected def onTestSources (fn :(Path => Boolean)) {
    testSourceDirs foreach { dir =>
      if (Files.exists(dir)) Files.walkFileTree(dir, new SimpleFileVisitor[Path]() {
        override def visitFile (file :Path, attrs :BasicFileAttributes) = {
          if (fn(file)) FileVisitResult.CONTINUE else FileVisitResult.TERMINATE
        }
      })
    }
  }

  protected val StackPat = Pattern.compile("""\s+at (\S+)\.([^.]+)\((\S+)\)""")
  protected val LineSep  = System.getProperty("line.separator")
}
