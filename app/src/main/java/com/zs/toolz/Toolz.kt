package com.zs.toolz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.zs.toolz.common.NavController
import com.zs.compose.theme.snackbar.SnackbarHostState as SnackbarController


@Composable
@NonRestartableComposable
context(activity: MainActivity)
fun Toolz(
    navController: NavController,
    controller: SnackbarController
) {

}