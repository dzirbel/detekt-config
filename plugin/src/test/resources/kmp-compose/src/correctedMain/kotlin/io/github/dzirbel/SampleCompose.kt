package io.github.dzirbel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    remember(name, modifier) { name.hashCode() + modifier.hashCode() }
}
