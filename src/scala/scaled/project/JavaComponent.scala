//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import java.nio.file.{Path, Paths}
import scaled._
import scaled.util.BufferBuilder

/** Defines additional persistent data for a Java project. */
case class JavaMeta (
  classes :SeqV[Path],
  outputDir :Path,
  buildClasspath :SeqV[Path],
  execClasspath :SeqV[Path])

/** Handles reading and writing [[JavaMeta]]s. */
object JavaMeta extends Project.MetaMeta[JavaMeta] {

  val zero = JavaMeta(Seq(), Paths.get("unused"), Seq(), Seq())

  def read (in :Map[String,SeqV[String]]) :JavaMeta = {
    val classes = in("classes").map(p => Paths.get(p))
    val Seq(outputDir) = in("dirs")
    val buildCP = in("buildClasspath").map(p => Paths.get(p))
    val execCP = in("execClasspath").map(p => Paths.get(p))
    JavaMeta(classes, Paths.get(outputDir), buildCP, execCP)
  }

  def write (out :ConfigFile.WriteMap, meta :JavaMeta) {
    out.write("classes", meta.classes.map(_.toString))
    out.write("dirs", Seq(meta.outputDir.toString))
    out.write("buildClasspath", meta.buildClasspath.map(_.toString))
    out.write("execClasspath", meta.execClasspath.map(_.toString))
  }
}

/** A component added to projects that have Java (or JVM-language) code. */
class JavaComponent (project :Project) extends Project.Component {

  /** Tracks Java-specific project metadata. */
  val javaMetaV = project.metaValue("java-meta", JavaMeta)

  def classes = javaMetaV().classes
  def outputDir :Path = javaMetaV().outputDir
  def buildClasspath :SeqV[Path] = javaMetaV().buildClasspath
  def execClasspath :SeqV[Path] = javaMetaV().execClasspath

  /** Adds any standard (Java) testing components to this project. This should be called after the
    * [[buildClasspath]] has been updated. */
  def addTesters () {
    JUnitTester.addComponent(project, this)
    TestNGTester.addComponent(project, this)
  }

  override def describeSelf (bb :BufferBuilder) {
    bb.addSubHeader("Java Info")
    bb.addSection("Output dirs:")
    bb.addKeysValues("compile: " -> outputDir.toString)
    bb.addSection("Build classpath:")
    buildClasspath foreach { p => bb.add(p.toString) }
    bb.addSection("Exec classpath:")
    execClasspath foreach { p => bb.add(p.toString) }
  }

  override def close () {} // nada
}
