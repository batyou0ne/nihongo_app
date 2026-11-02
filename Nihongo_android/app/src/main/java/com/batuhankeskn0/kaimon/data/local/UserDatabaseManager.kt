package com.batuhankeskn0.kaimon.data.local

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı bazlı Room veritabanı yöneticisi (`progress_<uid>.db`).
 *
 * iOS `NihongoApp.swift` içindeki `createModelContainer(for: uid)` davranışını birebir uygular:
 * - Oturum açılmamışsa veya anonim oturumdaysa `progress_anonymous.db` kullanılır.
 * - Her Firebase `uid` için izole `progress_<uid>.db` dosyası açılır.
 * - Anonim kullanıcı gerçek bir hesaba geçiş yaptığında (veya eski `default.db` / `progress_anonymous.db`
 *   mevcut olup yeni `progress_<uid>.db` henüz oluşmamışsa), `.db`, `-shm` ve `-wal` dosyaları
 *   veri kaybı yaşanmadan yeni kullanıcının veritabanı dosyasına taşınır (Migration).
 */
@Singleton
class UserDatabaseManager {

    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var activeDatabase: KaimonDatabase? = null

    private val _currentUidFlow = MutableStateFlow(ANONYMOUS_UID)
    val currentUidFlow: StateFlow<String> = _currentUidFlow.asStateFlow()

    private val _databaseFlow = MutableStateFlow<KaimonDatabase?>(null)
    val databaseFlow: StateFlow<KaimonDatabase?> = _databaseFlow.asStateFlow()

