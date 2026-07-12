/*
 * Copyright (c) 2025, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.kotlin.ble.client.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Serializes Bluetooth LE operations per device identifier while allowing operations for distinct
 * devices to proceed independently.
 */
object OperationMutex {
    private val locks = ConcurrentHashMap<String, Mutex>()

    private fun getLock(identifier: String): Mutex {
        locks[identifier]?.let { return it }

        val candidate = Mutex(locked = false)
        return locks.putIfAbsent(identifier, candidate) ?: candidate
    }

    /**
     * Executes the given action under this mutex's lock.
     *
     * @param identifier - The device identifier (e.g. MAC address).
     * @param owner - Optional owner token for debugging. When owner is specified (non-null value)
     * and this mutex is already locked with the same token (same identity),
     * this function throws [IllegalStateException].
     * @param block - The action to execute under the mutex's lock
     */
    suspend fun <T> withLock(identifier: String, owner: Any? = null, block: suspend () -> T): T {
        return getLock(identifier).withLock(owner) { block() }
    }

    /**
     * Locks this mutex, suspending caller until the lock is acquired (in other words, while the
     * lock is held elsewhere).
     *
     * This suspending function is cancellable: if the Job of the current coroutine is canceled
     * while this suspending function is waiting, this function immediately resumes with
     * [CancellationException].
     */
    suspend fun lock(identifier: String, owner: Any? = null) {
        getLock(identifier).lock(owner)
    }

    /**
     * Unlocks this mutex.
     *
     * Throws [IllegalStateException] if invoked on a mutex that is not locked or was locked with
     * a different owner token (by identity).
     */
    fun unlock(identifier: String, owner: Any? = null) {
        getLock(identifier).unlock(owner)
    }

    /**
     * Checks whether this mutex is locked by the specified owner.
     *
     * @param identifier - The device identifier (e.g. MAC address).
     * @param owner - The owner token to check.
     * @return `true` if this mutex is locked by the given owner.
     */
    fun holdsLock(identifier: String, owner: Any): Boolean {
        return locks[identifier]?.holdsLock(owner) == true
    }
}
