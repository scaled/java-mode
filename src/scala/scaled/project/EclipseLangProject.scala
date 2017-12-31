//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import java.net.URL
import java.nio.file.{Files, Path, Paths}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.eclipse.lsp4j._
import scaled._
import scaled.pacman.{Exec, Filez}
import scaled.util.{Close, Errors, Filler, Fetcher}

object EclipseLangProject {

  val ProjectFile = ".project"

  @Plugin(tag="project-finder")
  class FinderPlugin extends ProjectFinderPlugin("langserver", true, classOf[EclipseLangProject]) {
    def checkRoot (root :Path) :Int = if (exists(root, ProjectFile)) 1 else -1
  }

  @Plugin(tag="langserver")
  class EclipseLangPlugin extends LangPlugin {
    def suffs (root :Path) = Set("java", "scala") // TODO: others?
    def canActivate (root :Path) = Files.exists(root.resolve(ProjectFile))
    def createClient (project :Project) = resolveJDTLS(project).
      map(jdtls => new EclipseLangClient(project, serverCmd(project, jdtls)))
  }

  case class EclipseMeta (name :String)
  def readEclipseMeta (path :Path) = {
    import scala.xml._
    val xml = XML.loadFile(path.toFile)
    EclipseMeta((xml \\ "projectDescription" \ "name").text)
  }

  // from whence we download the Eclipse JDT language server
  val JdtFile = "jdt-language-server-latest.tar.gz"
  val JdtUrl = new URL(s"http://download.eclipse.org/jdtls/snapshots/$JdtFile")

  /** Downloads and unpacks the JDT LS, if needed. */
  def resolveJDTLS (project :Project) :Future[Path] = {
    val pkgSvc = project.metaSvc.service[PackageService]
    val selfSource = "git:https://github.com/scaled/java-mode.git"
    val selfRoot = pkgSvc.installDir(selfSource)
    val jdtlsDir = selfRoot.resolve("eclipse-jdt-ls")
    if (Files.exists(jdtlsDir)) Future.success(jdtlsDir)
    else {
      val jdtPath = selfRoot.resolve(JdtFile)
      Fetcher.fetch(project.pspace.wspace.exec, JdtUrl, jdtPath, pct => {
        project.emitStatus(s"Downloading $JdtFile: $pct%", true)
      }).map(targz => {
        project.emitStatus(s"Unpacking $JdtFile...", true)
        val jdtlsTmp = Files.createTempDirectory(selfRoot, "jdtls")
        try {
          untargz(targz, jdtlsTmp)
          Filez.deleteAll(jdtlsDir)
          Files.move(jdtlsTmp, jdtlsDir)
          jdtlsDir
        } finally {
          Files.deleteIfExists(targz)
          Filez.deleteAll(jdtlsTmp)
        }
      })
    }
  }

  /** Unpacks the .tar.gz file at `path` into the `into` directory. */
  def untargz (path :Path, into :Path) {
    using(new GzipCompressorInputStream(Files.newInputStream(path))) { gzin =>
      val tin = new TarArchiveInputStream(gzin)
      var entry = tin.getNextTarEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val file = into.resolve(Paths.get(entry.getName))
          Files.createDirectories(file.getParent)
          Files.copy(tin, file)
        }
        entry = tin.getNextTarEntry
      }
    }
  }

  /** Constructs the command line to invoke the JDT LS daemon. */
  def serverCmd (project :Project, jdtls: Path) = {
    def isLauncherJar (path :Path) = {
      val name = path.getFileName.toString
      name.startsWith("org.eclipse.equinox.launcher_") && name.endsWith(".jar")
    }
    val pluginsDir = jdtls.resolve("plugins")
    val launcherJar = Files.list(pluginsDir).filter(isLauncherJar).findFirst.toOpt || {
      throw Errors.feedback(s"Can't find launcher jar in $pluginsDir")
    }
    val configOS = System.getProperty("os.name") match {
      case name if (name equalsIgnoreCase "linux") => "linux"
      case name if (name startsWith "Windows") => "win"
      case _ => "mac"
    }
    val configDir = jdtls.resolve(s"config_$configOS")
    val dataDir = project.metaFile("eclipse-jdt-ls")
    Seq("java",
        "-Declipse.application=org.eclipse.jdt.ls.core.id1",
        "-Dosgi.bundles.defaultStartLevel=4",
        "-Declipse.product=org.eclipse.jdt.ls.core.product",
        "-noverify",
        "-Xmx1G",
        "-XX:+UseG1GC",
        "-XX:+UseStringDeduplication",
        "-jar", launcherJar.toString,
        "-configuration", configDir.toString,
        "-data", dataDir.toString)
  }
}

class EclipseLangClient (p :Project, cmd :Seq[String]) extends LangClient(p, cmd) {

  override def name = "Eclipse"

  // TODO: tweak the stuff we get back from JDT-LS to make it nicer
}

class EclipseLangProject (ps :ProjectSpace, r :Project.Root) extends AbstractFileProject(ps, r) {
  import EclipseLangProject._

  override protected def computeMeta (oldMeta :Project.Meta) = try {
    val sb = FileProject.stockIgnores
    // meta.get.ignoreNames.foreach { sb += FileProject.ignoreName(_) }
    // meta.get.ignoreRegexes.foreach { sb += FileProject.ignoreRegex(_) }
    ignores() = sb

    addComponent(classOf[Compiler], new LangCompiler(this))

    Future.success(oldMeta.copy(
      name = emeta.get.name,
      sourceDirs = Seq(rootPath) // TODO: ?
    ))
  } catch {
    case err :Throwable => Future.failure(err)
  }

  private[this] val emeta = new Close.Ref[EclipseMeta](toClose) {
    protected def create = readEclipseMeta(configFile)
  }

  private def rootPath = root.path
  private def configFile = rootPath.resolve(EclipseLangProject.ProjectFile)
}
