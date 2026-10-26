@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.compose.ui.test.junit4
import androidx.activity.ComponentActivity
class ActivityScenario<A> { fun recreate(): ActivityScenario<A> = this }
class ActivityScenarioRule<A> : org.junit.rules.TestRule { override fun apply(b: org.junit.runners.model.Statement, d: org.junit.runner.Description) = b; val scenario: ActivityScenario<A> get() = TODO() }
abstract class AndroidComposeTestRule<R : org.junit.rules.TestRule, A : ComponentActivity> : ComposeContentTestRule {
    abstract val activityRule: R
}
inline fun <reified A : ComponentActivity> createAndroidComposeRule(): AndroidComposeTestRule<ActivityScenarioRule<A>, A> = TODO()
