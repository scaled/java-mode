//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import codex.model.*;
import java.util.Iterator;
import java.util.List;
import scaled.*;
import scaled.project.*;
import scaled.util.Chars;
import scaled.util.Errors;

@Minor(name="codex-java",
       tags={ "java" },
       desc="A minor mode which enhances Java mode when a Codex is available.")
public class CodexJavaMode extends MinorMode {

  public final ProjectSpace pspace;
  public final Project project;
  public final JavaMode javaMode;

  /** The most recent index for the buffer's source file, if any. */
  public final OptValue<SourceIndex> index = OptValue.create();

  public CodexJavaMode (Env env, JavaMode javaMode) {
    super(env);
    pspace = ProjectSpace.apply(window().workspace());
    project = Project.apply(buffer());
    note(project.indexer().indexed().onValue(Std.fnU(idx -> {
      if (idx.store().equals(buffer().store())) index.update(idx);
    })));
    this.javaMode = javaMode; // the major mode we work with
  }

  @Override public Key.Map keymap () {
    return super.keymap().
      bind("codex-method-override", "C-c C-m C-o");
    // TODO: codex-method-go C-c C-m C-g
  }

  @Fn("Queries for a method in the enclosing class and inserts an override definition for it. " +
      "If the chosen method is already overridden, this navigates to the method instead.")
  public void codexMethodOverride () {
    if (!index.isDefined()) throw Errors.feedback("No Codex index available for this file.");

    Buffer buffer = buffer();
    Option<Def> enclO = index.get().encloser(buffer.offset(view().point().get().rowCol()));
    if (!enclO.isDefined()) throw Errors.feedback("Could not find enclosing type.");

    Def encl = enclO.get();
    if (encl.kind != Kind.TYPE) throw Errors.feedback(
      "The point must be inside a class declaration.");

    List<Def> meths = OO.resolveMethods(pspace.codex().stores(project), encl, this::isOverridable);
    // remove methods defined directly in encl
    for (Iterator<Def> iter = meths.iterator(); iter.hasNext(); ) {
      if (encl.id.equals(iter.next().outerId)) iter.remove();
    }

    // wow, this is unfortunate
    Completer<Def> comp = Completer.from(scaled.Iterable.view(meths),
                                         Std.<Def,String>fn(def -> def.globalRef().id));
    window().mini().read("Method:", "", methodHistory, comp).onSuccess(Std.fnU(meth -> {
      Sig sig = meth.sig().orElseThrow(() -> Errors.feedback("Signature unavailable for " + meth));
      long loc = view().point().get().atCol(0);

      // if the previous line is non-blank, insert a blank line
      long prev = view().point().get().prevStart();
      if (buffer.line(prev).indexOf(Chars.isNotWhitespace()) != -1) loc = buffer.split(loc);

      // now insert the method code
      String pre = spaces(javaMode.computeIndent(Loc.row$extension(loc)));
      loc = buffer.insertLine(loc, Line.apply(pre + "@Override"));
      loc = buffer.insertLine(loc, Line.apply(pre + accessPrefix(meth.access) + sig.text + " {"));
      String inpre = spaces(javaMode.computeIndent(Loc.row$extension(loc)));
      long inside = Loc.atCol$extension(loc, inpre.length());
      loc = buffer.insertLine(loc, Line.apply(inpre));
      loc = buffer.insertLine(loc, Line.apply(pre + "}"));

      // if the following line is non-blank, insert a blank line
      if (buffer.line(loc).indexOf(Chars.isNotWhitespace()) != -1) loc = buffer.split(loc);

      // leave the point inside the newly created method, ready for typing!
      view().point().update(new Loc(inside));
    }));
  }

  private boolean isOverridable (Def def) {
    if (def.access != Access.PUBLIC && def.access != Access.PROTECTED) return false;
    if (def.flavor == Flavor.STATIC_METHOD) return false;
    return true;
  }

  private String accessPrefix (Access access) {
    switch (access) {
    case PUBLIC: return "public ";
    case PROTECTED: return "protected ";
    case PRIVATE: return "private ";
    default: return "";
    }
  }

  // do I really not have a method somewhere that does this?
  private String spaces (int count) {
    StringBuilder sb = new StringBuilder();
    for (int ii = 0; ii < count; ii++) sb.append(' ');
    return sb.toString();
  }

  private Ring methodHistory = new Ring(20);
}
