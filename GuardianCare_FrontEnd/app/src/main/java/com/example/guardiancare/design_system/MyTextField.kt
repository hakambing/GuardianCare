package com.example.guardiancare.design_system

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A stable, simplified MyTextField using OutlinedTextField.
 *
 * @param value Current input text.
 * @param onValueChange Callback when text changes.
 * @param hint Label or placeholder text for the field.
 * @param leadingIcon Optional leading icon.
 * @param trailingIcon Optional trailing icon (e.g., checkmark).
 * @param trailingText Optional clickable trailing text (e.g., "Forgot?").
 * @param keyboardOptions Keyboard options (e.g., set KeyboardType.Email).
 * @param isPassword If true, masks the text.
 * @param onLeadingClick Called on leading icon click.
 * @param onTrailingClick Called on trailing icon / text click.
 */
@Composable
fun MyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    trailingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false,
    onLeadingClick: () -> Unit = {},
    onTrailingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visualTransformation =
        if (isPassword) PasswordVisualTransformation() else VisualTransformation.None

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(hint) },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onLeadingClick() }
                )
            }
        },
        trailingIcon = {
            when {
                trailingIcon != null -> {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onTrailingClick() }
                    )
                }
                trailingText != null -> {
                    Text(
                        text = trailingText,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onTrailingClick() }
                    )
                }
                else -> { /* no trailing element */ }
            }
        },
        modifier = modifier
    )
}
