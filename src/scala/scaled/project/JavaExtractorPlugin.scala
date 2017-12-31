//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import codex.extract.JavaExtractor
import scaled._

@Plugin(tag="codex-extractor")
class JavaExtractorPlugin extends ExtractorPlugin {

  override val suffs = Set("java")

  override def extractor (project :Project, suff :String) =
    if (project.isInstanceOf[JDKProject]) {
      val jdkp = project.asInstanceOf[JDKProject]
      Some(new JavaExtractor() {
        override def log (msg :String) = project.log(msg)
      }.summaryMode)
    } else project.component(classOf[JavaComponent]) map {
      java => new JavaExtractor() {
        private val codex = Codex(project.pspace.wspace.editor)
        override def classpath = java.buildClasspath
        override def log (msg :String) = project.log(msg)
      }
    }
}
