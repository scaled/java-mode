//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scala.Tuple2;
import scala.collection.immutable.List;
import scala.collection.immutable.Seq;
import scaled.*;
import scaled.code.CodeConfig;
import scaled.code.Commenter;
import scaled.code.Indenter;
import scaled.grammar.Grammar;
import scaled.grammar.GrammarCodeMode;
import scaled.grammar.GrammarConfig;
import scaled.grammar.Selector;

@Major(name="java",
       tags={ "code", "project", "java" },
       pats=".*\\.java",
       ints="java",
       desc="A major mode for editing Java language source code.")
public class JavaMode extends GrammarCodeMode {

  public JavaMode (Env env) {
    super(env);
  }

  @Override public List<Config.Defs> configDefs () {
    return SC.cons(JavaConfig.INSTANCE, super.configDefs());
  }

  @Override public Key.Map keymap () {
    return super.keymap().
      bind("ENTER",   "electric-newline").
      bind("S-ENTER", "electric-newline");
  }

  @Override public Seq<Grammar> grammars () {
    return JavaConfig.INSTANCE.grammars;
  }
  @Override public List<Selector.Fn> effacers () {
    return JavaConfig.INSTANCE.effacers;
  }
  @Override public List<Selector.Fn> syntaxers () {
    return JavaConfig.INSTANCE.syntaxers;
  }

  // TODO: val
  @Override public List<Indenter> indenters () {
    return SC.list(
      //    new Indenter.PairAnchorAlign(config, buffer) {
      //      protected val anchorM = Matcher.regexp("\\bfor\\b")
      //      protected val secondM = Matcher.regexp("yield\\b")
      //    },
      //    new Indenter.TryCatchAlign(config, buffer),
      //    new Indenter.TryFinallyAlign(config, buffer),
      //    new Indenter.IfElseIfElseAlign(config, buffer),
      //    new ScalaIndenter.ValueExprBody(config, buffer),
      new JavaIndenter.ExtendsImpls(indentCtx()),
      new JavaIndenter.Javadoc(indentCtx()),
      //    new Indenter.OneLinerWithArgs(config, buffer, blocker, Set("if", "while", "for")),
      //    new Indenter.OneLinerNoArgs(config, buffer, Set("else", "do", "try", "finally")),
      new JavaIndenter.ContinuedStmt(indentCtx()),
      new Indenter.ByBlock(indentCtx()) {
        @Override public int readBlockIndent (long pos) {
          return JavaIndenter.readBlockIndent(buffer(), pos);
        }
      }
    );
  }

  // TODO: val
  @Override public JavaCommenter commenter () {
    return new JavaCommenter();
  }

  @Override public int detectIndent () {
    return new Indenter.Detecter(3) {
      private Matcher pppM = Matcher.regexp("(public|protected|private)");
      // if the line starts with 'public/protected/private' then it is meaningful
      public int consider (LineV line, int start) {
        return line.matches(pppM, start) ? 1 : 0;
      }
    }.detectIndent(buffer());
  }

  //
  // FNs

  @Fn("Inserts a newline, then indents the subsequent line. Handles other \"smart\" cases such " +
      "as: If newline is inserted in the middle of a Javadoc comment, the next line is prepended " +
      "with * before indenting. TODO: other smarts.")
  public void electricNewline () {
    // shenanigans to determine whether we should auto-insert the doc prefix (* )
    boolean inDoc = commenter().inDoc(buffer(), view().point().get().rowCol());
    newline();
    Loc np = view().point().get();
    if (inDoc && buffer().charAt(np.rowCol()) != '*') {
        view().point().update(new Loc(commenter().insertDocPre(buffer(), np.rowCol())));
    }
    reindentAtPoint();
  }

  // TODO: more things!
}
