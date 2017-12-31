//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import java.lang.StringBuilder // override fucking scala.StringBuilder
import codex.model.{Def, Doc}
import org.htmlparser.lexer.Lexer
import org.htmlparser.{Remark, Tag, Text}
import scala.annotation.tailrec
import scala.collection.mutable.{ListBuffer}
import scaled._
import scaled.code.CodeConfig
import scaled.util.{BufferBuilder, Filler}

@Plugin(tag="doc-formatter")
class JavadocFormatterPlugin extends DocFormatterPlugin("java") {
  import DocFormatterPlugin._

  override def format (df :Def, doc :Doc, rawText :String) = {
    // trim the /**, */, and * bits out of the raw doc text (and other sneaky hackery)
    val text = trimDoc(rawText)

    // split the Javadoc into the description and the block tags (separated by tag)
    val chunks = ListBuffer[String]()
    val end = text.length ; var ss = 0 ; var whitepre = true ; var ii = 0
    while (ii < end) {
      val c = text.charAt(ii)
      if (whitepre) {
        // we hit the start of a new tag, add the previous tag
        if (c == '@') {
          chunks += text.substring(ss, ii)
          ss = ii
          whitepre = false
        } else if (!Character.isWhitespace(c)) whitepre = false
      } else if (c == '\n' || c == '\r') whitepre = true
      ii += 1
    }
    chunks += text.substring(ss, end)

    // more hackery! {@docRoot} appears inside an <a> tag which hoses our "split text around
    // {@foo} tags and then HTML parse things" approach; sigh
    val descrip = chunks.head.replaceAll("@docRoot", "DOCROOT")
    val tags = chunks.tail.toList

    new DocFormatterPlugin.Format() {
      override def summary (indent :String, bb :BufferBuilder) {
        val df = new DocFiller(indent, bb)
        format(df, firstLine(descrip))
        df.para() // flush last paragraph
      }

      override def full (indent :String, bb :BufferBuilder) {
        val df = new DocFiller(indent, bb)
        format(df, descrip)
        tags.foreach(formatTag(df, _))
        df.para() // flush last paragraph
      }

      private val DocStyle = CodeConfig.docStyle
      private val CodeStyle = EditorConfig.textStyle

      private def formatTag (df :DocFiller, text :String) {
        val ws = nextws(text, 0) ; val tag = text.substring(0, ws)
        df.list(tag + " ", CodeConfig.preprocessorStyle)
        val styleFirst = (tag == "@param" || tag == "@throws")
        if (!styleFirst) format(df, text.substring(ws))
        // special extra handling to style the @param arg or @throws exception
        else {
          val nws = nextws(text, nextnonws(text, ws))
          val style = if (tag == "@param") CodeConfig.variableStyle else CodeConfig.typeStyle
          df.add(text.substring(ws, nws), style)
          format(df, text.substring(nws))
        }
      }

      private def format (df :DocFiller, text :String) {
        // split the Javadoc up into HTML regions and {@tag} regions so as to avoid running the
        // latter regions through the HTML lexer, which causes antics
        @inline @tailrec def loop (pos :Int) {
          val nextTag = text.indexOf("{@", pos)
          if (nextTag == -1) appendHTML(text.substring(pos), df)
          else {
            if (nextTag > pos) appendHTML(text.substring(pos, nextTag), df)
            def scanEnd (start :Int) :Int = {
              var braces = 0
              var ii = start ; val end = text.length ; while (ii < end) {
                text.charAt(ii) match {
                  case '{' => braces += 1
                  case '}' => braces -= 1
                    if (braces == 0) return ii+1
                  case _ => // keep going
                }
                ii += 1
              }
              end
            }
            val endTag = scanEnd(nextTag)
            appendTag(text.substring(nextTag, endTag), df)
            loop(endTag)
          }
        }
        loop(0)
      }

      private def appendHTML (text :String, df :DocFiller) {
        val lexer = new Lexer(text)
        var style = DocStyle
        var node = lexer.nextNode(false) ; while (node != null) {
          node match {
            case txt :Text =>
              // if we're in a <pre> block, then split the text on newlines and add them as is
              if (df.isPre) prepPre(txt.getText).foreach(df.add)
              // otherwise flatten all whitespace together and append as one blob
              else {
                val ftxt = Filler.flatten(txt.getText)
                // if we're adding a totally blank line to the start of an empty block, then just
                // skip it, we don't want leading whitespace
                if (df.haveLines || ftxt.length > 1 || ftxt.trim.length > 0) df.add(ftxt, style)
              }
            case rem :Remark =>
              println(rem)
            case tag :Tag =>
              tag.getTagName match {
                case "P"   => if (!tag.isEndTag) df.para()
                case "LI"  => if (!tag.isEndTag) df.list("- ", DocStyle)
                case "UL"  => if ( tag.isEndTag) df.para()
                case "PRE" => if ( tag.isEndTag) df.para() else df.pre()
                case _ => style = if (tag.isEndTag) DocStyle
                                  // TODO: would be nice if these layered onto other styles
                                  else tag.getTagName match {
                                    case "TT"|"CODE" => CodeStyle
                                    case "I"|"EM"    => EditorConfig.italicStyle
                                    case "B"         => EditorConfig.boldStyle
                                    case "A"         => EditorConfig.underlineStyle
                                    case _ => println(s"TODO: $tag") ; DocStyle
                                  }
              }
          }
          node = lexer.nextNode(false)
        }
      }

      /** Prepares the contents of a <pre> block for adding; splits on newline and trims preceding
        * and trailing blanks. */
      private def prepPre (text :String) :SeqBuffer[String] = {
        val buf = SeqBuffer[String]() ; buf ++= Line.splitText(text)
        while (!buf.isEmpty && !nonblank(buf.head)) buf.removeAt(0)
        while (!buf.isEmpty && !nonblank(buf.last)) buf.removeAt(buf.size-1)
        buf
      }

      private def appendTag (text :String, df :DocFiller) {
        // determine the body start and tag text (skip {@ when extracting tag)
        val start = nextws(text, 0) ; val tag = text.substring(2, start)
        def closepos (l :String) = l.length-(if (l endsWith "}") 1 else 0)

        // handle link tags specially
        if (tag == "link" || tag == "linkplain") {
          // hacky state machine to skip over method links with whitespace (i.e. #foo(int, int))
          val tgt = new StringBuilder() ; val end = closepos(text)
          var state = 0 // 0 - pre-target, 1 in-target, 2 in-args, 3 post-args, 4 done
          var ii = start ; while (ii < end && state < 4) {
            val c = text.charAt(ii)
            if (Character.isWhitespace(c)) state match {
              case 0 =>               // skip leading whitespace
              case 1 => state = 4     // space pre-args indicates target is done
              case 2 => tgt.append(c) // spaces allowed in args
              case 3 => state = 4     // space post-args indicates target is done
            } else {
              // handle state transitions where we also append the char
              if (state == 0) state = 1
              else if (c == '(') state = 2
              else if (c == ')') state = 3
              tgt.append(c)
            }
            ii += 1
          }
          val label = if (ii < end) text.substring(ii, end).trim else ""
          val show = if (label.length == 0) tgt.toString else label
          val isType = show.length > 0 && Character.isUpperCase(show.charAt(0))
          df.add(show, if (isType) CodeConfig.typeStyle else CodeConfig.functionStyle)

        } else {
          val style = tag match {
            case "literal" => DocStyle
            case "code"    => CodeStyle
            case _         => println(s"TODO: $tag style") ; CodeStyle
          }
          val lines = Line.splitText(text)
          val first = lines.head ; val fstart = nextnonws(first, start)
          if (fstart < first.length) df.add(first.substring(fstart, closepos(first)), style)
          if (lines.length > 1) {
            lines.slice(1, lines.length-1).foreach(df.add(_, style))
            // strip the } suffix from the end of the escaped block
            val last = lines.last.substring(0, closepos(lines.last))
            if (nonblank(last)) df.add(last, style)
          }
        }
      }

      private def firstLine (text :String) :String = {
        val max = text.length
        @tailrec @inline def loop (ii :Int) :Int = if (ii < max) {
          val c = text.charAt(ii) ; val nn = ii+1
          if (c == '.' && nn < max && isEOS(text.charAt(nn))) nn
          else loop(nn)
        } else max
        Filler.flatten(text.substring(0, loop(0)))
      }

      // called on the character after a '.' when extracting the first line of docs
      private def isEOS (c :Char) = c == '<' || Character.isWhitespace(c)
    }
  }

