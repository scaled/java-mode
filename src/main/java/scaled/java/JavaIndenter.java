//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

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

    public int apply (Block block, LineV line, long pos) {
      if (!line.matches(extendsImplsM, Loc.c(pos))) return Indenter.NA();
      else {
        long loc = buffer().findBackward(classIfaceM, pos, block.start());
        if (loc == Loc.None()) return Indenter.NA();
        else {
          debug("Indenting extends/implements relative to class/interface @ " + Loc.show(loc));
          return indentFrom(readIndent(buffer(), loc), 2);
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

    public int apply (Block block, LineV line, long pos) {
      if (buffer().syntaxAt(pos) != Syntax.DocComment() ||
          !Indenter.startsWith(line, starM)) return Indenter.NA();
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
        return readIndent(openLine) + indent;
      }
    }
  }

  /** Indents continued statements. Various heuristics are applied to determine whether or not we
    * appear to be in a continued statement. See comments in the code for details. */
  public static class ContinuedStmt extends Indenter.PrevLineEnd {
    private Matcher annoM = Matcher.exact("@");
    public ContinuedStmt (Context ctx ) { super(ctx); }

    protected boolean isContinued (Block block, long pos, long prevPos) {
      char lc = buffer().charAt(pos), pc = buffer().charAt(prevPos);
      // if the line we're indenting is a block or a comment, it's not a continuation
      if (lc == '{' || lc == '}' || lc == '/') return false;
      // various terminators for the continued line that render us inapplicable
      if (pc == ';' || pc == '{' || pc == '}' || pc == ',') return false;
      // if the line we're continuing starts with an annotation, don't apply; this is not perfect,
      // but the vast majority of the time it's a method annotation, which should not trigger
      // further indentation; it would be nice if we did indent further in cases like:
      // @SuppressWarnings("unchecked") Foo<B> foo = (Foo<B>)
      //     someFooExpr;
      if (Indenter.startsWith(buffer().line(prevPos), annoM)) return false;
      return true;
    }

    public int apply (Block block, LineV line, long pos, long prevPos) {
      // if the current line is not a continuation, we're not applicable
      if (!isContinued(block, pos, prevPos)) return Indenter.NA();
      int indent;
      // if the previous line is *also* a continuation, then don't indent further
      long ppos = Loc.atCol$extension(prevPos, buffer().line(prevPos).firstNonWS());
      long pPrevPos = prevNonWS(block, ppos);
      if (isContinued(block, ppos, pPrevPos)) {
        debug("Indenting to match continued continued statement @ " + Loc.show(ppos));
        indent = 0;
      } else {
        debug("Indenting one step from continued statement @ " + Loc.show(prevPos));
        indent = 1;
      }
      return indentFrom(readIndentSkipArglist(buffer(), prevPos), indent);
    }
  }

  /** If we're in a `case` statement's pseudo-block, inset this line one step from the case. */
  public static class CaseBody extends Indenter {
    public CaseBody (Context ctx) { super(ctx); }

    private final Matcher caseColonM = Matcher.regexp("(case\\s|default).*:");
    private final Matcher closeB = Matcher.exact("}");

    public int apply (Block block, LineV line, long pos) {
      // if we're looking at 'case ...:' or '}' then don't apply this rule
      if (startsWith(line, caseColonM) || startsWith(line, closeB)) return Indenter.NA();
      // otherwise if the first line after the start of our block is 'case ...:' then we're in a
      // case pseudo block, so indent relative to the 'case' not the block
      else {
        // TODO: either skip comments, or search for caseArrowM and then make sure it is in our
        // same block... meh
        long caseLoc = Loc.nextL$extension(block.start());
        LineV caseLine = buffer().line(caseLoc);
        if (!startsWith(caseLine, caseColonM)) return Indenter.NA();
        else {
          debug("Identing one step from 'case' @ " + Loc.show(caseLoc));
          return indentFrom(readIndent(caseLine), 1);
        }
      }
    }
  }
}
