//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scaled.*;
import scaled.code.Commenter;
import static scaled.code.CodeConfig.*;

/** Extends [[Commenter]] with some Javadoc smarts. */
public class JavaCommenter extends Commenter {

  public final Matcher atCmdM = Matcher.regexp("@[a-z]+");

  @Override public String linePrefix () { return "//"; }
  @Override public String blockOpen () { return "/*"; }
  @Override public String blockClose () { return "*/"; }
  @Override public String blockPrefix () { return "*"; }
  @Override public String docOpen () { return "/**"; }

  @Override public CommentParagrapher mkParagrapher (Syntax syn, Buffer buf) {
    return new CommentParagrapher(syn, buf) {
      private boolean isAtCmdLine (LineV line) {
        return line.matches(atCmdM, commentStart(line));
      }
      // don't extend paragraph upwards if the current top is an @cmd
      @Override public boolean canPrepend (int row) {
        return super.canPrepend(row) && !isAtCmdLine(line(row+1));
      }
      // don't extend paragraph downwards if the new line is at an @cmd
      @Override public boolean canAppend (int row) {
        return super.canAppend(row) && !isAtCmdLine(line(row));
      }
    };
  }
}
