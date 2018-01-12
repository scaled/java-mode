//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import scaled.*;

/** Defines additional persistent data for a JVM language project. */
public class JavaMeta {

  /** Handles reading and writing [[JavaMeta]]s. */
  public static Project.MetaMeta<JavaMeta> META = new Project.MetaMeta<JavaMeta>() {
    public JavaMeta zero (Project project) {
      Path unused = Paths.get("unused");
      return new JavaMeta(Std.seq(), unused, unused, Std.seq(), Std.seq());
    }

    public JavaMeta read (Map<String,SeqV<String>> in) {
      Seq<Path> dirs = in.apply("dirs").map(Paths::get);
      return new JavaMeta(
        in.apply("classes").map(Paths::get),
        dirs.get(0),
        // TEMP: support old serialized files that lacked "target" directory
        dirs.get(Math.min(dirs.size(), 1)),
        in.apply("buildClasspath").map(Paths::get),
        in.apply("execClasspath").map(Paths::get)
      );
    }

    public void write (ConfigFile.WriteMap out, JavaMeta meta) {
      out.write("classes", meta.classes.map(Path::toString));
      out.write("dirs", Std.seq(meta.targetDir.toString(), meta.outputDir.toString()));
      out.write("buildClasspath", meta.buildClasspath.map(Path::toString));
      out.write("execClasspath", meta.execClasspath.map(Path::toString));
    }
  };

  public final SeqV<Path> classes;
  public final Path targetDir;
  public final Path outputDir;
  public final SeqV<Path> buildClasspath;
  public final SeqV<Path> execClasspath;

  public JavaMeta (SeqV<Path> classes, Path targetDir, Path outputDir,
                   SeqV<Path> buildClasspath, SeqV<Path> execClasspath) {
    this.classes = classes;
    this.targetDir = targetDir;
    this.outputDir = outputDir;
    this.buildClasspath = buildClasspath;
    this.execClasspath = execClasspath;
  }

  @Override public int hashCode () {
    return classes.hashCode() ^ targetDir.hashCode() ^ outputDir.hashCode() ^
      buildClasspath.hashCode() ^ execClasspath.hashCode();
  }

  @Override public boolean equals (Object other) {
    if (other instanceof JavaMeta) {
      JavaMeta ometa = (JavaMeta)other;
      return (Objects.equals(classes, ometa.classes) &&
              Objects.equals(targetDir, ometa.targetDir) &&
              Objects.equals(outputDir, ometa.outputDir) &&
              Objects.equals(buildClasspath, ometa.buildClasspath) &&
              Objects.equals(execClasspath, ometa.execClasspath));
    } else return false;
  }
}
