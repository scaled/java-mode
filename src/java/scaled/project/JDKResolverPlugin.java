//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import codex.extract.SourceSet;
import java.nio.file.Path;
import java.nio.file.Paths;
import scaled.*;
import scaled.pacman.JDK;

@Plugin(tag="project-resolver")
public class JDKResolverPlugin extends ResolverPlugin {

  @Override public void addComponents (Project project) {
    Path root = project.root().path();
    JDKRootPlugin.find(root).ifDefined(jdk -> {
      // add a filer component for the src.zip file
      project.addComponent(Filer.class, new ZipFiler(Std.seq(jdk.root())));

      // add a sources component that groks our zip-based source files
      project.addComponent(Sources.class, new Sources(Std.seq(jdk.root())) {
        @Override public Map<String, SourceSet> summarize () {
          return Map.<String, SourceSet>builder().put(
            "java", new SourceSet.Archive(jdk.root(), entry -> entry.getName().startsWith("java"))
          ).build();
        }
      });

      project.addComponent(JavaComponent.class, new JavaComponent(project) {
        public SeqV<Path> classes () { return Std.seq(); }
        public Path targetDir () { return Paths.get("unused"); }
        public Path outputDir () { return Paths.get("unused"); }
        public SeqV<Path> buildClasspath () { return Std.seq(); }
        public SeqV<Path> execClasspath () { return Std.seq(); }
      });

      project.metaV().update(new Project.Meta(
        "jdk-" + jdk.version(),
        Std.set(new Project.PlatformId(Project.JavaPlatform(), jdk.majorVersion())),
        Option.none()
      ));

      return null;
    });
  }
}
