//
// Scaled Java Project Support - helps Scaled Project framework grok Java.
// http://github.com/scaled/java-project/blob/master/LICENSE

package scaled.project

import codex.model._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, FileVisitResult, Path, SimpleFileVisitor}
import java.util.HashMap
import java.util.function.{Function, Predicate}
import java.util.jar.{JarFile, JarEntry}
import java.util.regex.Pattern
import java.util.stream.Stream
import org.objectweb.asm._
import org.objectweb.asm.signature.{SignatureReader, SignatureVisitor}
import org.objectweb.asm.util.TraceSignatureVisitor
import scaled._

/** [[ByteCodex]] helpers et cetera. */
object ByteCodex {
  import Opcodes._

  class Flags (val acc :Int) extends AnyVal {
    def isAbstract = (acc & ACC_ABSTRACT) != 0
    def isAnnotation = (acc & ACC_ANNOTATION) != 0
    def isBridge = (acc & ACC_BRIDGE) != 0
    def isDeprecated = (acc & ACC_DEPRECATED) != 0
    def isEnum = (acc & ACC_ENUM) != 0
    def isFinal = (acc & ACC_FINAL) != 0
    def isInterface = (acc & ACC_INTERFACE) != 0
    def isMandated = (acc & ACC_MANDATED) != 0
    def isNative = (acc & ACC_NATIVE) != 0
    def isPrivate = (acc & ACC_PRIVATE) != 0
    def isProtected = (acc & ACC_PROTECTED) != 0
    def isPublic = (acc & ACC_PUBLIC) != 0
    def isStatic = (acc & ACC_STATIC) != 0
    def isStrict = (acc & ACC_STRICT) != 0
    def isSuper = (acc & ACC_SUPER) != 0
    def isSynchronized = (acc & ACC_SYNCHRONIZED) != 0
    def isSynthetic = (acc & ACC_SYNTHETIC) != 0
    def isTransient = (acc & ACC_TRANSIENT) != 0
    def isVarargs = (acc & ACC_VARARGS) != 0
    def isVolatile = (acc & ACC_VOLATILE) != 0
  }

  trait Visitor {
    def visit (kind :Kind, name :String, path :List[String], flags :Flags, source :String) :Unit
  }

  def forJar (name :String, jarFile :Path) :ByteCodex = new ByteCodex(name) {
    override protected def onClasses (fn :(String, ClassReader) => Unit) {
      val jfile = new JarFile(jarFile.toFile)
      val entries = jfile.entries()
      while (entries.hasMoreElements) {
        val entry = entries.nextElement() ; val ename = entry.getName
        if (ename endsWith ".class") fn(ename, new ClassReader(jfile.getInputStream(entry)))
      }
      jfile.close()
    }
  }

  def forDir (name :String, root :Path) :ByteCodex = new ByteCodex(name) {
    override protected def onClasses (fn :(String, ClassReader) => Unit) {
      Files.walkFileTree(root, new SimpleFileVisitor[Path]() {
        override def visitFile (file :Path, attrs :BasicFileAttributes) = {
          if (!attrs.isDirectory) {
            val fname = file.getFileName.toString
            if (fname endsWith ".class") fn(fname, new ClassReader(Files.readAllBytes(file)))
          }
          FileVisitResult.CONTINUE
        }
      })
    }
  }

  abstract class Query (kinds :Set[Kind], reqPub :Boolean, val name :String) {
    def matches (node :Node) :Boolean =
      kinds(node.kind) && (!reqPub || node.acc.isPublic) && nameMatches(node.lname)
    def nameMatches (lname :String) :Boolean
  }

  def query (kinds :Set[Kind], pub :Boolean, name :String, prefix :Boolean) :Query =
    if (prefix) new Query(kinds, pub, name) {
      def nameMatches (lname :String) = lname startsWith name
      override def toString = s"prefix($name $kinds $pub)"
    }
    else new Query(kinds, pub, name) {
      def nameMatches (lname :String) = lname == name
      override def toString = s"exact($name $kinds $pub)"
    }

