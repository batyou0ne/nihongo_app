package com.batuhankeskn0.kaimon.data.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.batuhankeskn0.kaimon.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bildirim başlık ve gövde metni modeli.
 */
data class ReminderNotificationContent(
    val title: String,
    val body: String
)

/**
 * Kurulmuş günlük hatırlatıcı durumu (test edilebilirlik ve durum takibi için).
 */
data class ScheduledReminderSnapshot(
    val hour: Int,
    val minute: Int,
    val activeStreak: Int,
    val title: String,
    val body: String,
    val triggerAtMillis: Long
)

/**
 * Bildirim tercihlerini saklamak için depolama soyutlaması.
 * Cihazda `SharedPreferences`, JVM birim testlerinde `InMemoryReminderStorage` kullanılır.
 */
interface ReminderStorage {
    var isDailyReminderEnabled: Boolean
    var reminderTimeIntervalSeconds: Double
    var savedHour: Int
    var savedMinute: Int
    var savedActiveStreak: Int
    fun clear()
}

class InMemoryReminderStorage : ReminderStorage {
    override var isDailyReminderEnabled: Boolean = false
    override var reminderTimeIntervalSeconds: Double = 0.0
    override var savedHour: Int = 20
    override var savedMinute: Int = 0
    override var savedActiveStreak: Int = 0

    override fun clear() {
        isDailyReminderEnabled = false
        reminderTimeIntervalSeconds = 0.0
        savedHour = 20
        savedMinute = 0
        savedActiveStreak = 0
    }
}

class SharedPrefsReminderStorage(
    private val prefs: SharedPreferences
) : ReminderStorage {
    override var isDailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    override var reminderTimeIntervalSeconds: Double
        get() = Double.fromBits(prefs.getLong(KEY_TIME_INTERVAL_BITS, 0.0.toRawBits()))
        set(value) {
            prefs.edit().putLong(KEY_TIME_INTERVAL_BITS, value.toRawBits()).apply()
        }

    override var savedHour: Int
        get() = prefs.getInt(KEY_HOUR, 20)
        set(value) {
            prefs.edit().putInt(KEY_HOUR, value).apply()
        }

    override var savedMinute: Int
        get() = prefs.getInt(KEY_MINUTE, 0)
        set(value) {
            prefs.edit().putInt(KEY_MINUTE, value).apply()
        }

    override var savedActiveStreak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) {
            prefs.edit().putInt(KEY_STREAK, value).apply()
        }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val KEY_ENABLED = "isDailyReminderEnabled"
        const val KEY_TIME_INTERVAL_BITS = "reminderTimeInterval"
        const val KEY_HOUR = "reminderHour"
        const val KEY_MINUTE = "reminderMinute"
        const val KEY_STREAK = "reminderActiveStreak"
    }
}

/**
 * Günlük seri hatırlatıcı bildirimlerini yöneten merkezi servis.
 *
 * iOS `Services/NotificationManager.swift` sınıfının birebir Android karşılığıdır:
 * - Android 13+ (API 33+) `POST_NOTIFICATIONS` çalışma zamanı izin kontrolü yapar.
 * - `NotificationChannel` kurulumunu gerçekleştirir.
 * - `AlarmManager` ve `DailyReminderReceiver` üzerinden her gün seçilen saatte (`hour`, `minute`)
 *   kullanıcının aktif serisine (`activeStreak`) göre yerel bildirim gönderir.
 * - `UserProgressEntity.recordStudySession` çağrıldığında `updateDailyReminderStreak` ile
 *   bildirim metnindeki seri sayısını otomatik günceller.
 */
@Singleton
class NotificationManager {

    private var appContext: Context? = null
    private var storage: ReminderStorage
    private var nowMillisProvider: () -> Long
    private var timeZoneProvider: () -> TimeZone

    var isAuthorized: Boolean by mutableStateOf(false)
        private set

    var lastScheduledReminder: ScheduledReminderSnapshot? = null
        private set

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        storage = SharedPrefsReminderStorage(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
    ) {
        attachContext(context)
        sharedInstance = this
    }

    constructor(
        storage: ReminderStorage = InMemoryReminderStorage(),
        nowMillisProvider: () -> Long = { System.currentTimeMillis() },
        timeZoneProvider: () -> TimeZone = { TimeZone.getDefault() },
        initialAuthorized: Boolean = true
    ) {
        this.storage = storage
        this.nowMillisProvider = nowMillisProvider
        this.timeZoneProvider = timeZoneProvider
        this.isAuthorized = initialAuthorized
    }

