//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scala.Option;

import scaled.*;
import scaled.code.Block;
import scaled.code.Indenter;
import scaled.util.Chars;

public class JavaIndenter {

  /** Handles reading block (and pseudo-block) indent for Java code. This checks for wrapped
    * `extends` and `implements` clauses before falling back to the standard
    * [[readIndentSkipArglist]].
    */
  public static int readBlockIndent (BufferV buffer, long pos) {
    // if we're looking at extends or implements, move back to the line that contains "class" or
    // "interface" and indent relative to that
    if (Indenter.startsWith(buffer.line(pos), extendsOrImplM)) {
      long p = buffer.findBackward(classOrIfaceM, Loc.atCol$extension(pos, 0), buffer.start());
      if  (p == Loc.None()) {
        System.out.println("Missing (class|interface) for block on (extends|implements) line!");
        return 0;
      } else return Indenter.readIndent(buffer, p);
    }
    // otherwise fall back to readIndentSkipArglist
    else return Indenter.readIndentSkipArglist(buffer, pos);
  }
  private static Matcher classOrIfaceM = Matcher.regexp("\\b(class|interface)\\b");
  private static Matcher extendsOrImplM = Matcher.regexp("(extends|implements)\\b");

  /** Indents `extends` and `implements` relative to a preceding `(class|interface)` line. */
  public static class ExtendsImpls extends Indenter {
    private Matcher classIfaceM = Matcher.regexp("\\b(class|interface)\\b");
    private Matcher extendsImplsM = Matcher.regexp("(extends|implements)\\b");
    public ExtendsImpls (Context ctx) { super(ctx); }

    public Option<Object> apply (Block block, LineV line, long pos) {
      if (!line.matches(extendsImplsM, Loc.c(pos))) return SC.none();
      else {
        long loc = buffer().findBackward(classIfaceM, pos, block.start());
        if (loc == Loc.None()) return SC.none();
        else {
          debug("Indenting extends/implements relative to class/interface @ " + Loc.show(loc));
          return Option.apply(indentFrom(readIndent(buffer(), loc), 2));
        }
      }
    }
  }

  /** Aligns subsequent and final lines in Javadoc comments on the first `*`. */
  public static class Javadoc extends Indenter {
    private Matcher starM = Matcher.exact("*");
    private Matcher openM = Matcher.exact("/*");
    private Matcher docOpenM = Matcher.exact("/**");
    public Javadoc (Context ctx) { super(ctx); }

    public Option<Object> apply (Block block, LineV line, long pos) {
      if (buffer().syntaxAt(pos) != Syntax.DocComment() ||
          !Indenter.startsWith(line, starM)) return SC.none();
      else {
        // scan back to the first line of the comment and indent one from there; the logic is
        // slightly weirded to ensure that we don't go past the start of the buffer even if the
        // situation lacks sanity
        int row = Math.max(Loc.r(pos)-1, 0);
        while (row > 0 && !startsWith(buffer().line(row), openM)) row -= 1;
        // if the open comment row contains only /** then align with the first star, if it
        // contains /** followed by text, align with the second star
        LineV openLine = buffer().line(row);
        int indent;
        int opos = openLine.indexOf(docOpenM, 0);
        if (opos == -1) {
          debug("Aligning javadoc * with block comment start on row " + row + ".");
          indent = 1;
        } else {
          int spos = openLine.indexOf(Chars.isNotWhitespace(), opos+docOpenM.matchLength());
          if (spos == -1) {
            debug("Aligning javadoc * with bare doc comment start on row " + row + ".");
            indent = 1;
          } else {
            debug("Aligning javadoc * with textful doc comment start on row " + row + ".");
            indent = 2;
          }
        }
        return Option.apply(readIndent(openLine) + indent);
      }
    }
  }

  /** Indents continued statements. Various heuristics are applied to determine whether or not we
    * appear to be in a continued statement. See comments in the code for details. */
  public static class ContinuedStmt extends Indenter.PrevLineEnd {
    private Matcher annoM = Matcher.exact("@");
    public ContinuedStmt (Context ctx ) { super(ctx); }

    public Option<Object> apply (Block block, LineV line, long pos, long prevPos) {
      char lc = buffer().charAt(pos), pc = buffer().charAt(prevPos);
      // if the line we're indenting is a block or a comment, we're not applicable
      if (lc == '{' || lc == '}' || lc == '/') return SC.none();
      // various terminators for the continued line that render us inapplicable
      else if (pc == ';' || pc == '{' || pc == '}' || pc == ',') return SC.none();
      // if the line we're continuing starts with an annotation, don't apply; this is not perfect,
      // but the vast majority of the time it's a method annotation, which should not trigger
      // further indentation; it would be nice if we did indent further in cases like:
      // @SuppressWarnings("unchecked") Foo<B> foo = (Foo<B>)
      //     someFooExpr;
      else if (Indenter.startsWith(buffer().line(prevPos), annoM)) return SC.none();
      else {
        debug("Indenting one step from continued statement @ " + Loc.show(prevPos));
        return Option.apply(indentFrom(readIndentSkipArglist(buffer(), prevPos), 1));
      }
    }
  }
}
