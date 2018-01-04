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
      return new JavaMeta(Std.seq(), Paths.get("unused"), Std.seq(), Std.seq());
    }

    public JavaMeta read (Map<String,SeqV<String>> in) {
      return new JavaMeta(
        in.apply("classes").map(p -> Paths.get(p)),
        Paths.get(in.apply("dirs").get(0)),
        in.apply("buildClasspath").map(p -> Paths.get(p)),
        in.apply("execClasspath").map(p -> Paths.get(p))
      );
    }

    public void write (ConfigFile.WriteMap out, JavaMeta meta) {
      out.write("classes", meta.classes.map(Path::toString));
      out.write("dirs", Std.seq(meta.outputDir.toString()));
      out.write("buildClasspath", meta.buildClasspath.map(Path::toString));
      out.write("execClasspath", meta.execClasspath.map(Path::toString));
    }
  };

  public final SeqV<Path> classes;
  public final Path outputDir;
  public final SeqV<Path> buildClasspath;
  public final SeqV<Path> execClasspath;

  public JavaMeta (SeqV<Path> classes, Path outputDir,
                   SeqV<Path> buildClasspath, SeqV<Path> execClasspath) {
    this.classes = classes;
    this.outputDir = outputDir;
    this.buildClasspath = buildClasspath;
    this.execClasspath = execClasspath;
  }

  @Override public int hashCode () {
    return classes.hashCode() ^ outputDir.hashCode() ^ buildClasspath.hashCode() ^
      execClasspath.hashCode();
  }

  @Override public boolean equals (Object other) {
    if (other instanceof JavaMeta) {
      JavaMeta ometa = (JavaMeta)other;
      return (Objects.equals(classes, ometa.classes) &&
              Objects.equals(outputDir, ometa.outputDir) &&
              Objects.equals(buildClasspath, ometa.buildClasspath) &&
              Objects.equals(execClasspath, ometa.execClasspath));
    } else return false;
  }
}
