package com.example.money.data.local

/**
 * SQLCipher does not load its native library. Opening a database before this
 * call crashes in `SQLiteConnection.nativeOpen` with [UnsatisfiedLinkError].
 */
internal object SqlCipherLibrary {
    fun load() {
        System.loadLibrary("sqlcipher")
    }
}
