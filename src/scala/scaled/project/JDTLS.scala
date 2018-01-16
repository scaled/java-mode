//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.project

import java.net.URL
import java.nio.file.{Files, Path, Paths}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import scaled._
import scaled.pacman.Filez
import scaled.util.{Errors, Fetcher}

object JDTLS {

  // from whence we download the Eclipse JDT language server
  val JdtFile = "jdt-language-server-latest.tar.gz"
  val JdtUrl = new URL(s"http://download.eclipse.org/jdtls/snapshots/$JdtFile")

  /** Downloads and unpacks the JDTLS, if needed. */
  def resolve (metaSvc :MetaService, root :Project.Root) :Future[Path] = {
    val pkgSvc = metaSvc.service[PackageService]
    val selfSource = "git:https://github.com/scaled/java-mode.git"
    val selfRoot = pkgSvc.installDir(selfSource)
    val jdtlsDir = selfRoot.resolve("eclipse-jdt-ls")
    if (Files.exists(jdtlsDir)) Future.success(jdtlsDir)
    else {
      val jdtPath = selfRoot.resolve(JdtFile)
      Fetcher.fetch(metaSvc.exec, JdtUrl, jdtPath, pct => {
        metaSvc.log.log(s"Downloading $JdtFile: $pct%")
      }).map(targz => {
        metaSvc.log.log(s"Unpacking $JdtFile...")
        val jdtlsTmp = Files.createTempDirectory(selfRoot, "jdtls")
        try {
          untargz(targz, jdtlsTmp)
          Filez.deleteAll(jdtlsDir)
          Files.move(jdtlsTmp, jdtlsDir)
          jdtlsDir
        } finally {
          Files.deleteIfExists(targz)
          Filez.deleteAll(jdtlsTmp)
        }
      })
    }
  }

  /** Determines the path to the launcher jar in a JDTLS installation. */
  def launcherJar (jdtls :Path) :Option[Path] = {
    Files.list(jdtls.resolve("plugins")).filter(path => {
      val name = path.getFileName.toString
      name.startsWith("org.eclipse.equinox.launcher_") && name.endsWith(".jar")
    }).findFirst.toOpt
  }

  /** Unpacks the .tar.gz file at `path` into the `into` directory. */
  def untargz (path :Path, into :Path) {
    using(new GzipCompressorInputStream(Files.newInputStream(path))) { gzin =>
      val tin = new TarArchiveInputStream(gzin)
      var entry = tin.getNextTarEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val file = into.resolve(Paths.get(entry.getName))
          Files.createDirectories(file.getParent)
          Files.copy(tin, file)
        }
        entry = tin.getNextTarEntry
      }
    }
  }
}
