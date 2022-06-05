package com.linversion.simplecolorpicker.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.linversion.simplecolorpicker.OpenImageActivity

/**
 * @author linversion
 * on 2022/5/14
 */
@ExperimentalPermissionsApi
@Composable
fun Permission(
    permission: String = android.Manifest.permission.CAMERA,
    rationale: String = "This permission is important for this app. Please grant the permission",
    permissionNotAvailableContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission)
    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            OpenImageActivity.startActivity(context, it)
        }
    }

    PermissionRequired(
        permissionState = permissionState,
        permissionNotGrantedContent = {
            Rationale(text = rationale,
                onRequestPermission = {
                    permissionState.launchPermissionRequest()
                },
                onSelectImage = {
                    launcher.launch("image/*")
                }
            )
        },
        permissionNotAvailableContent = permissionNotAvailableContent,
        content = content
    )
}

@Composable
private fun Rationale(
    text: String,
    onRequestPermission: () -> Unit,
    onSelectImage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Permission request")
        },
        text = {
            Text(text)
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Grant")
            }
        },
        dismissButton = {
            Button(onClick = onSelectImage) {
                Text(text = "Select Image")
            }
        }
    )
}