    internal fun attachContext(context: Context) {
        val app = context.applicationContext
        this.appContext = app
        this.storage = SharedPrefsReminderStorage(
            app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
        createNotificationChannel(app)
        checkAuthorizationStatus()
    }

    /**
     * Android 13+ (API 33 / TIRAMISU) için `POST_NOTIFICATIONS` çalışma zamanı iznini ve
     * sistem bildirimlerinin açık olup olmadığını kontrol eder.
     */
    fun checkAuthorizationStatus(): Boolean {
        val context = appContext ?: return isAuthorized
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notificationsEnabled = runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(true)

        isAuthorized = runtimeGranted && notificationsEnabled
        return isAuthorized
    }

    /**
     * Çalışma zamanı bildirim izni sonucunu günceller (`ActivityResultLauncher` dönüşünde çağrılır).
     */
    fun onPermissionResult(granted: Boolean) {
        isAuthorized = granted
    }

    /**
     * iOS `NotificationManager.requestPermission()` karşılığı.
     */
    fun requestPermission(): Boolean {
        return checkAuthorizationStatus()
    }

    var isDailyReminderEnabled: Boolean
        get() = storage.isDailyReminderEnabled
        set(value) {
            storage.isDailyReminderEnabled = value
            if (!value) {
                cancelDailyReminder()
            }
        }

    var reminderTimeIntervalSeconds: Double
        get() = storage.reminderTimeIntervalSeconds
        set(value) {
            storage.reminderTimeIntervalSeconds = value
        }

    /**
     * Her gün belirtilen `hour` ve `minute` saatinde kullanıcının aktif serisine (`activeStreak`)
     * göre yerel hatırlatıcı kurar.
     */
    fun scheduleDailyReminder(hour: Int, minute: Int, activeStreak: Int? = null) {
        val coercedHour = hour.coerceIn(0, 23)
        val coercedMinute = minute.coerceIn(0, 59)
        val resolvedStreak = (activeStreak ?: storage.savedActiveStreak).coerceAtLeast(0)

        storage.savedHour = coercedHour
        storage.savedMinute = coercedMinute
        storage.savedActiveStreak = resolvedStreak

        if (storage.reminderTimeIntervalSeconds <= 0.0) {
            val calendar = Calendar.getInstance(timeZoneProvider()).apply {
                timeInMillis = nowMillisProvider()
                set(Calendar.HOUR_OF_DAY, coercedHour)
                set(Calendar.MINUTE, coercedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            storage.reminderTimeIntervalSeconds = calendar.timeInMillis / 1000.0
        }

        if (!isAuthorized && appContext != null && !checkAuthorizationStatus()) {
            return
        }

        setupDailyReminder(
            hour = coercedHour,
            minute = coercedMinute,
            activeStreak = activeStreak
        )
    }

    private fun setupDailyReminder(hour: Int, minute: Int, activeStreak: Int?) {
        val content = buildReminderContent(activeStreak)
        val triggerAtMillis = computeNextTriggerTimeMillis(
            hour = hour,
            minute = minute,
            nowMillis = nowMillisProvider(),
            timeZone = timeZoneProvider()
        )
        val streakVal = (activeStreak ?: 0).coerceAtLeast(0)

        lastScheduledReminder = ScheduledReminderSnapshot(
            hour = hour,
            minute = minute,
            activeStreak = streakVal,
            title = content.title,
            body = content.body,
            triggerAtMillis = triggerAtMillis
        )

        val context = appContext ?: return
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            hour = hour,
            minute = minute,
            activeStreak = streakVal,
            title = content.title,
            body = content.body
        )

        // Önceki hatırlatıcıyı temizle
        alarmManager.cancel(pendingIntent)

        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }.onFailure {
            runCatching {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    /**
     * `UserProgressEntity.recordStudySession` çağrıldığında bildirim metnindeki seri sayısını
     * otomatik olarak günceller (iOS `NotificationManager.updateDailyReminderStreak` karşılığı).
     */
    fun updateDailyReminderStreak(activeStreak: Int) {
        storage.savedActiveStreak = activeStreak.coerceAtLeast(0)
        if (!storage.isDailyReminderEnabled) return

        val savedTimeSeconds = storage.reminderTimeIntervalSeconds
        val (hour, minute) = if (savedTimeSeconds > 0.0) {
            val calendar = Calendar.getInstance(timeZoneProvider()).apply {
                timeInMillis = (savedTimeSeconds * 1000.0).toLong()
            }
            calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
        } else {
            storage.savedHour to storage.savedMinute
        }

        scheduleDailyReminder(
            hour = hour,
            minute = minute,
            activeStreak = activeStreak
        )
    }

    /**
     * Bekleyen günlük hatırlatıcı alarmını ve bildirimi iptal eder.
     */
    fun cancelDailyReminder() {
        lastScheduledReminder = null
        val context = appContext ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    companion object {
        const val PREFS_NAME = "kaimon_notification_prefs"
        const val CHANNEL_ID = "kaimon_daily_reminder"
        const val CHANNEL_NAME = "Günlük Hatırlatıcı"
        const val CHANNEL_DESCRIPTION = "Japonca günlük çalışma ve seri koruma hatırlatıcıları"
        const val NOTIFICATION_ID = 1001
        const val ALARM_REQUEST_CODE = 1001
        const val ACTION_DAILY_REMINDER = "com.batuhankeskn0.kaimon.ACTION_DAILY_REMINDER"

        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_BODY = "extra_reminder_body"
        const val EXTRA_STREAK = "extra_reminder_streak"
        const val EXTRA_HOUR = "extra_reminder_hour"
        const val EXTRA_MINUTE = "extra_reminder_minute"

        /**
         * iOS `NotificationManager.swift` satır 58-64 ile birebir aynı bildirim metnini üretir.
         */
        fun buildReminderContent(activeStreak: Int?): ReminderNotificationContent {
            return if (activeStreak != null && activeStreak > 0) {
                ReminderNotificationContent(
                    title = "Serini Kaybetme! 🔥",
                    body = "Tam $activeStreak gündür harika gidiyorsun. Bugün Japonca çalışıp serini korumak için uygulamaya gir!"
                )
            } else {
                ReminderNotificationContent(
                    title = "Çalışma Zamanı!",
                    body = "Japonca hedeflerine bir adım daha yaklaş. Günlük egzersizlerini yapmak için hemen uygulamaya gir! 🎌"
                )
            }
        }

        /**
         * Seçilen `hour` ve `minute` için bir sonraki tetikleme zamanını (epoch ms) hesaplar.
         * Eğer bugünkü saat geçmişse bir sonraki güne kurar.
         */
        fun computeNextTriggerTimeMillis(
            hour: Int,
            minute: Int,
            nowMillis: Long = System.currentTimeMillis(),
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= nowMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val sysManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        ?: return
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESCRIPTION
                }
                sysManager.createNotificationChannel(channel)
            }
        }

        internal fun buildAlarmPendingIntent(
            context: Context,
            hour: Int,
            minute: Int,
            activeStreak: Int,
            title: String,
            body: String
        ): PendingIntent {
            val intent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = ACTION_DAILY_REMINDER
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
                putExtra(EXTRA_STREAK, activeStreak)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        @Volatile
        private var sharedInstance: NotificationManager = NotificationManager()

        val shared: NotificationManager
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }

        internal fun setSharedForTesting(manager: NotificationManager) {
            sharedInstance = manager
        }
    }
}

/**
 * `AlarmManager` tarafından tetiklenen günlük hatırlatıcı `BroadcastReceiver` bileşeni.
 * Bildirimi gösterir ve ertesi gün aynı saat için alarmı yeniden kurar.
 */
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationManager.createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val prefs = context.applicationContext.getSharedPreferences(
            NotificationManager.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val storage = SharedPrefsReminderStorage(prefs)
        val streak = intent?.getIntExtra(
            NotificationManager.EXTRA_STREAK,
            storage.savedActiveStreak
        ) ?: storage.savedActiveStreak

        val fallbackContent = NotificationManager.buildReminderContent(streak)
        val title = intent?.getStringExtra(NotificationManager.EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() } ?: fallbackContent.title
        val body = intent?.getStringExtra(NotificationManager.EXTRA_BODY)
            ?.takeIf { it.isNotBlank() } ?: fallbackContent.body

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(NotificationManager.NOTIFICATION_ID, notification)
        }

        // Ertesi gün için hatırlatıcıyı yeniden kur
        if (storage.isDailyReminderEnabled) {
            val hour = intent?.getIntExtra(NotificationManager.EXTRA_HOUR, storage.savedHour)
                ?: storage.savedHour
            val minute = intent?.getIntExtra(NotificationManager.EXTRA_MINUTE, storage.savedMinute)
                ?: storage.savedMinute
            NotificationManager.shared.scheduleDailyReminder(
                hour = hour,
                minute = minute,
                activeStreak = streak
            )
        }
    }
}