  private class Node (val parent :Node, _kind :Kind, _name :String, _acc :Int,
                      _desc :String, _sig :String, _src :String) {
    var kind  :Kind   = _kind
    var name  :String = _name
    var lname :String = _name.toLowerCase
    var acc   :Flags  = new Flags(_acc)
    var desc  :String = _desc
    var sig   :String = _sig
    var src   :String = _src
    lazy val kids = new HashMap[String,Node]()

    def id = if (kind == Kind.FUNC) s"$name$desc" else name

    // def toElement = Element(kind, name, parent.path, acc.isPublic, elemAttrs)
    // def elemAttrs =  NoAttrs // TODO

    // def members :Seq[Element] = kids.values.map(_.toElement).toSeq

    def node (path :List[String]) :Node =
      if (path.isEmpty) this
      else {
        val k = kids.get(path.head)
        if (k != null) k.node(path.tail)
        else throw new NoSuchElementException(path.toString)
      }

    def get (name :String) = Mutable.getOrPut(kids, name, {
      val desc = (name :: path).reverse.tail.mkString(".") // turn path into Java-style package
      child(Kind.MODULE, name, ACC_PUBLIC, desc, null)
    })

    def child (kind :Kind, name :String, acc :Int, desc :String, sig :String) =
      new Node(this, kind, name, acc, desc, sig, src)

    def add (kind :Kind, name :String, acc :Int, desc :String, sig :String) = {
      val node = child(kind, name, acc, desc, sig)
      kids.put(node.id, node)
      node
    }

    def reinit (kind :Kind, acc :Int, desc :String, sig :String) {
      this.kind = kind
      this.acc = new Flags(acc)
      this.desc = desc
      this.sig = sig
    }

    def path :List[String] = id :: (if (parent == null) Nil else parent.path)

    def signature () = {
      if (kind == Kind.MODULE) desc
      else if (sig != null) decodeSig(name, sig)
      else s"$name$desc"
    }

    def dump (indent :String) {
      println("$indent$this")
      val nindent = s"$indent  "
      kids.values foreach { _.dump(nindent) }
    }

    def visit (path :List[String], vis :Visitor) {
      vis.visit(kind, name, path, acc, src)
      val npath = name :: path
      kids.values foreach { _.visit(npath, vis) }
    }

    def visit (fn :Node => Unit) {
      fn(this)
      kids.values foreach { _.visit(fn) }
    }

    override def toString = {
      val pub = if (acc.isPublic) "pub" else "nonpub"
      s"$kind $name / $signature / $pub / $src"
    }
  }

  private def decodeSig (name :String, sig :String) = {
    val sr = new SignatureReader(sig)
    val tv = new TraceSignatureVisitor(Opcodes.ASM5)
    sr.accept(tv)
    val exns = tv.getExceptions
    if (exns != null) println(s"TODO: $name -> $exns")
    val rv = tv.getReturnType match {
      case null => ""
      case rtyp => s"$rtyp "
    }
    s"$rv$name${tv.getDeclaration}"
  }
}

/** Provides (partial) [[Codex]] metadata from the information in Java classfiles.
  * @param name the name of the `Assembly` element that will be the parent of all elements in this
  * codex.
  *
  * @define PATHNOTE The path should be in outer-most to inner-most order. NOTE: this is the
  * opposite of how it appears in [[Element.path]]. The path should be relative to this assembly
  * (i.e. not contain the assembly name at its root).
  */
abstract class ByteCodex (name :String) {
  import ByteCodex._
  import Opcodes._

  // /** The element that is the parent of all elements in this codex. */
  // val elem = Element(Assembly, name, Nil, true, NoAttrs)

  // /** Returns the element at `path` if any. $PATHNOTE
  //   * @throw NoSuchElementException if the element cannot be found. */
  // def element (path :List[String]) :Element = root.node(path).toElement

  // /** Returns the members of the element at `path`. $PATHNOTE
  //   * @throw NoSuchElementException if the element cannot be found. */
  // def members (path :List[String]) :Seq[Element] = root.node(path).members

  // /** Adds to `into` all elements whose name starts with `name` (if `prefix`) or equals `name` (if
  //   * not `prefix`) and which match `kinds`. `name` should be all lower case and names will be
  //   * matched case insensitively. */
  // def find (query :Query, into :Seq.Builder[Element]) {
  //   println(s"Finding $query in $name")
  //   def add (ns :Iterable[Node]) {
  //     val iter = ns.iterator ; var ii = 0 ; while (iter.hasNext) {
  //       val node = iter.next
  //       if (query.matches(node)) into += node.toElement
  //       ii += 1
  //     }
  //   }
  //   if (query.name.length > 0) index.apply(query.name, add)
  // }

