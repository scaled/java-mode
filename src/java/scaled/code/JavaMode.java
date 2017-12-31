//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code;

import scaled.*;
import scaled.grammar.GrammarCodeMode;

@Major(name="java",
       tags={ "code", "project", "java" },
       pats=".*\\.java",
       ints="java",
       desc="A major mode for editing Java language source code.")
public class JavaMode extends GrammarCodeMode {

  /** Configuration for java-mode. */
  public static class JavaConfig extends Config.JavaDefs {

    @Var("If true, switch blocks are indented one step.")
    public final Config.Key<Boolean> indentSwitchBlock = key(Boolean.FALSE);
  }
  public static final JavaConfig CONFIG = new JavaConfig();

  public JavaMode (Env env) {
    super(env);
  }

  @Override public List<Config.Defs> configDefs () {
    return super.configDefs().cons(CONFIG);
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
