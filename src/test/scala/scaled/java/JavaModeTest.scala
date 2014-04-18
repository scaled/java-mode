//
// Scaled Java Mode - a Scaled major mode for editing Java code
// http://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.java

import java.io.{File, StringReader}
import org.junit.Assert._
import org.junit._
import scaled._
import scaled.grammar._
import scaled.impl.BufferImpl
import scaled.major.CodeConfig

class JavaModeTest {

  val testJavaCode = Seq(
    //                1         2         3         4         5         6         7         8
    //      012345678901234567890123456789012345678901234567890123456789012345678901234567890123456
    /* 0*/ "package foo;",
    /* 1*/ "",
    /* 2*/ "/**",
    /* 3*/ " * This is some test Java code that we'll use to test {@link Grammar} and specifically",
    /* 4*/ " * the {@literal JavaDoc} grammar.",
    /* 5*/ " * @see http://manual.macromates.com/en/language_grammars",
    /* 6*/ " */",
    /* 7*/ "public class Test extends Baffle {",
    /* 8*/ "   /**",
    /* 9*/ "    * A constructor, woo!",
    /*10*/ "    * @param foo for fooing.",
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

  val html = getClass.getClassLoader.getResourceAsStream("HTML.tmLanguage")
  val javaDoc = getClass.getClassLoader.getResourceAsStream("JavaDoc.tmLanguage")
  val java = getClass.getClassLoader.getResourceAsStream("Java.tmLanguage")
  // Grammar.parse(java).print(System.out)
  val grammars = Seq(Grammar.parse(html), Grammar.parse(javaDoc), Grammar.parse(java))

  @Test def testStylesLink () {
    val buffer = testBuffer("Test.java", testJavaCode)
    val scoper = new Scoper(grammars, buffer, List(new Selector.Processor(JavaConfig.effacers)))
    // println(scoper.showMatchers(Set("#code", "#class")))
    assertTrue("@link contents scoped as link",
               scoper.scopesAt(Loc(3, 61)).contains("markup.underline.link.javadoc"))
    assertEquals("@link contents styled as link",
                 Styles(CodeConfig.preprocessorStyle), buffer.stylesAt(Loc(3, 61)))
  }
}
