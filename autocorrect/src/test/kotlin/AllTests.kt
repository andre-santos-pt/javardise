import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    TestAddAssign::class,
    TestRenameExpression::class
)
class AllTests