package io.github.wjy.meditate.data

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

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
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    // 🚀 性能优化：序列化单例。配置缓存解析器，开启 ignoreUnknownKeys 增强鲁棒性。
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun getDao(): JournalDao {
        return _dao ?: AppDatabase.getDatabase(appContext).journalDao().also { _dao = it }
    }

    /**
     * 提供一个响应式 DAO 流，当数据库切换时会自动发射新的 DAO
     */
    fun getDaoFlow(): Flow<JournalDao> = _refreshTrigger.map {
        getDao()
    }

    /**
     * 清除缓存的 DAO 实例，并触发所有监听 DAO 的流重新加载
     */
    fun refresh() {
        _dao = null
        _refreshTrigger.tryEmit(Unit)
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
