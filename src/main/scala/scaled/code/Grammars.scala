//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled.grammar._

object Grammars {

  def htmlGrammar = Grammar.parse(stream("HTML.tmLanguage"))
  def javaDocGrammar = Grammar.parse(stream("JavaDoc.tmLanguage"))
  def javaGrammar = Grammar.parse(stream("Java.tmLanguage"))
  lazy val javaGrammars = Seq(htmlGrammar, javaDocGrammar, javaGrammar)

  def propsGrammar = Grammar.parse(stream("JavaProperties.tmLanguage"))
  lazy val propsGrammars = Seq(propsGrammar)

  private def stream (path :String) = getClass.getClassLoader.getResourceAsStream(path)
}
