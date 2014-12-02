//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import java.nio.file.Path;
import scala.Tuple2;
import scaled.*;
import scaled.code.CodeConfig;
import scaled.code.Commenter;
import scaled.code.Indenter;
import scaled.grammar.Grammar;
import scaled.grammar.GrammarCodeMode;
import scaled.grammar.GrammarConfig;
import scaled.grammar.Selector;
import static scaled.code.CodeConfig.*;

public class JavaConfig extends Config.Defs {

  public static final JavaConfig INSTANCE = new JavaConfig();

  @Var("If true, switch blocks are indented one step.")
  public final Config.Key<Boolean> indentSwitchBlock = key(Boolean.FALSE);

  // map TextMate grammar scopes to Scaled style definitions
  public final List<Selector.Fn> effacers = Std.list(
    // Java code colorizations
    GrammarConfig.effacer("comment.line", commentStyle()),
    GrammarConfig.effacer("comment.block", docStyle()),
    GrammarConfig.effacer("constant", constantStyle()),
    GrammarConfig.effacer("invalid", invalidStyle()),
    GrammarConfig.effacer("keyword", keywordStyle()),
    GrammarConfig.effacer("string", stringStyle()),

    GrammarConfig.effacer("storage.type.java", typeStyle()), // TODO: handle match-specificity (drop .java)
    GrammarConfig.effacer("storage.type.generic", typeStyle()),
    GrammarConfig.effacer("storage.type.primitive", typeStyle()),
    GrammarConfig.effacer("storage.type.object", typeStyle()), // meh, colors array []s same as type...
    GrammarConfig.effacer("storage.type.annotation", preprocessorStyle()),
    GrammarConfig.effacer("storage.modifier.java", keywordStyle()),
    GrammarConfig.effacer("storage.modifier.package", moduleStyle()),
    GrammarConfig.effacer("storage.modifier.extends", keywordStyle()),
    GrammarConfig.effacer("storage.modifier.implements", keywordStyle()),
    GrammarConfig.effacer("storage.modifier.import", typeStyle()),

    GrammarConfig.effacer("entity.name.type.class", typeStyle()),
    GrammarConfig.effacer("entity.other.inherited-class", typeStyle()),
    GrammarConfig.effacer("entity.name.function.java", functionStyle()),

    GrammarConfig.effacer("variable.language", keywordStyle()),
    GrammarConfig.effacer("variable.parameter", variableStyle()),
    GrammarConfig.effacer("variable.other.type", variableStyle()),

    // Javadoc colorizations
    GrammarConfig.effacer("markup.underline", preprocessorStyle()),

    // HTML in Javadoc colorizations
    GrammarConfig.effacer("entity.name.tag", constantStyle())
  );

  // map TextMate grammar scopes to Scaled syntax definitions
  public final List<Selector.Fn> syntaxers = Std.list(
    GrammarConfig.syntaxer("comment.line", Syntax.LineComment()),
    GrammarConfig.syntaxer("comment.block", Syntax.DocComment()),
    GrammarConfig.syntaxer("constant", Syntax.OtherLiteral()),
    GrammarConfig.syntaxer("string", Syntax.StringLiteral())
  );

  public final PropertyV<Grammar.Set> grammars = resource(
    Std.seq("HTML.ndf", "JavaDoc.ndf", "Java.ndf"), Grammar.parseNDFs());
}
