package com.cbkii.btandroidts.presentation.navigation.args

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PeripheralDetailArgs(
    val address: String,
    val name: String
) : Parcelable
