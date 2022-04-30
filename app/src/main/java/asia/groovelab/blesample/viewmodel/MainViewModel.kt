package asia.groovelab.blesample.viewmodel

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// MainActivityとデータとの架け橋を担うクラス
class MainViewModel(val app: Application): AndroidViewModel(app) {
    // ユーザーのアクションの種類を定義
    enum class Action {
        Central,
        Peripheral,
        None
    }

    private val mAction = MutableLiveData<Action>()
    val action: LiveData<Action> = mAction

    init {
        mAction.value = Action.None
    }

    // ViewModelにクリックメソッドを定義していることを意味する。
    @Suppress("UNUSED_PARAMETER")
    fun onClickCentralButton(view: View) {
        mAction.postValue(Action.Central)
    }

    // ViewModelにクリックメソッドを定義していることを意味する。
    @Suppress("UNUSED_PARAMETER")
    fun onClickPeripheralButton(view: View) {
        mAction.postValue(Action.Peripheral)
    }
}