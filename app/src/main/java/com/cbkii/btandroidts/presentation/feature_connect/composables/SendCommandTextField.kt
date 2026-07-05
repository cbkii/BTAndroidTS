package com.cbkii.btandroidts.presentation.feature_connect.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.ui.theme.BTAndroidTSTheme

@Composable
fun SendCommandTextField(
	value: String,
	onChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	isEnable: Boolean = true,
	onImeAction: () -> Unit = {},
	maxLines: Int = 2,
	shape: Shape = MaterialTheme.shapes.large,
	cursorColor: Brush = SolidColor(MaterialTheme.colorScheme.primary),
	textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
	color: Color = MaterialTheme.colorScheme.onSurface
) {
	val interaction = remember { MutableInteractionSource() }
	val isFocused by interaction.collectIsFocusedAsState()

	val surfaceColor by animateColorAsState(
		targetValue = if (isFocused) MaterialTheme.colorScheme.surfaceContainerHighest
		else MaterialTheme.colorScheme.surfaceContainerHigh,
		label = "Container color"
	)

	val iconButtonColor by animateColorAsState(
		targetValue = if (isEnable) MaterialTheme.colorScheme.primaryContainer
		else MaterialTheme.colorScheme.primary.copy(alpha = .4f),
		label = "Icon button colors"
	)

	val currentOnChange by rememberUpdatedState(onChange)
	val currentOnImeAction by rememberUpdatedState(onImeAction)
	val currentHint = stringResource(id = R.string.text_field_placeholder)

	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier
	) {
		Surface(
			color = surfaceColor,
			shape = shape,
			contentColor = color,
			modifier = Modifier.weight(1f),
		) {
			Box(
				modifier = Modifier.padding(all = 16.dp),
			) {
				androidx.compose.ui.viewinterop.AndroidView(
					factory = { ctx ->
						android.widget.EditText(ctx).apply {
							setText(value)
							background = null
							hint = currentHint
							imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
							inputType = android.text.InputType.TYPE_CLASS_TEXT
							addTextChangedListener(object : android.text.TextWatcher {
								override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
								override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
								override fun afterTextChanged(s: android.text.Editable?) {
									currentOnChange(s?.toString() ?: "")
								}
							})
							setOnEditorActionListener { _, actionId, _ ->
								if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
									currentOnImeAction()
									true
								} else {
									false
								}
							}
						}
					},
					update = { view ->
						if (view.text.toString() != value) {
							view.setText(value)
							view.setSelection(value.length)
						}
						view.isEnabled = isEnable
					},
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
		IconButton(
			onClick = onImeAction,
			enabled = isEnable,
			colors = IconButtonDefaults
				.filledIconButtonColors(containerColor = iconButtonColor)
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_send),
				contentDescription = stringResource(id = R.string.dialog_action_send),
			)
		}
	}
}

private class TextValuePreviewParams :
	CollectionPreviewParameterProvider<String>(listOf("", "Some value"))

@PreviewLightDark
@Composable
private fun SendCommandTextFieldPreview(
	@PreviewParameter(TextValuePreviewParams::class)
	value: String
) = BTAndroidTSTheme {
	Surface {
		SendCommandTextField(
			value = value,
			isEnable = false,
			onChange = {},
			modifier = Modifier.padding(10.dp)
		)
	}
}