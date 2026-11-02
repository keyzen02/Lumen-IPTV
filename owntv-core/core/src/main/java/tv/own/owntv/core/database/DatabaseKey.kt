package tv.own.owntv.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Lumen: wrap a random database passphrase with a non-exportable Android Keystore key.
 * No plaintext copy, cloud backup, fixed key, or destructive recovery on key loss.
 * The separate application ID means official OwnTV's unencrypted database is never opened here.
 */
object DatabaseKey {
    private const val ALIAS = "lumen.database.wrap.v1"
    private const val KEY_FILE = "lumen-database-key.v1"
    private const val AAD = "tv.lumen.iptv.database.v1"

    @Synchronized
    fun load(context: Context): ByteArray {
        val file = AtomicFile(File(context.noBackupFilesDir, KEY_FILE))
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (file.baseFile.exists()) {
            val key = store.getKey(ALIAS, null) as? SecretKey
                ?: error("Database wrapping key is unavailable; restore an encrypted backup.")
            val bytes = file.readFully()
            require(bytes.size >= 12 + 16) { "Invalid database key envelope" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
            return cipher.doFinal(bytes, 12, bytes.size - 12)
        }
        check(!context.getDatabasePath(OwnTVDatabase.NAME).exists()) {
            "Database exists without its key; refusing to replace or erase it."
        }
        val key = (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore",
        ).apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try {
            output.write(cipher.iv + cipher.doFinal(passphrase))
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            passphrase.fill(0)
            throw failure
        }
        return passphrase
    }
}
