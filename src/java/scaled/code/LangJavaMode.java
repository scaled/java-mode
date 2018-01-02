//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code;

import scaled.*;
import scaled.project.*;
import scaled.util.Chars;

@Minor(name="lang-java",
       tags={ "java" }, stateTypes={ LangClient.class },
       desc="A minor mode which enhances Java mode when a Language Server is available.")
public class LangJavaMode extends MinorMode {

  private final LangClient langClient;

  public LangJavaMode (Env env) {
    super(env);
    langClient = LangClient.apply(buffer());
  }

  @Override public Key.Map keymap () {
    return super.keymap().
      bind("lang-import-type",       "C-c C-i");
      // bind("codex-method-override",   "C-c C-m C-o").
      // bind("codex-method-implement", "C-c C-m C-i");
  }

  @Fn("Queries for a type (completed by the language server) and adds an import for it.")
  public void langImportType () {
    window().mini().read("Type:", wordAt(view().point().get()), wspace().historyRing("lang-type"),
                         langClient.symbolCompleter(window())).onSuccess(sym -> {
      String fqName = sym.getContainerName() + "." + sym.getName();
      ImportUtil.insertImport(buffer(), fqName);
    });
  }

  /** Returns the "word" at the specified location in the buffer. */
  protected String wordAt (Loc loc) {
    return buffer().regionAt(loc.rowCol(), Chars.Word$.MODULE$).
      map(line -> line.asString()).mkString();
  }
}
