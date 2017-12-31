//
// Scaled Java Mode - support for editing Java code
// https://github.com/scaled/java-mode/blob/master/LICENSE

package scaled.code

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
    /* 2*/ "/**",
    /* 3*/ " * This is some test Java code that we'll use to test {@link Grammar} and specifically",
    /* 4*/ " * the {@literal JavaDoc} grammar.",
    /* 5*/ " * @see http://manual.macromates.com/en/language_grammars",
    /* 6*/ " */",
    /* 7*/ "public class Test extends Baffle {",
    /* 8*/ "   /**",
    /* 9*/ "    * A constructor, <b>woo</b>!",
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

  def javaDoc = getClass.getClassLoader.getResource("JavaDoc.ndf")
  def java = getClass.getClassLoader.getResource("Java.ndf")
  def javaProps = getClass.getClassLoader.getResource("JavaProperties.ndf")
  val grammars = Seq(Grammar.parseNDF(javaDoc), Grammar.parseNDF(java))

  @Test def dumpGrammar () {
    Grammar.parseNDF(javaProps).print(System.out)
  }

  @Test def testStylesLink () {
    val buffer = BufferImpl(new TextStore("Test.java", "", testJavaCode))
    val scoper = Grammar.testScoper(
      grammars, buffer, List(new Selector.Processor(new JavaGrammarPlugin().effacers)))
    // println(scoper.showMatchers(Set("#code", "#class")))
    // 0 until buffer.lines.length foreach {
    //   ll => scoper.showScopes(ll) foreach { s => println(ll + ": " + s) }}
    assertTrue("@link contents scoped as link",
               scoper.scopesAt(Loc(3, 61)).contains("markup.underline.link.javadoc"))
    assertEquals("@link contents styled as link",
                 List(CodeConfig.preprocessorStyle), buffer.stylesAt(Loc(3, 61)))
  }
}
