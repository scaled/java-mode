//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.nio.file.Path;
import scaled.*;
import scaled.pacman.JDK;

@Plugin(tag="project-root")
public class JDKRootPlugin extends RootPlugin {

  /** Returns the JDK rooted in {@code root} if any. */
  public static Option<JDK> find (Path root) {
    return Seq.view(JDK.jdks()).find(jdk -> jdk.root().equals(root));
  }

  @Override public int checkRoot (Path root) {
    return find(root).isDefined() ? 1 : -1;
  }

  @Override public Option<Project.Root> apply (Project.Id id) {
    if (id instanceof Project.PlatformId) {
      String platform = ((Project.PlatformId)id).platform();
      String version = ((Project.PlatformId)id).version();
      if (platform.equals(Project.JavaPlatform())) {
        return Seq.view(JDK.jdks()).
          find(jdk -> jdk.majorVersion().equals(version)).
          map(jdk -> new Project.Root(jdk.root(), ""));
      }
    }
    return Option.none();
  }
}
