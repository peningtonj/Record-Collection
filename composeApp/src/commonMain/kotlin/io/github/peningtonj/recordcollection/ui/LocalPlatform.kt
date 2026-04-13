package io.github.peningtonj.recordcollection.ui

import androidx.compose.runtime.compositionLocalOf

enum class AppPlatform { ANDROID, DESKTOP }

/** Provided by each platform's RecordCollectionApp so shared UI can adapt its layout. */
val LocalPlatform = compositionLocalOf { AppPlatform.DESKTOP }

