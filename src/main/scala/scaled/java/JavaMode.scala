//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java

import scaled._
import scaled.grammar.{Grammar, GrammarConfig, GrammarCodeMode}
import scaled.code.{CodeConfig, Commenter, Indenter}
import scaled.util.{Chars, Paragrapher}

object JavaConfig extends Config.Defs {
  import CodeConfig._
  import GrammarConfig._

  // map TextMate grammar scopes to Scaled style definitions
  val effacers = List(
    // Java code colorizations
    effacer("comment.line", commentStyle),
    effacer("comment.block", docStyle),
    effacer("constant", constantStyle),
    effacer("invalid", invalidStyle),
    effacer("keyword", keywordStyle),
    effacer("string", stringStyle),

    effacer("storage.type.java", typeStyle), // TODO: handle match-specificity (drop .java)
    effacer("storage.type.generic", typeStyle),
    effacer("storage.type.primitive", typeStyle),
    effacer("storage.type.object", typeStyle), // meh, colors array []s same as type...
    effacer("storage.type.annotation", preprocessorStyle),
    effacer("storage.modifier.java", keywordStyle),
    effacer("storage.modifier.package", moduleStyle),
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

  // map TextMate grammar scopes to Scaled syntax definitions
  val syntaxers = List(
    syntaxer("comment.line", Syntax.LineComment),
    syntaxer("comment.block", Syntax.DocComment),
    syntaxer("constant", Syntax.OtherLiteral),
    syntaxer("string", Syntax.StringLiteral)
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
  override def syntaxers = JavaConfig.syntaxers

  override val indenters = List(
//    new Indenter.PairAnchorAlign(config, buffer) {
//      protected val anchorM = Matcher.regexp("\\bfor\\b")
//      protected val secondM = Matcher.regexp("yield\\b")
//    },
//    new Indenter.TryCatchAlign(config, buffer),
//    new Indenter.TryFinallyAlign(config, buffer),
//    new Indenter.IfElseIfElseAlign(config, buffer),
//    new ScalaIndenter.ValueExprBody(config, buffer),
    new JavaIndenter.ExtendsImpls(indentCtx),
    new JavaIndenter.Javadoc(indentCtx),
//    new Indenter.OneLinerWithArgs(config, buffer, blocker, Set("if", "while", "for")),
//    new Indenter.OneLinerNoArgs(config, buffer, Set("else", "do", "try", "finally")),
//    new ScalaIndenter.CaseBody(config, buffer),
    new Indenter.ByBlock(indentCtx) {
      override def readBlockIndent (pos :Loc) = JavaIndenter.readBlockIndent(buffer, pos)
    }
  )

  override val commenter :JavaCommenter = new JavaCommenter()

  override def detectIndent = { val id = new Indenter.Detecter(3) {
    private val pppM = Matcher.regexp("(public|protected|private)")
    // if the line starts with 'public/protected/private' then it is meaningful
    def consider (line :LineV, start :Int) :Int = if (line.matches(pppM, start)) 1 else 0
  }.detectIndent(buffer) ; println(s"Detected $id") ; id }

  //
  // FNs

  @Fn("""Inserts a newline, then indents the subsequent line. Handles other "smart" cases such as:
         If newline is inserted in the middle of a Javadoc comment, the next line is prepended with
         * before indenting. TODO: other smarts.""")
  def electricNewline () {
    // shenanigans to determine whether we should auto-insert the doc prefix (* )
    val inDoc = commenter.inDoc(buffer, view.point())
    newline()
    val np = view.point()
    if (inDoc && buffer.charAt(np) != '*') view.point() = commenter.insertDocPre(buffer, np)
    reindentAtPoint()
  }

  // TODO: more things!
}
