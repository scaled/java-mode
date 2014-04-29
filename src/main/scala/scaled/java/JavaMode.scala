//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java

import scaled._
import scaled.grammar._
import scaled.major.CodeConfig

object JavaConfig extends Config.Defs {
  import EditorConfig._
  import CodeConfig._
  import GrammarConfig._

  // map TextMate grammar scopes to Scaled style definitions
  val effacers = List(
    // Java code colorizations
    effacer("comment.line", commentStyle),
    effacer("comment.block", docStyle),
    effacer("constant", constantStyle),
    effacer("invalid", warnStyle),
    effacer("keyword", keywordStyle),
    effacer("storage.type.java", typeStyle), // TODO: handle match-specificity (drop .java)
    effacer("storage.type.generic.java", typeStyle),
    effacer("storage.type.annotation", preprocessorStyle),
    effacer("storage.modifier.java", keywordStyle),
    effacer("storage.modifier.package", constantStyle),
    effacer("storage.modifier.extends", keywordStyle),
    effacer("storage.modifier.implements", keywordStyle),
    effacer("storage.modifier.import", typeStyle),
    effacer("storage.type.primitive", typeStyle),
    effacer("entity.name.type.class", typeStyle),
    effacer("entity.other.inherited-class", typeStyle),
    effacer("entity.name.function.java", functionStyle),
    effacer("variable.language", keywordStyle),
    effacer("variable.parameter", variableStyle),
    effacer("variable.other.type", variableStyle),
    // Javadoc colorizations
    effacer("markup.underline", preprocessorStyle),
    // HTML in Javadoc colorizations
    effacer("entity.name.tag", constantStyle)
  )

  def htmlGrammar = Grammar.parse(stream("HTML.tmLanguage"))
  def javaDocGrammar = Grammar.parse(stream("JavaDoc.tmLanguage"))
  def javaGrammar = Grammar.parse(stream("Java.tmLanguage"))
  lazy val grammars = Seq(htmlGrammar, javaDocGrammar, javaGrammar)
}

@Major(name="java",
       tags=Array("code", "project", "java"),
       pats=Array(".*\\.java"),
       ints=Array("java"),
       desc="A major mode for editing Java language source code.")
class JavaMode (env :Env) extends GrammarCodeMode(env) {

  override def dispose () {} // nada for now

  override def configDefs = JavaConfig :: super.configDefs
  override protected def grammars = JavaConfig.grammars
  override protected def effacers = JavaConfig.effacers

  // TODO: more things!
}
