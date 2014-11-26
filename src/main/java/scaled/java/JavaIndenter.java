//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scaled.*;
import scaled.code.Indenter;
import scaled.util.Chars;

public class JavaIndenter extends Indenter.ByBlock {

  public JavaIndenter (Buffer buffer, Config config) {
    super(buffer, config);
  }

  // TODO: make all the things configurable

  @Override public int computeIndent (Indenter.State state, int base, LineV line, int first) {
    // pop case statements out one indentation level
    if (line.matches(caseColonM, first)) return base - indentWidth();
    // bump extends/implements in two indentation levels
    else if (line.matches(extendsImplsM, first)) return base + 2*indentWidth();
    // otherwise do the standard business
    else return super.computeIndent(state, base, line, first);
  }

  @Override public int computeCloseIndent (Indenter.BlockS state, LineV line, int first) {
    // if the top of the stack is a switch + block, then skip both of those
    if (state.next() instanceof SwitchS) return state.next().next().indent(config(), false);
    else return super.computeCloseIndent(state, line, first);
  }

  @Override public BlockStater createStater () {
    return new BlockStater() {
      @Override public State adjustStart (LineV line, int first, int last, State start) {
        // if this line opens a block or doc comment, push a state for it
        if (Indenter.countComments(line, first) > 0) {
          // if this is a doc comment which is followed by non-whitespace, then indent to match the
          // second star rather than the first
          return new CommentS(line.matches(firstLineDocM, first) ? 2 : 1, start);
        }
        else if (config().apply(JavaConfig.INSTANCE.indentSwitchBlock) &&
                 line.matches(switchM, first)) {
          return new SwitchS(start);
        }
        // otherwise leave the start as is
        return start;
      }

      @Override public State adjustEnd (LineV line, int first, int last, State start, State cur) {
        // if this line closes a doc/block comment, pop our comment state from the stack
        if (Indenter.countComments(line, first) < 0) cur = cur.popIf(s -> s instanceof CommentS);

        // determine whether this line is continued onto the next line (heuristically)
        if (last >= 0) {
          char lastC = line.charAt(last);
          boolean isContinued;
          switch (lastC) {
          case '.': case '+': case '-': case '?': case '=': isContinued = true; break;
          case ':': isContinued = !line.matches(caseColonM, first); break;
          default:  isContinued = false; break;
          }
          boolean inContinued = (cur instanceof ContinuedS);
          if (isContinued && !inContinued) return new ContinuedS(cur);
          else if (inContinued && !isContinued) return cur.next(); // pop the ContinuedS
        }

        // otherwise we are full of normalcy
        return cur;
      }

      @Override public State closeBlock (LineV line, char close, int col, State state) {
        State popped = super.closeBlock(line, close, col, state);
        // if there's a SwitchS on top of the stack after we pop a } block, pop it off too
        return (close == '}' && popped instanceof SwitchS) ? popped.next() : popped;
      }
    };
  }

  protected static class CommentS extends Indenter.State {
    public final int inset;
    public CommentS (int inset, State next) {
      super(next);
      this.inset = inset;
    }
    @Override public int indent (Config config, boolean top) {
      return inset + next().indent(config, false);
    }
    @Override public String show () { return "CommentS(" + inset + ")"; }
  }

  protected static class ContinuedS extends Indenter.State {
    public ContinuedS (State next) { super(next); }
    @Override public int indent (Config config, boolean top) {
      // if we're a continued statement directly inside an expr block, let the expr block dictate
      // alignment rather than our standard extra indents
      State n = next();
      return (n instanceof ExprS) ? n.indent(config, top) : super.indent(config, top);
    }
    @Override public String show () { return "ContinuedS"; }
  }

  protected static class SwitchS extends Indenter.State {
    public SwitchS (State next) { super(next); }
    @Override public String show () { return "SwitchS"; }
  }

  private final Matcher caseColonM = Matcher.regexp("(case\\s|default).*:");
  private final Matcher extendsImplsM = Matcher.regexp("(extends|implements)\\b");
  private final Matcher switchM = Matcher.regexp("switch\\b");

  private final Matcher firstLineDocM = Matcher.regexp("/\\*\\*\\s*\\S+");
}
