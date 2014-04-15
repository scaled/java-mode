//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled._
import scaled.grammar._
import scaled.major.CodeConfig

object PropertiesConfig extends Config.Defs {
  import CodeConfig._
  import GrammarCodeConfig._

  // map TextMate grammar scopes to Scaled style definitions
  val effacers = List(
    effacer("comment.line", commentStyle),
    effacer("keyword", keywordStyle)
  )
}

@Major(name="properties",
       tags=Array("code", "project", "properties"),
       pats=Array(".*\\.properties"),
       desc="A major mode for editing Java properties files.")
class PropertiesMode (env :Env) extends GrammarCodeMode(env) {

  override def configDefs = PropertiesConfig :: super.configDefs
  override protected def grammars = Grammars.propsGrammars
  override protected def effacers = PropertiesConfig.effacers
}
