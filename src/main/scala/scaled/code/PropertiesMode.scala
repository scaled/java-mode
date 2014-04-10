//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled._
import scaled.grammar.{Selector, Scoper, Span}
import scaled.major.{CodeConfig, CodeMode}

object PropertiesConfig extends Config.Defs {
  import EditorConfig._
  import CodeConfig._

  // map TextMate grammar scopes to Scaled style definitions
  val colorizers = List(
    effacer("comment.line", commentStyle),
    effacer("keyword", keywordStyle)
  )

  /** A predicate we use to strip `code` styles from a line before restyling it. */
  private val codeP = (style :String) => style startsWith "code"

  /** Compiles `selector` into a TextMate grammar selector and pairs it with a function that applies
    * `cssClass` to buffer spans matched by the selector. */
  def effacer (selector :String, cssClass :String) =
    (Selector.parse(selector), (buf :Buffer, span :Span) => {
      // println(s"Applying $cssClass to $span")
      buf.updateStyles(_ - codeP + cssClass, span)
    })
}

@Major(name="properties",
       tags=Array("code", "project", "properties"),
       pats=Array(".*\\.properties"),
       desc="A major mode for editing Java properties files.")
class PropertiesMode (env :Env) extends CodeMode(env) {

  // use a TextMate grammar for code highlighting
  val scoper = new Scoper(Grammars.propsGrammars, view.buffer)
  scoper.apply(new Selector.Processor(PropertiesConfig.colorizers))

  override def configDefs = JavaConfig :: super.configDefs
  override def keymap = super.keymap ++ Seq(
    "M-A-p" -> "show-syntax" // TODO: also M-PI?
  )
  override def dispose () {} // TODO: remove all colorizations?

  @Fn("Displays the TextMate syntax scopes at the point.")
  def showSyntax () {
    val ss = scoper.scopesAt(view.point())
    view.popup() = Popup(if (ss.isEmpty) List("No scopes.") else ss, Popup.UpRight(view.point()))
  }

}
