package pt.iscte.javardise.tests

import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    MethodSignatureTest::class,
    CommandTests::class,
    FocusTest::class,
    ObserverTests::class)
class TestSuite

const val TEST_SPEED = 500