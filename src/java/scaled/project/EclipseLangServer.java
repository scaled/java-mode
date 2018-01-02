//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Augments the standard {@code LanguageServer} interface with Eclipse JDT LS extensions.
 */
public interface EclipseLangServer extends LanguageServer {

  @JsonSegment("java")
  public interface JavaExtensions {

    public static enum BuildWorkspaceStatus {
      FAILED, SUCCEED, WITH_ERROR, CANCELLED,
    }

    @JsonRequest
    CompletableFuture<String> classFileContents (TextDocumentIdentifier documentUri);

    @JsonNotification
    void projectConfigurationUpdate (TextDocumentIdentifier documentUri);

    @JsonRequest
    CompletableFuture<BuildWorkspaceStatus> buildWorkspace (boolean forceReBuild);
  }

  /**
   * Extensions provided by the Eclipse Language Server.
   */
  @JsonDelegate
  JavaExtensions getJavaExtensions ();
}
