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
    project.component[JavaComponent].map(java => new JavaExtractor() {
      override def classpath = java.classes ++ java.buildClasspath
      override def log (msg :String) = project.log(msg)
      // if this is a JDK project, use summary mode to avoid grinding through the amazing four
      // hundred billion method bodies
    }.setSummaryMode(JDKRootPlugin.find(project.root.path).isDefined))
}
