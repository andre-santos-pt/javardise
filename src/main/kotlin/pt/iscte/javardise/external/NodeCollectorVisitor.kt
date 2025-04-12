package pt.iscte.javardise.external

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

fun main() {
    val code = """
        class Test {
            static int f() {
                return 0;
            }
            
        }
    """
    val parse = StaticJavaParser.parse(code)
    val ranges = mutableListOf<Node>()
    parse.accept(NodeCollectorVisitor(), ranges)

    println(ranges)

    val parse2 = StaticJavaParser.parse(code)
    val ranges2 = mutableListOf<Node>()
    parse2.accept(NodeCollectorVisitor(), ranges2)

    println(ranges2)
    println(ranges == ranges2)
}

class NodeCollectorVisitor : VoidVisitorAdapter<MutableList<Node>>() {
    override fun visit(n: CompilationUnit, arg: MutableList<Node>) {
       // arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ClassOrInterfaceDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: MethodDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: NodeList<*>, arg: MutableList<Node>) {
        super.visit(n, arg)
    }

    override fun visit(n: AnnotationDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: AnnotationMemberDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ArrayAccessExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ArrayCreationExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ArrayCreationLevel, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ArrayInitializerExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ArrayType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: AssertStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: AssignExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: BinaryExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: BlockComment, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: BlockStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: BooleanLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: BreakStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: CastExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: CatchClause, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: CharLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ClassExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ClassOrInterfaceType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ConditionalExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ConstructorDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ContinueStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: DoStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: DoubleLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: EmptyStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: EnclosedExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: EnumConstantDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: EnumDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ExplicitConstructorInvocationStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ExpressionStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: FieldAccessExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: FieldDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ForStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ForEachStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: IfStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ImportDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: InitializerDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: InstanceOfExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: IntegerLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: IntersectionType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: JavadocComment, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: LabeledStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: LambdaExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: LineComment, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: LocalClassDeclarationStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: LocalRecordDeclarationStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: LongLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: MarkerAnnotationExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: MemberValuePair, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: MethodCallExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: MethodReferenceExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: NameExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: Name, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: NormalAnnotationExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: NullLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ObjectCreationExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: PackageDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: Parameter, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: PrimitiveType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: RecordDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: CompactConstructorDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ReturnStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SimpleName, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SingleMemberAnnotationExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: StringLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SuperExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SwitchEntry, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SwitchStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SynchronizedStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ThisExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ThrowStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: TryStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: TypeExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: TypeParameter, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: UnaryExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: UnionType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: UnknownType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: VariableDeclarationExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: VariableDeclarator, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: VoidType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: WhileStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: WildcardType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ModuleDeclaration, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ModuleRequiresDirective, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ModuleExportsDirective, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ModuleProvidesDirective, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ModuleUsesDirective, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ModuleOpensDirective, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: UnparsableStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: ReceiverParameter, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: VarType, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: Modifier, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: SwitchExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: TextBlockLiteralExpr, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

    override fun visit(n: YieldStmt, arg: MutableList<Node>) {
        arg.add(n)
        super.visit(n, arg)
    }

//    override fun visit(n: PatternExpr, arg: MutableList<Node>) {
//        arg.add(n)
//        super.visit(n, arg)
//    }
}

