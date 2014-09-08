//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scala.collection.immutable.List;
import scala.collection.immutable.Nil$;
import scala.collection.immutable.Seq;

import scaled.*;
import scaled.code.CodeConfig;
import scaled.code.Commenter;
import scaled.code.Indenter;
import scaled.grammar.*;
import static scaled.code.CodeConfig.*;

@Major(name="properties",
       tags={ "code", "project", "properties" },
       pats={ ".*\\.properties", "package.scaled", "module.scaled" },
       desc="A major mode for editing Java properties files.")
public class PropertiesMode extends GrammarCodeMode {

  public PropertiesMode (Env env) { super(env); }

  @Override public List<Config.Defs> configDefs () {
    return SC.cons(PropertiesConfig.INSTANCE, super.configDefs());
  }

  @Override public Seq<Grammar> grammars () {
    return PropertiesConfig.INSTANCE.grammars;
  }
  @Override public List<Selector.Fn> effacers () {
    return PropertiesConfig.INSTANCE.effacers;
  }
  @Override public List<Selector.Fn> syntaxers () {
    return PropertiesConfig.INSTANCE.syntaxers;
  }

  @Override public List<Indenter> indenters () {
    return SC.nil();
  }
  @Override public Commenter commenter () {
    return new Commenter() {
      // TODO: ! is also a comment start character, sigh...
      @Override public String linePrefix () { return "#"; }
    };
  }
}
