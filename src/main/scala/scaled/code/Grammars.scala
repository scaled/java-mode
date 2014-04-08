//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled.grammar._

object Grammars {

  def htmlGrammar = Grammar.parse(stream("HTML.tmLanguage"))
  def javaDocGrammar = Grammar.parse(stream("JavaDoc.tmLanguage"))
  def javaGrammar = Grammar.parse(stream("Java.tmLanguage"))
  lazy val grammars = Seq(htmlGrammar, javaDocGrammar, javaGrammar)

  private def stream (path :String) = getClass.getClassLoader.getResourceAsStream(path)
}
