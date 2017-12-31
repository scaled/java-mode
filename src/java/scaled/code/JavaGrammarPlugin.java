//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code;

import scaled.*;
import scaled.grammar.*;
import static scaled.code.CodeConfig.*;

@Plugin(tag="textmate-grammar")
public class JavaGrammarPlugin extends GrammarPlugin {

  public Map<String, String> grammars () {
    return Map.<String, String>builder().
      put("source.java", "Java.ndf").
      put("text.html.javadoc", "JavaDoc.ndf").
      build();
  }

  public List<Selector.Fn> effacers () {
    return Std.list(
      // Java code colorizations
      effacer("comment.line", commentStyle()),
      effacer("comment.block", docStyle()),
      effacer("constant", constantStyle()),
      effacer("invalid", invalidStyle()),
      effacer("keyword", keywordStyle()),
      effacer("string", stringStyle()),

      effacer("storage.type.java", typeStyle()), // TODO: handle match-specificity (drop .java)
      effacer("storage.type.generic", typeStyle()),
      effacer("storage.type.primitive", typeStyle()),
      effacer("storage.type.object", typeStyle()), // meh, colors array []s same as type...
      effacer("storage.type.annotation", preprocessorStyle()),
      effacer("storage.modifier.java", keywordStyle()),
      effacer("storage.modifier.package", moduleStyle()),
      effacer("storage.modifier.extends", keywordStyle()),
      effacer("storage.modifier.implements", keywordStyle()),
      effacer("storage.modifier.import", typeStyle()),

      effacer("entity.name.type.class", typeStyle()),
      effacer("entity.other.inherited-class", typeStyle()),
      effacer("entity.name.function.java", functionStyle()),

      effacer("variable.language", keywordStyle()),
      effacer("variable", variableStyle()),

      // Javadoc colorizations
      effacer("markup.underline", preprocessorStyle()),
      effacer("markup.raw.code", preprocessorStyle()),

      // HTML in Javadoc colorizations
      effacer("entity.name.tag", constantStyle())
    );
  }

  public List<Selector.Fn> syntaxers () {
    return Std.list(
      syntaxer("comment.line", Syntax.LineComment()),
      syntaxer("comment.block", Syntax.DocComment()),
      syntaxer("constant", Syntax.OtherLiteral()),
      syntaxer("string", Syntax.StringLiteral())
    );
  }
}
