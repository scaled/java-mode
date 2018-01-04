//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.nio.file.Path;
import scaled.*;
import scaled.util.BufferBuilder;

/** A component added to projects that have Java (or JVM-language) code. */
public class JavaComponent extends Project.Component {

  /** The project of which we're a component. */
  public final Project project;

  /** Tracks Java-specific project metadata. */
  public final Value<JavaMeta> javaMetaV;

  public JavaComponent (Project project) {
    this.project = project;
    this.javaMetaV = project.metaValue("java-meta", JavaMeta.META);
  }

  public SeqV<Path> classes () { return javaMetaV.get().classes; }
  public Path outputDir () { return javaMetaV.get().outputDir; }
  public SeqV<Path> buildClasspath () { return javaMetaV.get().buildClasspath; }
  public SeqV<Path> execClasspath () { return javaMetaV.get().execClasspath; }

  /** Adds any standard (Java) testing components to this project. This should be called after the
    * [[buildClasspath]] has been updated. */
  public void addTesters () {
    JUnitTester.addComponent(project, this);
    TestNGTester.addComponent(project, this);
  }

  @Override public void describeSelf (BufferBuilder bb) {
    bb.addSubHeader("Java Info");
    bb.addSection("Output dirs:");
    bb.addKeysValues(Std.seq(Std.pair("compile: ", outputDir().toString())));
    bb.addSection("Build classpath:");
    buildClasspath().foreach(p -> bb.add(p.toString()));
    bb.addSection("Exec classpath:");
    execClasspath().foreach(p -> bb.add(p.toString()));
  }

  @Override public void close () {} // nada
}
