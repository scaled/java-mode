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

  /** One or more directories or jar files that contain all classes provided by this project. */
  public abstract SeqV<Path> classes ();
  /** The directories or jar files needed to compile this project. Note: this <em>should not</em>
    * contain the jar files or directories in {@link #classes}. */
  public abstract SeqV<Path> buildClasspath ();
  /** The directories or jar files needed to run code in this project. Note: this <em>should</em>
    * contain the jar files and directories in {@link #classes}. */
  public abstract SeqV<Path> execClasspath ();

  /** Adds any standard (Java) testing components to this project. This should be called after the
    * [[buildClasspath]] has been updated. */
  public void addTesters () {
    JUnitTester.addComponent(project, this);
    TestNGTester.addComponent(project, this);
  }

  @Override public void describeSelf (BufferBuilder bb) {
    bb.addSubHeader("Java Info");
    bb.addSection("Classes:");
    classes().foreach(p -> bb.add(p.toString()));
    bb.addSection("Build classpath:");
    buildClasspath().foreach(p -> bb.add(p.toString()));
    bb.addSection("Exec classpath:");
    execClasspath().foreach(p -> bb.add(p.toString()));
  }

  @Override public void close () {} // nada
}
