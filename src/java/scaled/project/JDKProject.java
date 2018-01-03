//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import codex.extract.SourceSet;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import scaled.*;
import scaled.pacman.JDK;

/** A special project for use when another project depends on the JDK. This will eventually handle
  * locating JDKs on different platforms and turning the contents of src.zip into a Codex, and
  * whatever other special casery arises.
  */
public class JDKProject extends Project {

  @Plugin(tag="project-finder")
  public static class FinderPlugin extends ProjectFinderPlugin {
    public FinderPlugin () {
      super("jdk", true, JDKProject.class);
    }

    @Override public int checkRoot (Path root) {
      return (Seq.view(JDK.jdks()).exists(jdk -> jdk.root().equals(root))) ? 1 : -1;
    }

    @Override public Option<Project.Seed> apply (Id id) {
      if (id instanceof PlatformId) {
        String platform = ((PlatformId)id).platform();
        String version = ((PlatformId)id).version();
        if (platform.equals(Project.JavaPlatform())) {
          return Seq.view(JDK.jdks()).find(jdk -> jdk.majorVersion().equals(version)).
            map(jdk -> seed(new Project.Root(jdk.root(), ""), List.apply(jdk)));
        }
      }
      return Option.none();
    }

    @Override public List<Object> injectArgs (Project.Root root) {
      JDK jdk = Seq.view(JDK.jdks()).find(j -> j.root().equals(root.path())).getOrElse(() -> {
        throw new IllegalArgumentException("Invalid JDK root: " + root);
      });
      return Std.list(jdk);
    }
  }

  public final JDK jdk;

  public JDKProject (ProjectSpace ps, JDK jdk) {
    super(ps, new Project.Root(jdk.root(), ""));
    this.jdk = jdk;
  }

  @Override public Future<Meta> computeMeta (Meta oldMeta) {
    // add a filer component for our zip file(s)
    addComponent(Filer.class, new ZipFiler(Std.seq(jdk.root())));

    // add a sources component that groks our zip-based source files
    addComponent(Sources.class, new Sources(Std.seq(jdk.root())) {
      @Override public Map<String, SourceSet> summarize () {
        return Map.<String, SourceSet>builder().put(
          "java", new SourceSet.Archive(jdk.root(), entry -> entry.getName().startsWith("java"))
        ).build();
      }
    });

    return Future.success(new Meta(
      "jdk-" + jdk.version(),
      Std.seq(new PlatformId(Project.JavaPlatform(), jdk.majorVersion()))
    ));
  }
}
