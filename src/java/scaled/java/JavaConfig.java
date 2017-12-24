//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scaled.*;

public class JavaConfig extends Config.JavaDefs {

  public static final JavaConfig INSTANCE = new JavaConfig();

  @Var("If true, switch blocks are indented one step.")
  public final Config.Key<Boolean> indentSwitchBlock = key(Boolean.FALSE);
}
