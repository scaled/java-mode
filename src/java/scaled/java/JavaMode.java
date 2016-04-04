//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

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
    return super.configDefs().cons(JavaConfig.INSTANCE);
  }

  @Override public Grammar.Set grammars () {
    return JavaConfig.INSTANCE.grammars.get();
  }
  @Override public List<Selector.Fn> effacers () {
    return JavaConfig.INSTANCE.effacers;
  }
  @Override public List<Selector.Fn> syntaxers () {
    return JavaConfig.INSTANCE.syntaxers;
  }

  @Override public Indenter createIndenter () {
    return new JavaIndenter(config());
  }

  // TODO: val
  @Override public Commenter commenter () {
    return new Commenter() {
      @Override public String linePrefix () { return "//"; }
      @Override public String blockOpen () { return "/*"; }
      @Override public String blockClose () { return "*/"; }
      @Override public String blockPrefix () { return "*"; }
      @Override public String docOpen () { return "/**"; }

      @Override public CommentParagrapher mkParagrapher (Syntax syn, Buffer buf) {
        return new DocCommentParagrapher(syn, buf);
      }
    };
  }

  // TODO: more things!
}
