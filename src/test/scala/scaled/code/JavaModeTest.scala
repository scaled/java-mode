//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

import java.io.{File, StringReader}
import org.junit.Assert._
import org.junit._
import scaled._
import scaled.grammar._
import scaled.impl.BufferImpl

class JavaModeTest {

  val testJavaCode = Seq(
    //                1         2         3         4         5         6         7         8
    //      012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
    /* 0*/ "package foo;",
    /* 1*/ "",
    /* 2*/ "/** This is some test Java code that we'll use to test {@code Grammar} and specifically",
    /* 3*/ " * the {@literal JavaDoc} grammar.",
    /* 4*/ " * @see http://manual.macromates.com/en/language_grammars",
    /* 5*/ " */",
    /* 6*/ "public class Test extends Baffle {",
    /* 7*/ "   /**",
    /* 8*/ "    * A constructor, woo!",
    /* 8*/ "    * @param foo for fooing.",
    /*10*/ "    * @param bar for barring.",
    /*11*/ "    */",
    /*12*/ "   public Test () {}",
    /*13*/ "",
    /*14*/ "   /**",
    /*15*/ "    * A method. How exciting. Let's {@link Test} to something.",
    /*16*/ "    * @throws IllegalArgumentException if we feel like it.",
    /*17*/ "    */",
    /*18*/ "   @Deprecated(\"Use peanuts\")",
    /*19*/ "   public void test (int count) {}",
    /*20*/ "}").mkString("\n")

  def testBuffer (name :String, text :String) =
    BufferImpl(name, new File(name), new StringReader(text))

  @Test def testParse () {
    val html = getClass.getClassLoader.getResourceAsStream("HTML.tmLanguage")
    val javaDoc = getClass.getClassLoader.getResourceAsStream("JavaDoc.tmLanguage")
    val java = getClass.getClassLoader.getResourceAsStream("Java.tmLanguage")
    // Grammar.parse(java).print(System.out)
    val grammars = Seq(Grammar.parse(html), Grammar.parse(javaDoc), Grammar.parse(java))
    val buffer = testBuffer("Test.java", testJavaCode)
    val scoper = new Scoper(grammars, buffer)
    println(scoper.toString(Set("#code", "#class")))
    // TEMP: for now use a TextMate grammar for code highlighting
    scoper.apply(new Selector.Processor(JavaConfig.colorizers))
  }
}
