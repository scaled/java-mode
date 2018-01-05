//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import scaled.*;

@Plugin(tag="project-root")
public class EclipseRootPlugin extends RootPlugin.File {

  public EclipseRootPlugin () {
    super(".project");
  }
}
