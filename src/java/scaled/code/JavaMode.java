//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code;

import codex.model.Kind;
import scaled.*;
import scaled.grammar.GrammarCodeMode;
import scaled.project.Analyzer;
import scaled.util.Chars;

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

  @Override public Key.Map keymap () {
    return super.keymap().
      bind("import-type",       "C-c C-i");
      // bind("method-override",   "C-c C-m C-o").
      // bind("method-implement", "C-c C-m C-i");
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

  @Fn("Queries for a type (completed by the analyzer) and adds an import for it.")
  public void importType () {
    Analyzer analyzer = Analyzer.apply(buffer());
    window().mini().read("Type:", wordAt(view().point().get()), wspace().historyRing("java-type"),
                         analyzer.symbolCompleter(Option.some(Kind.TYPE))).onSuccess(sym -> {
      ImportUtil.insertImport(buffer(), analyzer.fqName(sym));
    });
  }

  /** Returns the "word" at the specified location in the buffer. */
  protected String wordAt (Loc loc) {
    return buffer().regionAt(loc.rowCol(), Chars.Word$.MODULE$).
      map(line -> line.asString()).mkString();
  }
}
