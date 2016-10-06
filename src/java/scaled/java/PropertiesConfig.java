//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import java.nio.file.Path;
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
    GrammarConfig.effacer("comment.doc", docStyle()),
    GrammarConfig.effacer("keyword", keywordStyle())
  );

  // map TextMate grammar scopes to Scaled syntax definitions
  public final List<Selector.Fn> syntaxers = Std.list(
    GrammarConfig.syntaxer("comment.line", Syntax.LineComment()),
    GrammarConfig.syntaxer("comment.doc", Syntax.DocComment())
  );

  public final PropertyV<Grammar.Set> grammars = resource(
    "JavaProperties.ndf", Grammar.parseNDFs());
}
