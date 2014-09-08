//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scala.collection.immutable.List;
import scala.collection.Seq;

public class SC {

  public static <T> List<T> list (T... elems) {
    throw new RuntimeException("TODO");
  }

  public static <A, B extends A> List<B> cons (B head, List<A> list) {
    throw new RuntimeException("TODO");
  }

  public static <A> Seq<A> concat (Seq<? extends A> sa, Seq<? extends A> sb) {
    throw new RuntimeException("TODO");
  }
}
