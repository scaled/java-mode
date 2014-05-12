//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java

import scaled._
import scaled.grammar.{Grammar, GrammarConfig, GrammarCodeMode}
import scaled.code.{CodeConfig, Commenter, Indenter}
import scaled.util.Chars

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
    effacer("string", stringStyle),

    effacer("storage.type.java", typeStyle), // TODO: handle match-specificity (drop .java)
    effacer("storage.type.generic", typeStyle),
    effacer("storage.type.primitive", typeStyle),
    effacer("storage.type.object", typeStyle), // meh, colors array []s same as type...
    effacer("storage.type.annotation", preprocessorStyle),
    effacer("storage.modifier.java", keywordStyle),
    effacer("storage.modifier.package", constantStyle),
    effacer("storage.modifier.extends", keywordStyle),
    effacer("storage.modifier.implements", keywordStyle),
    effacer("storage.modifier.import", typeStyle),

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
  import CodeConfig._
  import Chars._

  override def configDefs = JavaConfig :: super.configDefs

  override def keymap = super.keymap ++ Seq(
    "ENTER"   -> "electric-newline",
    "S-ENTER" -> "electric-newline"
  )

  override def grammars = JavaConfig.grammars
  override def effacers = JavaConfig.effacers

  override val indenters = List(
//    new Indenter.PairAnchorAlign(config, buffer) {
//      protected val anchorM = Matcher.regexp("\\bfor\\b")
//      protected val secondM = Matcher.regexp("yield\\b")
//    },
//    new Indenter.TryCatchAlign(config, buffer),
//    new Indenter.TryFinallyAlign(config, buffer),
//    new Indenter.IfElseIfElseAlign(config, buffer),
//    new ScalaIndenter.ValueExprBody(config, buffer),
    new JavaIndenter.ExtendsImpls(config, buffer),
    new JavaIndenter.Javadoc(config, buffer),
//    new Indenter.OneLinerWithArgs(config, buffer, blocker, Set("if", "while", "for")),
//    new Indenter.OneLinerNoArgs(config, buffer, Set("else", "do", "try", "finally")),
//    new ScalaIndenter.CaseBody(config, buffer),
    new Indenter.ByBlock(config, buffer) {
      override def readBlockIndent (pos :Loc) = JavaIndenter.readBlockIndent(buffer, pos)
    }
  )

  class JavaCommenter (buffer :Buffer) extends Commenter(buffer) {
    override def commentPrefix = "//"
    override def docPrefix = "*"

    private val openDocM = Matcher.exact("/**")
    private val closeDocM = Matcher.exact("*/")

    def inDoc (p :Loc) :Boolean = {
      val line = buffer.line(p)
      // we need to be on doc-styled text...
      ((buffer.stylesNear(p) contains docStyle) &&
       // and not on the open doc (/**)
       !line.matches(openDocM, p.col) &&
       // and not on or after the close doc (*/)
       (line.lastIndexOf(closeDocM, p.col) == -1))
    }

    def insertDocPre (p :Loc) :Loc = {
      buffer.insert(p, docPrefix, Styles.None)
      p + (0, docPrefix.length)
    }

    override def commentDelimLen (line :LineV, col :Int) =
      if (line.matches(openDocM, col)) openDocM.matchLength
      else if (line.matches(closeDocM, col)) closeDocM.matchLength
      else super.commentDelimLen(line, col)
  }
  override val commenter :JavaCommenter = new JavaCommenter(buffer)

  //
  // FNs

  @Fn("""Inserts a newline, then indents the subsequent line. Handles other "smart" cases such as:
         If newline is inserted in the middle of a Javadoc comment, the next line is prepended with
         * before indenting. TODO: other smarts.""")
  def electricNewline () {
    // shenanigans to determine whether we should auto-insert the doc prefix (* )
    val inDoc = commenter.inDoc(view.point())
    newline()
    val np = view.point()
    if (inDoc && buffer.charAt(np) != '*') view.point() = commenter.insertDocPre(np)
    reindentAtPoint()
  }

  // TODO: more things!
}
