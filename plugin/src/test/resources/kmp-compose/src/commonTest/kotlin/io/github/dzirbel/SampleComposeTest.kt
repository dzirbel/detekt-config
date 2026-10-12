package io.github.dzirbel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

@Composable
fun TestGreeting(modifier: Modifier) {
    Greeting(name = "test", modifier = modifier)
}

class ComposeLibraryTest {
    @Test
    fun emptyModifierIsAnIdentity() {
        assertEquals(Modifier, Modifier.then(Modifier))
    }
}
