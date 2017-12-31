//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import codex.extract.SourceSet
import java.nio.file.Path
import java.util.function.Predicate
import java.util.zip.ZipEntry
import scaled._
import scaled.pacman.JDK

/** A special project for use when another project depends on the JDK. This will eventually handle
  * locating JDKs on different platforms and turning the contents of src.zip into a Codex, and
  * whatever other special casery arises.
  */
class JDKProject (ps :ProjectSpace, val jdk :JDK)
    extends AbstractZipFileProject(ps, Project.Root(jdk.root)) {
  import Project._

  override val zipPaths = Seq(jdk.root)

  override protected def computeMeta (oldMeta :Meta) = {
    Future.success(oldMeta.copy(
      name = s"jdk-${jdk.version}",
      ids = Seq(PlatformId(JavaPlatform, jdk.majorVersion))
    ))
  }

  override def summarizeSources = Map(
    "java" -> new SourceSet.Archive(zipPaths.head, new Predicate[ZipEntry] {
      def test (entry :ZipEntry) = entry.getName.startsWith("java")
    })
  )
}

object JDKProject {
  import Project._

  @Plugin(tag="project-finder")
  class FinderPlugin extends ProjectFinderPlugin("jdk", true, classOf[JDKProject]) {
    override def checkRoot (root :Path) :Int = if (JDK.jdks.exists(_.root == root)) 1 else -1

    override def apply (id :Id) = id match {
      case PlatformId(JavaPlatform, version) =>
        JDK.jdks.find(_.majorVersion == version).map(jdk => seed(Root(jdk.root), jdk :: Nil))
      case _ => None
    }

    override protected def injectArgs (root :Root) = List(JDK.jdks.find(_.root == root.path) || {
      throw new IllegalArgumentException(s"Invalid JDK root: $root")
    })
  }
}
