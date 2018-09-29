//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.nio.file.Path;
import scaled.*;
import scaled.util.BufferBuilder;

/** A component added to projects that have Java (or JVM-language) code. */
public abstract class JavaComponent extends Project.Component {

  /** The project of which we're a component. */
  public final Project project;

  public JavaComponent (Project project) {
    this.project = project;
  }

  public abstract SeqV<Path> classes ();
  public abstract Path targetDir ();
  public abstract Path outputDir ();
  public abstract SeqV<Path> buildClasspath ();
  public abstract SeqV<Path> execClasspath ();

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
