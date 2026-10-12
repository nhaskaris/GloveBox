package com.eliteonetube.glovebox.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.util.OdometerCheck

/** Asks the user to confirm an odometer reading that looks wrong; saving anyway is always possible. */
@Composable
fun OdometerWarningDialog(
    check: OdometerCheck,
    unit: String,
    onSaveAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = when (check) {
        is OdometerCheck.LowerThanPrevious -> stringResource(R.string.odometer_warn_lower, check.previous, unit)
        is OdometerCheck.HigherThanNext -> stringResource(R.string.odometer_warn_higher, check.next, unit)
        is OdometerCheck.ImplausibleJump -> stringResource(R.string.odometer_warn_jump, check.distance, unit, check.previous)
        OdometerCheck.Ok -> return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.odometer_warn_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onSaveAnyway) { Text(stringResource(R.string.save_anyway)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_reading)) } }
    )
}
