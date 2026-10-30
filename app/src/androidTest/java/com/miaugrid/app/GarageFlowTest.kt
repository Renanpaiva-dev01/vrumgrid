package com.miaugrid.app

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.miaugrid.core.settings.CarBody
import com.miaugrid.core.settings.CarDecal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Garagem: as escolhas do carrinho são salvas nas configurações e sobrevivem à recriação. */
@RunWith(AndroidJUnit4::class)
class GarageFlowTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetPlayerDataRule()).around(compose)

    private val settings get() = ApplicationProvider.getApplicationContext<MiauGridApp>().container.playerData.settings

    /**
     * `performScrollTo` só rola o contêiner rolável mais próximo. Os itens da garagem ficam em
     * linhas com rolagem horizontal dentro da coluna vertical, então rolamos a coluna primeiro.
     */
    private fun clickGarageItem(tag: String) {
        compose.onNodeWithTag("garage").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun waitFor(tag: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun escolhasDaGaragemSaoSalvas() {
        waitFor("open_garage")
        compose.onNodeWithTag("open_garage").performScrollTo().performClick()
        waitFor("garage")
        clickGarageItem("body_van")
        compose.waitUntil(3_000) { settings.value.car.body == CarBody.VAN }
        clickGarageItem("decal_number")
        compose.onNodeWithTag("decal_number").assertIsSelected()
        compose.waitUntil(3_000) { settings.value.car.decal == CarDecal.NUMBER }
        waitFor("number_plus")
        clickGarageItem("number_plus")

        compose.waitUntil(3_000) { settings.value.car.number == 8 }
        assertEquals(CarBody.VAN, settings.value.car.body)
        assertEquals(CarDecal.NUMBER, settings.value.car.decal)

        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { settings.value.car.body == CarBody.VAN }
    }

    @Test
    fun restaurarCarroOriginal() {
        waitFor("open_garage")
        compose.onNodeWithTag("open_garage").performScrollTo().performClick()
        waitFor("garage")
        clickGarageItem("body_formula")
        compose.waitUntil(3_000) { settings.value.car.body == CarBody.FORMULA }
        clickGarageItem("garage_reset")
        compose.waitUntil(3_000) { settings.value.car.body == CarBody.SEDAN }
    }
}
