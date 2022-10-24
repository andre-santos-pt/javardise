package pt.iscte.javardise.tests

import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    CommandTests::class,
    FocusTest::class,
    ObserverTests::class)
class TestSuite

const val TEST_SPEED = 200