import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    TestAddAssign::class,
    TestAddCall::class,
    TestRenameExpression::class
)
class AllTests