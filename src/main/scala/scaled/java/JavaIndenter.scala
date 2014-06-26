//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java

import scaled._
import scaled.code.{Block, Indenter}
import scaled.util.Chars

object JavaIndenter {
  import Indenter._
  import Chars._

  /** Handles reading block (and pseudo-block) indent for Java code. This checks for wrapped
    * `extends` and `implements` clauses before falling back to the standard
    * [[readIndentSkipArglist]].
    */
  def readBlockIndent (buffer :BufferV, pos :Loc) :Int = {
    // if we're looking at extends or implements, move back to the line that contains "class" or
    // "interface" and indent relative to that
    if (startsWith(buffer.line(pos), extendsOrImplM)) {
      buffer.findBackward(classOrIfaceM, pos.atCol(0)) match {
        case Loc.None => println(
          s"Missing (class|interface) for block on (extends|implements) line!") ; 0
        case      loc => readIndent(buffer, loc)
      }
    }
    // otherwise fall back to readIndentSkipArglist
    else readIndentSkipArglist(buffer, pos)
  }
  private val classOrIfaceM = Matcher.regexp("""\b(class|interface)\b""")
  private val extendsOrImplM = Matcher.regexp("""(extends|implements)\b""")

  /** Indents `extends` and `implements` relative to a preceding `(class|interface)` line. */
  class ExtendsImpls (ctx :Context) extends Indenter(ctx) {
    private val classIfaceM = Matcher.regexp("""\b(class|interface)\b""")
    private val extendsImplsM = Matcher.regexp("""(extends|implements)\b""")

    def apply (block :Block, line :LineV, pos :Loc) :Option[Int] = {
      if (!line.matches(extendsImplsM, pos.col)) None
      else buffer.findBackward(classIfaceM, pos, block.start) match {
        case Loc.None => None
        case loc =>
          debug(s"Indenting extends/implements relative to class/interface @ $loc")
          Some(indentFrom(readIndent(buffer, loc), 2))
      }
    }
  }

  /** Aligns subsequent and final lines in Javadoc comments on the first `*`. */
  class Javadoc (ctx :Context) extends Indenter(ctx) {
    private val starM = Matcher.exact("*")
    private val openM = Matcher.exact("/*")
    private val docOpenM = Matcher.exact("/**")

    def apply (block :Block, line :LineV, pos :Loc) :Option[Int] =
      if (buffer.syntaxAt(pos) != Syntax.DocComment || !startsWith(line, starM)) None
      else {
        // scan back to the first line of the comment and indent one from there; the logic is
        // slightly weirded to ensure that we don't go past the start of the buffer even if the
        // situation lacks sanity
        var row = math.max(pos.row-1, 0)
        while (row > 0 && !startsWith(buffer.line(row), openM)) row -= 1
        // if the open comment row contains only /** then align with the first star, if it
        // contains /** followed by text, align with the second star
        val openLine = buffer.line(row)
        val indent = openLine.indexOf(docOpenM) match {
          case -1 => debug(s"Aligning javadoc * with block comment start on row $row.") ; 1
          case ii => openLine.indexOf(isNotWhitespace, ii+docOpenM.matchLength) match {
            case -1 => debug(s"Aligning javadoc * with bare doc comment start on row $row.") ; 1
            case ii => debug(s"Aligning javadoc * with textful doc comment start on row $row.") ; 2
          }
        }
        Some(readIndent(openLine) + indent)
      }
  }
}
