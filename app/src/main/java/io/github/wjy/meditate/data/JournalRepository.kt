package io.github.wjy.meditate.data

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BlurSettings(
    val enabled: Boolean,
    val implementation: String,
    val intensity: Float
)

@SuppressLint("StaticFieldLeak")
class JournalRepository private constructor(context: Context) {
    // 强制使用 ApplicationContext，确保单例持有的是与进程同生命周期的 Context
    private val appContext = context.applicationContext
    
    val settingsManager = SettingsManager(appContext)
    private var _dao: JournalDao? = null

    suspend fun getDao(): JournalDao {
        return _dao ?: AppDatabase.getDatabase(appContext).journalDao().also { _dao = it }
    }

    /**
     * 合并模糊设置，减少 Combine 参数数量，提升性能
     */
    val blurSettings: Flow<BlurSettings> = combine(
        settingsManager.blurEnabled,
        settingsManager.blurImplementation,
        settingsManager.blurIntensity
    ) { enabled, impl, intensity ->
        BlurSettings(enabled, impl, intensity)
    }

    companion object {
        @Volatile
        private var INSTANCE: JournalRepository? = null

        fun getInstance(context: Context): JournalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JournalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
