//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scaled.*;
import scaled.code.CodeConfig;
import scaled.code.Commenter;
import scaled.grammar.*;
import static scaled.code.CodeConfig.*;

public class PropertiesConfig extends Config.Defs {

  public static final PropertiesConfig INSTANCE = new PropertiesConfig();

  // map TextMate grammar scopes to Scaled style definitions
  public final List<Selector.Fn> effacers = Std.list(
    GrammarConfig.effacer("comment.line", commentStyle()),
    GrammarConfig.effacer("keyword", keywordStyle())
  );

  // map TextMate grammar scopes to Scaled syntax definitions
  public final List<Selector.Fn> syntaxers = Std.list(
    GrammarConfig.syntaxer("comment.line", Syntax.LineComment())
  );

  public Grammar propsGrammar () {
    return Grammar.parseNDF(stream("JavaProperties.ndf"));
  }
  public final Seq<Grammar> grammars = Std.seq(propsGrammar());

  private PropertiesConfig () {
    super(false);
  }
}