    val currentUid: String
        get() = _currentUidFlow.value

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.appContext = context.applicationContext
        sharedInstance = this
    }

    constructor()

    /**
     * Aktif kullanıcı için Room veritabanını döner; henüz açılmamışsa oluşturur.
     */
    fun getDatabase(uid: String? = _currentUidFlow.value): KaimonDatabase {
        synchronized(lock) {
            val targetUid = normalizeUid(uid)
            val currentDb = activeDatabase
            if (currentDb != null && _currentUidFlow.value == targetUid) {
                return currentDb
            }
            return switchUserInternal(targetUid)
        }
    }

    /**
     * Firebase Auth `uid` değiştiğinde çağrılır.
     * Önceki bağlantıyı güvenli şekilde kapatır, gerekiyorsa anonim/varsayılan veritabanını
     * yeni `uid` dosyasına taşır ve yeni `progress_<uid>.db` bağlantısını açar.
     */
    fun switchUser(uid: String?): KaimonDatabase {
        synchronized(lock) {
            val targetUid = normalizeUid(uid)
            val currentDb = activeDatabase
            if (currentDb != null && _currentUidFlow.value == targetUid) {
                return currentDb
            }
            return switchUserInternal(targetUid)
        }
    }

    val progressDao: ProgressDao
        get() = getDatabase().progressDao()

    val sessionDao: SessionDao
        get() = getDatabase().sessionDao()

    val activityDao: ActivityDao
        get() = getDatabase().activityDao()

    val levelProgressDao: LevelProgressDao
        get() = getDatabase().levelProgressDao()

    internal fun attachContext(context: Context) {
        synchronized(lock) {
            this.appContext = context.applicationContext
        }
    }

    /**
     * Birim testlerde bellek-içi (in-memory) veya sahte `KaimonDatabase` enjekte etmek için kullanılır.
     */
    fun setDatabaseForTesting(database: KaimonDatabase, uid: String = ANONYMOUS_UID) {
        synchronized(lock) {
            val normalizedUid = normalizeUid(uid)
            activeDatabase = database
            _currentUidFlow.value = normalizedUid
            _databaseFlow.value = database
        }
    }

    fun close() {
        synchronized(lock) {
            activeDatabase?.close()
            activeDatabase = null
            _databaseFlow.value = null
        }
    }

    /**
     * Hesap silme (`AuthService.deleteAccount`) sonrası kullanıcının yerel `progress_<uid>.db`
     * dosyalarını (`-shm`/`-wal` dahil) kalıcı olarak siler. Aktif veritabanı bu kullanıcıya aitse önce kapatılır.
     * Anonim veritabanı bu yöntemle silinemez.
     */
    fun deleteUserDatabase(uid: String?): Boolean {
        synchronized(lock) {
            val targetUid = normalizeUid(uid)
            if (targetUid == ANONYMOUS_UID) return false
            val context = appContext ?: return false

            if (_currentUidFlow.value == targetUid) {
                activeDatabase?.close()
                activeDatabase = null
                _databaseFlow.value = null
            }
            return context.deleteDatabase(databaseFileName(targetUid))
        }
    }

    private fun switchUserInternal(targetUid: String): KaimonDatabase {
        val context = checkNotNull(appContext) {
            "UserDatabaseManager başlatılmadı. KaimonApplication.onCreate içinde UserDatabaseManager.init(context) çağrılmalıdır."
        }

        // Açık veritabanı varsa WAL/SHM kayıtlarının diske yazılması için önce kapat.
        activeDatabase?.close()
        activeDatabase = null

        val targetDbName = databaseFileName(targetUid)
        val targetDbFile = context.getDatabasePath(targetDbName)
        val databaseDir = targetDbFile.parentFile ?: context.filesDir
        if (!databaseDir.exists()) {
            databaseDir.mkdirs()
        }

        migrateDatabaseFilesIfNeeded(databaseDir = databaseDir, targetUid = targetUid)

        val newDatabase = Room.databaseBuilder(
            context,
            KaimonDatabase::class.java,
            targetDbName
        )
            .fallbackToDestructiveMigration()
            .build()

        activeDatabase = newDatabase
        _currentUidFlow.value = targetUid
        _databaseFlow.value = newDatabase
        return newDatabase
    }

    companion object {
        const val ANONYMOUS_UID = "anonymous"
        const val LEGACY_DEFAULT_DB_NAME = "default.db"

        @Volatile
        private var sharedInstance: UserDatabaseManager = UserDatabaseManager()

        val shared: UserDatabaseManager
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }

        /**
         * Firebase kullanıcısından yerel veritabanı kimliğini çözer: oturum yoksa veya anonim oturumdaysa
         * `anonymous`, bağlı (Google/Email) hesapta ise gerçek `uid` döner.
         * Anonim -> bağlı geçişte `migrateDatabaseFilesIfNeeded` ilerlemeyi yeni `progress_<uid>.db` dosyasına taşır.
         */
        fun resolveDatabaseUid(firebaseUid: String?, isAnonymous: Boolean): String =
            if (firebaseUid.isNullOrBlank() || isAnonymous) ANONYMOUS_UID else normalizeUid(firebaseUid)

        fun normalizeUid(uid: String?): String {
            val trimmed = uid?.trim().orEmpty()
            if (trimmed.isEmpty()) return ANONYMOUS_UID
            return trimmed.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }

        fun databaseFileName(uid: String?): String =
            "progress_${normalizeUid(uid)}.db"

        /**
         * Eğer hedef `progress_<targetUid>.db` henüz yoksa ve `progress_anonymous.db` (veya eski `default.db`)
         * mevcutsa, `.db`, `-shm` ve `-wal` dosyalarını yeni hedefe taşır.
         */
        fun migrateDatabaseFilesIfNeeded(databaseDir: File, targetUid: String): Boolean {
            val normalizedTarget = normalizeUid(targetUid)
            val targetDbName = databaseFileName(normalizedTarget)
            val targetDbFile = File(databaseDir, targetDbName)

            if (targetDbFile.exists()) {
                return false
            }

            val sourceCandidates = buildList {
                if (normalizedTarget != ANONYMOUS_UID) {
                    add(File(databaseDir, databaseFileName(ANONYMOUS_UID)))
                }
                add(File(databaseDir, LEGACY_DEFAULT_DB_NAME))
            }

            val sourceDbFile = sourceCandidates.firstOrNull { it.exists() && it.isFile } ?: return false

            return runCatching {
                moveFileAtomicallyOrCopy(sourceDbFile, targetDbFile)

                val sourceShm = File(databaseDir, "${sourceDbFile.name}-shm")
                val targetShm = File(databaseDir, "$targetDbName-shm")
                if (sourceShm.exists()) {
                    moveFileAtomicallyOrCopy(sourceShm, targetShm)
                }

                val sourceWal = File(databaseDir, "${sourceDbFile.name}-wal")
                val targetWal = File(databaseDir, "$targetDbName-wal")
                if (sourceWal.exists()) {
                    moveFileAtomicallyOrCopy(sourceWal, targetWal)
                }
                true
            }.getOrDefault(false)
        }

        private fun moveFileAtomicallyOrCopy(source: File, target: File) {
            target.parentFile?.mkdirs()
            runCatching {
                Files.move(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }.onFailure {
                source.copyTo(target, overwrite = true)
                source.delete()
            }
        }
    }
}
