//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import java.nio.file.{Files, Path, Paths}
import scaled._
import scaled.pacman.JDK
import scaled.util.{Errors, SubProcess}

@Plugin(tag="runner")
class JavaRunnerPlugin (pspace :ProjectSpace) extends RunnerPlugin(pspace) {

  override def id = "java"

  override def exampleExecutions = Seq(
    "# example.runner: java               # the runner to use for this execution",
    "# example.env:    FOO=bar            # sets the 'FOO' environment variable",
    "# example.jvmver: 8                  # the Java major version to use: 6, 7, 8, ...",
    "# example.jvmarg: -mx256M            # an arg to be passed to the JVM",
    "# example.jvmarg: -Dfoo=bar          # an arg to be passed to the JVM",
    "# example.project: projectname       # the name of the project which contains `class`",
    "# example.class:  foo.bar.HelloWorld # the class whose main method will be run",
    "# example.arg:    Hello              # the first command line arg",
    "# example.arg:    world.             # the second command line arg"
  )

  override protected def config (exec :Execution) :SubProcess.Config = {
    val jvmver = exec.param("jvmver", JDK.thisJDK.majorVersion)
    val jdk = JDK.jdks.find(_.majorVersion == jvmver) getOrElse JDK.thisJDK
    // TODO: it would be nice to issue a warning if we can't find the desired JDK
    val pname = exec.param("project")
    pspace.allProjects.find(_._2 == pname).map(_._1).flatMap(pspace.projectIn) match {
      case None => throw Errors.feedback(
        s"Cannot find project '$pname' for execution '${exec.name}'")

      case Some(proj) => proj.component[JavaComponent] match {
        case None => throw Errors.feedback(
            s"Project '$pname' has no Java component (execution: '${exec.name}')")

        case Some(java) =>
          val cpath = java.execClasspath.mkString(":")
          val cmd = Seq(jdk.binJava.toString, "-classpath", cpath) ++
            exec.param("jvmarg", Seq()) ++ Seq(exec.param("class")) ++ exec.param("arg", Seq())
          SubProcess.Config(cmd.toArray, env=exec.paramMap("env"), cwd=proj.root.path)
      }
    }
  }
}
