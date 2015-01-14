//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import java.util.Comparator;
import scaled.*;
import scaled.util.Errors;

public class ImportUtil {

  public static final Matcher importM = Matcher.regexp("^import ");
  public static final Matcher packageM = Matcher.regexp("^package ");
  public static final Matcher firstDefM = Matcher.regexp("(class|interface|@interface|enum)");

  public static class ImportGroup {
    public final int firstRow;
    public final Seq<String> lines;
    public final String longestPrefix;

    public ImportGroup (int firstRow, Seq<String> lines) {
      this.firstRow = firstRow;
      this.lines = lines;
      this.longestPrefix = Completer.longestPrefix(lines);
    }

    public int matchLength (String newImport) {
      return Completer.sharedPrefix(longestPrefix, newImport).length();
    }

    public int lastRow () { return firstRow + lines.size(); }

    public void insert (Buffer buffer, String newImport, Line newLine) {
      int newRow = firstRow + lines.size();
      for (int ii = 0, ll = lines.size(); ii < ll; ii++) {
        String line = lines.get(ii);
        if (newImport.compareTo(line) > 0) continue;
        newRow = firstRow+ii;
        break;
      }
      buffer.insert(Loc.apply(newRow, 0), Std.seq(newLine, Line.Empty()));
    }

    @Override public String toString () {
      return "[row=" + firstRow + ", longPre=" + longestPrefix + ", lines=" + lines + "]";
    }
  }

  /**
   * Finds the appropriate line on which to insert an import for {@code fqName} and inserts it. If
   * the imports are grouped by whitespace, this function first searches for the most appropriate
   * group, then inserts the import alphabetically therein. If no appropriate group can be found, a
   * new group is created. If no imports exist at all, the import is added after the package
   * statement (or the start of the buffer if no package statement can be found).
   */
  public static void insertImport (Buffer buffer, String fqName) {
    String newImport = "import " + fqName + ";";
    Line newLine = Line.apply(newImport);

    // first figure out where we're going to stop looking (at the first class, etc.)
    long firstDef = buffer.findForward(firstDefM, buffer.start(), buffer.end());
    int stopRow = (firstDef == Loc.None()) ? buffer.lines().size() : Loc.row$extension(firstDef);

    // parse all of the imports into groups
    SeqBuffer<ImportGroup> groups = SeqBuffer.withCapacity(4);
    SeqBuffer<String> imports = SeqBuffer.withCapacity(8);
    int firstRow = 0;
    for (int row = 0, stop = stopRow; row < stop; row++) {
      LineV line = buffer.line(row);
      if (line.matches(importM, 0)) {
        if (imports.isEmpty()) firstRow = row;
        imports.append(line.asString());
      } else if (!imports.isEmpty()) {
        groups.append(new ImportGroup(firstRow, imports.toSeq()));
        imports.clear();
      }
    }
    // if the crazy programmer has imports jammed up against the first class decl,
    // be sure to handle that case
    if (!imports.isEmpty()) groups.append(new ImportGroup(firstRow, imports.toSeq()));

    // if we have at least one group, find the one that has the longest shared prefix with our to be
    // inserted import and most likely insert our new import therein
    if (!groups.isEmpty()) {
      ImportGroup best = groups.maxBy(Std.fn(g -> g.matchLength(newImport)), Comparator.<Integer>naturalOrder());
      // if we already have this import, then report that to the user
      if (best.lines.contains(newImport)) throw Errors.feedback(fqName + " already imported.");
      // make sure we either match at least one package level of our best group, or that the group
      // itself is a hodge-podge (its longest prefix does not contain one package level)
      String sharedPrefix = Completer.sharedPrefix(newImport, best.longestPrefix);
      if (sharedPrefix.contains(".") || !best.longestPrefix.contains(".")) {
        best.insert(buffer, newImport, newLine);
        return;
      }
      // otherwise fall through and create a new group
    }

    // create a new group before the first group which sorts alphabetically after our import
    for (ImportGroup group : groups) {
      if (group.lines.head().compareTo(newImport) < 0) continue;
      buffer.insert(Loc.apply(group.firstRow, 0), Std.seq(newLine, Line.Empty(), Line.Empty()));
      return;
    }

    // if we haven't matched yetd, insert after the last group, or if we have no last group, after
    // the package statement, and if we have no package statement, then at the very start
    int newRow = groups.isEmpty() ? findPackageRow(buffer, stopRow) : groups.last().lastRow();
    buffer.insert(Loc.apply(newRow, 0), newRow > 0 ? Std.seq(Line.Empty(), newLine, Line.Empty()) :
                  Std.seq(newLine, Line.Empty()));
  }

  private static int findPackageRow (Buffer buffer, int stopRow) {
    for (int ii = 0; ii < stopRow; ii++) if (buffer.line(ii).matches(packageM, 0)) return ii+1;
    return 0;
  }
}
