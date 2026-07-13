package com.dentalmarket.app.data

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Firebase's SDK reports results using a "Task" + callback style (older
// Android pattern). Kotlin coroutines (used everywhere else in this app)
// expect functions to just "suspend" and hand back a result directly.
// This one small function bridges the two, so the rest of our code never
// has to deal with callbacks.
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
        }
    }
}
