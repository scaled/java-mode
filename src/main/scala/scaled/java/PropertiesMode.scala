//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java

import scaled._
import scaled.grammar._
import scaled.code.{CodeConfig, Commenter}

object PropertiesConfig extends Config.Defs {
  import CodeConfig._
  import GrammarConfig._

  // map TextMate grammar scopes to Scaled style definitions
  val effacers = List(
    effacer("comment.line", commentStyle),
    effacer("keyword", keywordStyle)
  )

  def propsGrammar = Grammar.parse(stream("JavaProperties.tmLanguage"))
  lazy val grammars = Seq(propsGrammar)
}

@Major(name="properties",
       tags=Array("code", "project", "properties"),
       pats=Array(".*\\.properties", "package.scaled"),
       desc="A major mode for editing Java properties files.")
class PropertiesMode (env :Env) extends GrammarCodeMode(env) {

  override def configDefs = PropertiesConfig :: super.configDefs
  override def grammars = PropertiesConfig.grammars
  override def effacers = PropertiesConfig.effacers

  override val indenters = Nil
  override val commenter = new Commenter(buffer) {
    override def linePrefix = "#"
    // TODO: ! is also a comment start character, sigh...
  }
}
