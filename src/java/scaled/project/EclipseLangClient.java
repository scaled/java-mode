//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

import scaled.*;
import scaled.util.Errors;

public class EclipseLangClient extends LangClient {

  public static String PROJECT_FILE = ".project";

  @Plugin(tag="langserver")
  public static class EclipseLangPlugin extends LangPlugin {
    @Override public Set<String> suffs (Project.Root root) {
      return Std.set("java", "scala"); // TODO: others?
    }

    @Override public boolean canActivate (Project.Root root) {
      return Files.exists(root.path().resolve(PROJECT_FILE));
    }

    @Override public Future<LangClient> createClient (Project proj) {
      return JDTLS.resolve(proj.metaSvc(), proj.root()).map(
        jdtls -> new EclipseLangClient(proj.metaSvc(), proj.root().path(), serverCmd(proj, jdtls)));
    }
  }

  /** Constructs the command line to invoke the JDT LS daemon. */
  private static Seq<String> serverCmd (Project proj, Path jdtls) {
    String osName = System.getProperty("os.name");
    String configOS = "mac";
    if (osName.equalsIgnoreCase("linux")) configOS = "linux";
    if (osName.startsWith("Windows")) configOS = "win";

    Path launcherJar = JDTLS.launcherJar(jdtls).getOrElse(() -> {
      throw Errors.feedback("Can't find launcher jar in " + jdtls);
    });
    Path configDir = jdtls.resolve("config_" + configOS);
    Path dataDir = proj.metaFile("eclipse-jdt-ls");

    return Std.seq("java",
                   "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                   "-Dosgi.bundles.defaultStartLevel=4",
                   "-Declipse.product=org.eclipse.jdt.ls.core.product",
                   "-noverify",
                   "-Xmx1G",
                   "-XX:+UseG1GC",
                   "-XX:+UseStringDeduplication",
                   "-jar", launcherJar.toString(),
                   "-configuration", configDir.toString(),
                   "-data", dataDir.toString());
  }

  public EclipseLangClient (MetaService metaSvc, Path root, Seq<String> cmd) {
    super(metaSvc, root, cmd);
  }

  @Override public String name () { return "Eclipse"; }

  public static class StatusReport {
    String message;
    String type;

    public StatusReport (String message, String type) {
      this.message = message;
      this.type = type;
    }
  }

  @Override public Class<?> langServerClass () {
    return EclipseLangServer.class;
  }

  /** Fetches the contents for a "synthetic" location, one hosted by the language server. */
  @Override public Future<String> fetchContents (Location loc, Executor exec) {
    try {
      TextDocumentIdentifier docId = new TextDocumentIdentifier(loc.getUri());
      return LSP.adapt(getJavaExtensions().classFileContents(docId), exec);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override public String modeFor (Location loc) { return "java"; }

  private EclipseLangServer.JavaExtensions getJavaExtensions () {
    return ((EclipseLangServer)server()).getJavaExtensions();
  }

  /**
   * Notifies us of the JDT LS status. (Eclipse JDT LS extension)
   */
  @JsonNotification("language/status")
  public void statusNotification(StatusReport report) {
    messages().emit(name() + ": " + report.message);
  }

  // TODO: tweak the stuff we get back from JDT-LS to make it nicer
}
