//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java;

import scala.Option;
import scala.collection.immutable.$colon$colon;
import scala.collection.immutable.List;
import scala.collection.mutable.WrappedArray;

public class SC {

  public static <T> List<T> nil () {
    return (List<T>)List.empty();
  }

  public static <T> List<T> list (T... elems) {
    return WrappedArray.<T>make(elems).toList();
  }

  public static <A, B extends A> List<B> cons (B head, List<A> list) {
    return new $colon$colon(head, list);
  }

  public static <A> Option<A> none () {
    return Option.<A>apply(null);
  }
}
