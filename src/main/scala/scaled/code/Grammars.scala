//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import scaled.grammar._

object Grammars {

  def javaGrammar = Grammar.parse(stream("Java.tmLanguage"))
  def javaDocGrammar = Grammar.parse(stream("JavaDoc.tmLanguage"))
  lazy val grammars = Seq(javaDocGrammar, javaGrammar)

  private def stream (path :String) = getClass.getClassLoader.getResourceAsStream(path)
}
