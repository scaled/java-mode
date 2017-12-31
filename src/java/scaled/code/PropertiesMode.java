//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code;

import scaled.*;
import scaled.grammar.GrammarCodeMode;

@Major(name="properties",
       tags={ "code", "project", "properties" },
       pats={ ".*\\.properties", "package.scaled", "module.scaled" },
       desc="A major mode for editing Java properties files.")
public class PropertiesMode extends GrammarCodeMode {

  public PropertiesMode (Env env) { super(env); }

  @Override public String langScope () {
    return "source.java-props";
  }

  @Override public Commenter commenter () {
    return new Commenter() {
      @Override public String docOpen () { return "##"; }
      @Override public String docPrefix () { return "##"; }
      // TODO: ! is also a comment start character, sigh...
      @Override public String linePrefix () { return "#"; }
    };
  }
}