  /** Visits every element in this codex. */
  def visit (vis :Visitor) {
    root.visit(Nil, vis)
  }

  /** Dumps a debugging representation of this codex to stdout. */
  def dump () {
    root.dump("")
  }

  /** Applies `fn` to all classes in this codex. */
  protected def onClasses (fn :(String,ClassReader) => Unit) :Unit

  private val root = new Node(null, Kind.MODULE, name, ACC_PUBLIC, ":assembly", null, "") {
    // override def toElement = ByteCodex.this.elem
  }

  // TODO: something fancier like a btree?
  class Index {
    // a two level mapping (a-z,X) x (a-z,X)
    private[this] val nodes = Array.ofDim[SeqBuffer[Node]](27, 27)
    private def slot (c :Char) = {
      val lc = Character.toLowerCase(c)
      if (lc >= 'a' && lc <= 'z') (lc - 'a')+1 else 0
    }
    private def nodes (name :String, create :Boolean) :SeqBuffer[Node] = {
      val c0 = slot(name.charAt(0))
      val c1 = if (name.length > 1) slot(name.charAt(1)) else 0
      val ns = nodes(c0)(c1)
      if (ns == null && create) {
        val nns = SeqBuffer[Node]()
        nodes(c0)(c1) = nns
        nns
      } else ns
    }

    def add (node :Node) :Unit =
      if (node.name.length > 0) nodes(node.name, true) += node
      // else println(s"Dropping $node")

    def apply (name :String, fn :Iterable[Node] => Unit) {
      def apply (ns :SeqBuffer[Node]) = if (ns != null) fn(ns)
      if (name.length == 1) nodes(slot(name.charAt(0))) foreach(apply)
      else apply(nodes(name, false))
    }

    root.visit(add) // populate ourselves
  }
  lazy private val index = new Index()

  // resolve all of our classes
  onClasses {
    // TODO: react/RFuture$1Sequencer.class, for example, is a class defined in a method in RFuture;
    // expose or not? if so, how to determine what method it's defined in, etc. etc.
    val anonPat = Pattern.compile(".*\\$[0-9]+\\.class")
    val visitor = new ClassViz()
    (file, reader) => try {
      if (!anonPat.matcher(file).matches) {
        // println(s"** Processing $file")
        reader.accept(visitor, ClassReader.SKIP_CODE|ClassReader.SKIP_FRAMES)
      } // else println(s"** Skipping anonymous $file")
    } catch {
      case e :Exception =>
        println(s"Error parsing package class: $file")
        e.printStackTrace(System.err)
    }
  }

  private def isIgnore (acc :Int) = { val f = new Flags(acc) ; (f.isSynthetic || f.isBridge) }

  private class ClassViz extends ClassVisitor(Opcodes.ASM5) {

    private var cnode :Node = _ // the node for the class we're currently processing
    private var fnode :Node = _
    private var mnode :Node = _

    private val fielder = new FieldVisitor(Opcodes.ASM5) {
      override def visitAnnotation (desc :String, viz :Boolean) = {
        // println(s"Field.visitAnnotation($desc, $viz)")
        null
      }

      override def visitTypeAnnotation (typeRef :Int, typePath :TypePath,
                                        desc :String, viz :Boolean) = {
        println(s"Field.visitTypeAnnotation($typeRef, $typePath, $desc, $viz)")
        null
      }

      override def visitAttribute (attr :Attribute) {
        // println(s"Field.visitAttribute($attr)")
      }
    }

    private val methoder = new MethodVisitor(Opcodes.ASM5) {
      override def visitParameter (name :String, access :Int) {
        println(s"Field.visitParameter($name, ${accessToString(access)})")
      }

      override def visitAnnotation (desc :String, viz :Boolean) = {
        // println(s"Method.visitAnnotation($desc, $viz)")
        null
      }

      override def visitTypeAnnotation (typeRef :Int, typePath :TypePath,
                                        desc :String, viz :Boolean) = {
        println(s"Method.visitTypeAnnotation($typeRef, $typePath, $desc, $viz)")
        null
      }

      override def visitParameterAnnotation (param :Int, desc :String, viz :Boolean) = {
        // println(s"Method.visitParameterAnnotation($param, $desc, $viz)")
        null
      }

      override def visitAttribute (attr :Attribute) {
        println(s"Method.visitAttribute($attr)")
      }

      override def visitLineNumber (line :Int, start :Label) {
        println(s"Method.visitLineNumber($line, $start)")
      }
    }

