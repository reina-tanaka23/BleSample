package asia.groovelab.blesample

import android.os.Bundle
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import asia.groovelab.blesample.coroutine.LifecycleScope
import asia.groovelab.blesample.coroutine.LifecycleScopeSupport
import asia.groovelab.blesample.databinding.ActivityPeripheralBinding
import asia.groovelab.blesample.extension.toast
import asia.groovelab.blesample.viewmodel.PeripheralViewModel


class PeripheralActivity : AppCompatActivity(), LifecycleScopeSupport {
    override val scope = LifecycleScope(this)

    // viewModelのインスタンスを取得
    /* lazyは、プロパティの初期化を遅らせる方法の一つ。対象のプロパティが初めて呼び出されたときに初期化され、
    初期化されたら次回以降は必ず同じ値を返す */
    private val viewModel: PeripheralViewModel by lazy {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ViewModelProvider(this, factory).get(PeripheralViewModel::class.java)
    }
    // ↓使われていないので、不要かと
    private var scrollView: ScrollView? = null

    // 画面が生成されたときに呼ばれる
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  setup binding
        val binding: ActivityPeripheralBinding = DataBindingUtil.setContentView(this, R.layout.activity_peripheral)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        //  toolbarを設定
        setSupportActionBar(binding.toolbar)

        //  initialize data（savedInstanceStateには、Activityのインスタンス状態が含まれている。）
        if (savedInstanceState == null) {
            if (!viewModel.canAdvertise) {
                toast("can not start peripheral")
            }
        }
    }

    // 端末のバックキー押下時に実行される。
    override fun onBackPressed() {
        viewModel.clear()
        super.onBackPressed()
    }
}