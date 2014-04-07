//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled._
import scaled.grammar.{Scoper, Selector, Span}
import scaled.major.{CodeConfig, CodeMode}

@Major(name="java",
       tags=Array("code", "project", "java"),
       pats=Array(".*\\.java"),
       ints=Array("java"),
       desc="A major editing mode for the Java language.")
class JavaMode (editor :Editor, config :Config, view :RBufferView, disp :Dispatcher)
    extends CodeMode(editor, config, view, disp) {

  // map TextMate grammar scopes to Scaled style definitions
  import EditorConfig._
  import CodeConfig._
  val colorizers = Map(
    Selector.parse("comment")  -> efface(commentStyle),
    Selector.parse("constant") -> efface(constantStyle),
    Selector.parse("invalid")  -> efface(warnStyle),
    Selector.parse("keyword") -> efface(keywordStyle),
    Selector.parse("storage modifier") -> efface(keywordStyle),
    Selector.parse("storage type") -> efface(typeStyle),
    Selector.parse("variable parameter") -> efface(variableStyle),
    Selector.parse("variable language") -> efface(keywordStyle),
    Selector.parse("variable other type") -> efface(variableStyle))

  private def efface (cssClass :String) = (buf :Buffer, span :Span) => {
    // TODO: first remove all code faces, then add the desired faces?
    buf.addStyle(cssClass, span)
  }

  // TEMP: for now use a TextMate grammar for code highlighting
  val scoper = new Scoper(Seq(Grammars.javaDoc), view.buffer)
  scoper.apply(new Selector.Processor(colorizers))

  override def dispose () {} // TODO

  // TODO: things!
}
