package asia.groovelab.blesample.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart

interface LifecycleScopeSupport {
    val scope: LifecycleScope

    // 使われていないので、不要かと
    fun launch(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ) = scope.bindLaunch(start, block)
}
