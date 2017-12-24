//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scaled.*;
import scaled.code.CodeConfig;
import scaled.code.Commenter;
import scaled.code.Indenter;
import scaled.grammar.GrammarCodeMode;

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

  @Override public String langScope () {
    return "source.java";
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
    };
  }

  // TODO: more things!
}
