//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code;

import scaled.*;
import scaled.grammar.*;
import static scaled.code.CodeConfig.*;

@Plugin(tag="textmate-grammar")
public class PropertiesGrammarPlugin extends GrammarPlugin {

  public Map<String, String> grammars () {
    return Map.<String, String>builder().
      put("source.java-props", "JavaProperties.ndf").
      build();
  }

  public List<Selector.Fn> effacers () {
    return Std.list(
      effacer("comment.line", commentStyle()),
      effacer("comment.doc", docStyle()),
      effacer("keyword", keywordStyle())
    );
  }

  public List<Selector.Fn> syntaxers () {
    return Std.list(
      syntaxer("comment.line", Syntax.LineComment()),
      syntaxer("comment.doc", Syntax.DocComment())
    );
  }
}
