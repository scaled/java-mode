//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import java.net.URL
import java.nio.file.Path
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

  case class EclipseMeta (name :String)
  def readEclipseMeta (path :Path) = {
    import scala.xml._
    val xml = XML.loadFile(path.toFile)
    EclipseMeta((xml \\ "projectDescription" \ "name").text)
  }
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
