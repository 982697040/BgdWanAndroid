package com.bgd.myapplication.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileNotFoundException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** No transport types escape this boundary. Generation rejects results from a previous account. */
data class SessionSnapshot(
    val initialized: Boolean = false,
    val generation: Long = 0,
    val cookies: List<String> = emptyList(),
    val username: String? = null,
    val collectedIds: Set<Int> = emptySet(),
)

interface SessionStorage {
    val snapshots: StateFlow<SessionSnapshot>
    fun initialize()
    fun reset(): Long
    fun invalidate(generation: Long)
    fun update(generation: Long, transform: (SessionSnapshot) -> SessionSnapshot)
}

@Serializable
private data class StoredSession(
    val version: Int = 1,
    val cookies: List<String> = emptyList(),
    val username: String? = null,
    val collectedIds: Set<Int> = emptySet(),
)

/** Blocking storage methods are invoked only on IO workers, never from composition. */
@Singleton
class EncryptedSessionStorage @Inject constructor(@ApplicationContext context: Context) : SessionStorage {
    private val file = AtomicFile(File(context.noBackupFilesDir, "wanandroid-session"))
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableSnapshots = MutableStateFlow(SessionSnapshot())
    override val snapshots = mutableSnapshots.asStateFlow()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    @Synchronized override fun initialize() {
        if (mutableSnapshots.value.initialized) return
        val stored = try {
            val bytes = file.openRead().use { it.readBytes() }
            if (bytes.size < 28) throw SerializationException("Invalid encrypted session")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            json.decodeFromString<StoredSession>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString())
                .also { if (it.version != 1) throw SerializationException("Unsupported session version") }
        } catch (error: FileNotFoundException) {
            if (file.baseFile.exists()) throw AppException(AppError.Storage, error)
            StoredSession()
        } catch (_: GeneralSecurityException) {
            // Credentials encrypted under an invalidated key cannot be recovered.
            file.delete()
            try {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(KEY_ALIAS) }
            } catch (error: Exception) {
                throw AppException(AppError.Storage, error)
            }
            StoredSession()
        } catch (_: SerializationException) {
            file.delete()
            StoredSession()
        } catch (error: Exception) {
            throw AppException(AppError.Storage, error)
        }
        mutableSnapshots.value = SessionSnapshot(true, cookies = stored.cookies,
            username = stored.username, collectedIds = stored.collectedIds)
    }

    @Synchronized override fun reset(): Long {
        initialize()
        val next = SessionSnapshot(initialized = true, generation = mutableSnapshots.value.generation + 1)
        persist(next)
        mutableSnapshots.value = next
        return next.generation
    }

    @Synchronized override fun invalidate(generation: Long) {
        if (mutableSnapshots.value.generation == generation) reset()
    }

    @Synchronized override fun update(generation: Long, transform: (SessionSnapshot) -> SessionSnapshot) {
        initialize()
        val current = mutableSnapshots.value
        if (current.generation != generation) throw AppException(AppError.SessionChanged)
        val next = transform(current).copy(initialized = true, generation = generation)
        persist(next)
        mutableSnapshots.value = next
    }

    private fun persist(snapshot: SessionSnapshot) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
            val record = StoredSession(cookies = snapshot.cookies, username = snapshot.username, collectedIds = snapshot.collectedIds)
            val bytes = cipher.iv + cipher.doFinal(json.encodeToString(record).encodeToByteArray())
            val stream = file.startWrite()
            try { stream.write(bytes); file.finishWrite(stream) }
            catch (error: Exception) { file.failWrite(stream); throw error }
        } catch (error: Exception) { throw AppException(AppError.Storage, error) }
    }

    private companion object { const val KEY_ALIAS = "wanandroid-session" }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionStorageModule {
    @Binds abstract fun sessionStorage(storage: EncryptedSessionStorage): SessionStorage
}
