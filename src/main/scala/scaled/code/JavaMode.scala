//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled._
import scaled.grammar.{Scoper, Selector, Span}
import scaled.major.{CodeConfig, CodeMode}

object JavaConfig extends ConfigDefs {
  import EditorConfig._
  import CodeConfig._

  // map TextMate grammar scopes to Scaled style definitions
  val colorizers = List(
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

@Major(name="java",
       tags=Array("code", "project", "java"),
       pats=Array(".*\\.java"),
       ints=Array("java"),
       desc="A major editing mode for the Java language.")
class JavaMode (editor :Editor, config :Config, view :RBufferView, disp :Dispatcher)
    extends CodeMode(editor, config, view, disp) {

  // TEMP: for now use a TextMate grammar for code highlighting
  val scoper = new Scoper(Grammars.grammars, view.buffer)
  scoper.apply(new Selector.Processor(JavaConfig.colorizers))

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

  // TODO: more things!
}
