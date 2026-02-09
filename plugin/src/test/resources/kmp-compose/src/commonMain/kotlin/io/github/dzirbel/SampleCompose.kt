package io.github.dzirbel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Greeting(modifier: Modifier, name: String) {
    name.hashCode()
    Body(modifier)
}

@Composable
private fun Body(modifier: Modifier = Modifier) {
    modifier.hashCode()
}
