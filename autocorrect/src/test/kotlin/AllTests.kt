import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    TestAddField::class,
    TestAddMethod::class,
    TestAddParameter::class,
    TestModifyParameter::class,
    TestAddAssign::class,
    TestAddCall::class,
    TestRenameCallExpression::class,
    TestCallExpressionAddScope::class,
    TestRenameExpression::class,
    TestModifyCallExpression::class,

    TestModifyBinaryExpression::class,
    TestModifyUnaryExpression::class
)
class AllTests