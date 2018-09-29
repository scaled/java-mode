//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.nio.file.Path;
import scaled.*;
import scaled.util.BufferBuilder;

/** A Java component that stores its metadata persistently. */
public class JavaMetaComponent extends JavaComponent {

  /** Tracks Java-specific project metadata. */
  public final Value<JavaMeta> javaMetaV;

  public JavaMetaComponent (Project project) {
    super(project);
    this.javaMetaV = project.metaValue("java-meta", JavaMeta.META);
  }

  @Override public SeqV<Path> classes () { return javaMetaV.get().classes; }
  @Override public Path targetDir () { return javaMetaV.get().targetDir; }
  @Override public Path outputDir () { return javaMetaV.get().outputDir; }
  @Override public SeqV<Path> buildClasspath () { return javaMetaV.get().buildClasspath; }
  @Override public SeqV<Path> execClasspath () { return javaMetaV.get().execClasspath; }
}