    override def visit (version :Int, access :Int, name :String, sig :String,
                        superName :String, ifcs :Array[String]) {
      // println(s"-- visit($version, ${accessToString(access)}, $name, $sig, $superName, ${ifcs.mkString(" ")})")
      cnode = (decompose(name) :\ root) { (p, n) => n.get(p) }
      cnode.reinit(Kind.TYPE, access, null, sig)
      // TODO: incorporate super class and interfaces into model?
    }

    override def visitSource (source :String, debug :String) {
      // println(s"visitSource($source, $debug)")
      cnode.src = source
      if (debug != null) println(s"LOOK MA, debug: $debug")
    }

    override def visitOuterClass (owner :String, name :String, desc :String) {
      // println(s"visitOuterClass($owner, $name, $desc)")
    }

    override def visitAnnotation (desc :String, viz :Boolean) = {
      // TODO: incorporate this into attrs? or signature?
      // println(s"visitAnnotation($desc, $viz)")
      null
    }

    override def visitTypeAnnotation (typeRef :Int, typePath :TypePath,
                                      desc :String, viz :Boolean) = {
      // TODO: incorporate this into attrs? or signature?
      println(s"visitTypeAnnotation($typeRef, $typePath, $desc, $viz)")
      null
    }

    override def visitAttribute (attr :Attribute) {
      // println(s"visitAttribute($attr)")
    }

    override def visitInnerClass (name :String, outerName :String, innerName :String, access :Int) {
      // println(s"visitInnerClass($name, $outerName, $innerName, ${accessToString(access)})")
    }

    override def visitField (access :Int, name :String, desc :String, sig :String,
                             value :Object) = {
      // println(s"visitField(${accessToString(access)}, $name, $desc, $sig, $value)")
      if (isIgnore(access)) null else {
        fnode = cnode.add(Kind.VALUE, name, access, desc, sig)
        fielder
      }
    }

    override def visitMethod (access :Int, name :String, desc :String, sig :String,
                              exns :Array[String]) = {
      // println(s"visitMethod(${accessToString(access)}, $name, $desc, $sig, $exns)")
      if (isIgnore(access)) null else {
        mnode = cnode.add(Kind.FUNC, name, access, desc, sig)
        methoder
      }
    }

    override def visitEnd () {
      // println(s"visitEnd()")
      cnode = null
      mnode = null
      fnode = null
    }

    private def decompose (name :String) :List[String] = {
      def popdollars (name :String) :List[String] = name.lastIndexOf('$') match {
        case -1 => name.lastIndexOf('.') match {
          case -1 => name :: Nil
          case ii => name.substring(ii+1) :: name.substring(0, ii) :: Nil
        }
        case ii => name.substring(ii+1) :: popdollars(name.substring(0, ii))
      }
      popdollars(name.replace('/', '.'))
    }

    private def accessToString (acc :Int) = {
      val sb = new StringBuilder()
      def add (flag :Int, name :String) = if ((acc & flag) != 0) sb.append('|').append(name)
      add(ACC_ABSTRACT, "abstract")
      add(ACC_ANNOTATION, "annotation")
      add(ACC_BRIDGE, "bridge")
      add(ACC_DEPRECATED, "deprecated")
      add(ACC_ENUM, "enum")
      add(ACC_FINAL, "final")
      add(ACC_INTERFACE, "interface")
      add(ACC_MANDATED, "mandated")
      add(ACC_NATIVE, "native")
      add(ACC_PRIVATE, "private")
      add(ACC_PROTECTED, "protected")
      add(ACC_PUBLIC, "public")
      add(ACC_STATIC, "static")
      add(ACC_STRICT, "strict")
      add(ACC_SUPER, "super")
      add(ACC_SYNCHRONIZED, "synchronized")
      add(ACC_SYNTHETIC, "syntheitc")
      add(ACC_TRANSIENT, "transient")
      add(ACC_VARARGS, "varargs")
      add(ACC_VOLATILE, "volatile")
      if (sb.length > 0) sb.deleteCharAt(0) // trim first |
      sb.toString
    }
  }
}
