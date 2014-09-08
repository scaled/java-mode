//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scala.Tuple2;
import scala.collection.immutable.List;
import scala.collection.immutable.Seq;
import scaled.*;
import scaled.code.CodeConfig;
import scaled.code.Commenter;
import scaled.code.Indenter;
import scaled.grammar.Grammar;
import scaled.grammar.GrammarCodeMode;
import scaled.grammar.GrammarConfig;
import scaled.grammar.Selector;
import scaled.util.Chars;
import scaled.util.Paragrapher;
import static scaled.code.CodeConfig.*;

class JavaConfig extends Config.Defs {

  public static final JavaConfig INSTANCE = new JavaConfig();

  // map TextMate grammar scopes to Scaled style definitions
  public final List<Selector.Fn> effacers = SC.list(
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
  public final List<Selector.Fn> syntaxers = SC.list(
    GrammarConfig.syntaxer("comment.line", Syntax.LineComment()),
    GrammarConfig.syntaxer("comment.block", Syntax.DocComment()),
    GrammarConfig.syntaxer("constant", Syntax.OtherLiteral()),
    GrammarConfig.syntaxer("string", Syntax.StringLiteral())
  );

  public Grammar htmlGrammar () {
    return Grammar.parseNDF(stream("HTML.ndf"));
  }
  public Grammar javaDocGrammar () {
    return Grammar.parseNDF(stream("JavaDoc.ndf"));
  }
  public Grammar javaGrammar () {
    return Grammar.parseNDF(stream("Java.ndf"));
  }
  public final Seq<Grammar> grammars = SC.list(htmlGrammar(), javaDocGrammar(), javaGrammar());

  private JavaConfig () {
    super(false);
  }
}

@Major(name="java",
       tags={ "code", "project", "java" },
       pats=".*\\.java",
       ints="java",
       desc="A major mode for editing Java language source code.")
class JavaMode extends GrammarCodeMode {

  public JavaMode (Env env) {
    super(env);
  }

  @Override public List<Config.Defs> configDefs () {
    return SC.cons(JavaConfig.INSTANCE, super.configDefs());
  }

  @Override public Seq<Tuple2<String,String>> keymap () {
    return SC.concat(super.keymap(), SC.<Tuple2<String,String>>list(
      Tuple2.<String,String>apply("ENTER",   "electric-newline"),
      Tuple2.<String,String>apply("S-ENTER", "electric-newline")));
  }

  @Override public Seq<Grammar> grammars () {
    return JavaConfig.INSTANCE.grammars;
  }
  @Override public List<Selector.Fn> effacers () {
    return JavaConfig.INSTANCE.effacers;
  }
  @Override public List<Selector.Fn> syntaxers () {
    return JavaConfig.INSTANCE.syntaxers;
  }

  // TODO: val
  @Override public List<Indenter> indenters () {
    return List.apply(
      //    new Indenter.PairAnchorAlign(config, buffer) {
      //      protected val anchorM = Matcher.regexp("\\bfor\\b")
      //      protected val secondM = Matcher.regexp("yield\\b")
      //    },
      //    new Indenter.TryCatchAlign(config, buffer),
      //    new Indenter.TryFinallyAlign(config, buffer),
      //    new Indenter.IfElseIfElseAlign(config, buffer),
      //    new ScalaIndenter.ValueExprBody(config, buffer),
      new JavaIndenter.ExtendsImpls(indentCtx),
      new JavaIndenter.Javadoc(indentCtx),
      //    new Indenter.OneLinerWithArgs(config, buffer, blocker, Set("if", "while", "for")),
      //    new Indenter.OneLinerNoArgs(config, buffer, Set("else", "do", "try", "finally")),
      new JavaIndenter.ContinuedStmt(indentCtx),
      new Indenter.ByBlock(indentCtx) {
        @Override public int readBlockIndent (Loc pos) {
          return JavaIndenter.readBlockIndent(buffer, pos);
        }
      }
    );
  }

  // TODO: val
  @Override public JavaCommenter commenter () {
    return new JavaCommenter();
  }

  @Override public int detectIndent () {
    return new Indenter.Detecter(3) {
      private Matcher pppM = Matcher.regexp("(public|protected|private)");
      // if the line starts with 'public/protected/private' then it is meaningful
      public int consider (LineV line, int start) {
        return line.matches(pppM, start) ? 1 : 0;
      }
    }.detectIndent(buffer);
  }

  //
  // FNs

  @Fn("Inserts a newline, then indents the subsequent line. Handles other \"smart\" cases such " +
      "as: If newline is inserted in the middle of a Javadoc comment, the next line is prepended " +
      "with * before indenting. TODO: other smarts.")
  public void electricNewline () {
    // shenanigans to determine whether we should auto-insert the doc prefix (* )
    boolean inDoc = commenter.inDoc(buffer, view.point());
    newline();
    Loc np = view.point();
    if (inDoc && buffer.charAt(np) != '*') view.point.update(commenter.insertDocPre(buffer, np));
    reindentAtPoint();
  }

  // TODO: more things!
}