  private def trimDoc (text :String) :String = {
    val buf = new StringBuilder()
    val lines = Line.splitText(text)
    def idx (ii :Int, off :Int, na :Int) = if (ii == -1) na else ii+off
    var ii = 0; while (ii < lines.length) {
      val line = lines(ii)
      val start = if (ii == 0) idx(line.indexOf("/**"), 3, 0)
                  else idx(line.indexOf("*"), 1, 0)
      val end = if (ii == lines.length-1) idx(line.lastIndexOf("*/"), 0, line.length)
                else line.length
      if (end >= start) {
        // sneak in some hacky unhackery; Javadoc 8 introduced @implSpec @implNote and some other
        // tags which apparently just expand to a bold header in normal Javadoc, but also break the
        // contract that the first @foo at the start of a line starts the block tags section, yay!
        specialAtIdx(line, start) match {
          case -1 => buf.append(line, start, end)
          case ii => buf.append(replaceSpecialAt(line.substring(start, end).trim))
        }
        buf.append('\n')
      }
      ii += 1
    }
    buf.toString
  }

  private val SpecialAtMap = Map("@implSpec" -> "<p><b>Implementation Requirements:</b>",
                                 "@implNote" -> "<p><b>Implementation Note:</b>",
                                 "@apiNote"  -> "<p><b>API Note:</b>")
  private val SpecialAts = List() ++ SpecialAtMap.keySet

  private def nextws (text :String, start :Int) :Int = {
    val end = text.length ; var ws = start
    while (ws < end && !Character.isWhitespace(text.charAt(ws))) ws += 1
    ws
  }
  private def nextnonws (text :String, start :Int) :Int = {
    val end = text.length ; var ws = start
    while (ws < end && Character.isWhitespace(text.charAt(ws))) ws += 1
    ws
  }
  private def nonblank (text :String) = nextnonws(text, 0) < text.length

  private def specialAtIdx (line :String, start :Int) :Int = {
    val end = line.length
    var ii = start ; while (ii < end) {
      val c = line.charAt(ii)
      if (c == '@') {
        var at = SpecialAts.head ; while (at != Nil) {
          if (line.regionMatches(ii, at, 0, at.length)) return ii
          at = at.tail
        }
      }
      else if (!Character.isWhitespace(c)) return -1
      ii += 1
    }
    return -1
  }

  private def replaceSpecialAt (line :String) :String =
    SpecialAtMap.fold(line) { (l, kv) => l.replaceAll(kv._1, kv._2) }
}
