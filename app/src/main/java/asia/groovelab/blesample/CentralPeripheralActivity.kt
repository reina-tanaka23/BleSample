package asia.groovelab.blesample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View.FOCUSABLE
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.webkit.WebView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import asia.groovelab.blesample.adapter.ItemListAdapter
import asia.groovelab.blesample.coroutine.LifecycleScope
import asia.groovelab.blesample.coroutine.LifecycleScopeSupport
import asia.groovelab.blesample.databinding.ActivityCentralPeripheralBinding
import asia.groovelab.blesample.extension.toHexString
import asia.groovelab.blesample.extension.toast
import asia.groovelab.blesample.model.Item
import asia.groovelab.blesample.model.Peripheral
import asia.groovelab.blesample.viewmodel.CentralPeripheralViewModel
import kotlinx.coroutines.launch
import java.util.*


class CentralPeripheralActivity : AppCompatActivity(), LifecycleScopeSupport {
    companion object {
        private const val PERIPHERAL_EXTRA = "peripheral"

        // CentralPeripheralActivityを生成する。
        fun intent(context: Context, peripheral: Peripheral) =
            Intent(context, CentralPeripheralActivity::class.java).putExtra(
                PERIPHERAL_EXTRA,
                peripheral
            )
    }

    override val scope = LifecycleScope(this)
    private lateinit var webView: WebView

    // viewModelのインスタンスを取得
    /* lazyは、プロパティの初期化を遅らせる方法の一つ。対象のプロパティが初めて呼び出されたときに初期化され、
    初期化されたら次回以降は必ず同じ値を返す */
    private val viewModel: CentralPeripheralViewModel by lazy {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ViewModelProvider(this, factory).get(CentralPeripheralViewModel::class.java)
    }

    // 画面が生成されたときに呼ばれる
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  setup binding
        val binding: ActivityCentralPeripheralBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_central_peripheral
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.listView.setAdapter(
            ItemListAdapter(this)
        )
        binding.listView.setOnGroupClickListener { _, _, _, _ -> true }
        binding.listView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            viewModel.getItem(groupPosition, childPosition)?.let {
                if (it.isWritable && it.isReadable) {
//                    showAlertDialogForReadWrite(it)
                    // 読み込み・書き込み可能な場合
                    reloadingFunc(it)
                } else if (it.isWritable) {
                    // 書き込み可能な場合
                    showAlertDialogForWrite(it)
                } else if (it.isReadable) {
                    // 読み込み可能な場合
                    viewModel.readCharacteristic(it)
                }
            }
            true
        }
        // 再接続を検知
        viewModel.reconnected.observe(this, Observer {
            scope.launch {
                toast(getString(R.string.connect_again))
            }
        })
        // 切断を検知
        viewModel.disconnectedFromDevice.observe(this, Observer {
            scope.launch {
                toast(getString(R.string.disconnected))
                finish()
            }
        })
        // 書き込みを検知
        viewModel.wroteCharacteristicHandler = { _, _ ->
            scope.launch {
                toast("success to write")
            }
        }
        // データ受信を検知
        viewModel.notifiedCharacteristicHandler = { uuid, bytes ->
            scope.launch {
                bytes?.let { toast("notified\n${uuid}:${it.toHexString()}") }
            }
        }
        webView = findViewById<WebView>(R.id.mWebV)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://www.google.co.jp/maps/?hl=ja")
        //  toolbarを設置
        setSupportActionBar(binding.toolbar)

        //  initialize data（savedInstanceStateには、Activityのインスタンス状態が含まれている。）
        if (savedInstanceState == null) {
            (intent.getParcelableExtra(PERIPHERAL_EXTRA) as? Peripheral)?.also {
                viewModel.connect(it)
            }
        }
    }

    // メニューバーを生成
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_central_peripheral, menu)
        return true
    }

    // メニューバー内の項目をタップした時に実行される
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.disconnect_button -> {
                disconnectedAndDismiss()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 端末のバックキー押下時に実行される
    override fun onBackPressed() {
        disconnectedAndDismiss()
    }

    private fun disconnectedAndDismiss() {
        viewModel.disconnect {
            scope.launch {
                finish()
            }
        }
    }

    // 取得したBLEデータの読み込み
    private fun reloadingFunc(item: Item) {
        // 読み込み
        viewModel.readCharacteristic(item)
        // 最終実現したいのは、BLE情報を取得した位置の情報を取得して、点々のピンにしたい（追跡できるよう）
//      var str = item.readValue
        var str = "0x33342e393835343830372c3133372e303033343335382c3130" //TTC
        var output = ""
        var i = 0
        if (str != null) {
            val len = str.length
            str = str.takeLast(len - 2)
            while (i < str.length) {
                val strForOut = str.substring(i, i + 2)
                output += strForOut.toInt(16).toChar()
                i += 2
            }
            val array = output.split(",")
//            if (array != null ) {
                webView.loadUrl("https://www.google.co.jp/maps?q=" + array[0] + "," + array[1])
//              webView.loadUrl("https://map.yahoo.co.jp/place?lat=" + array[0] + "&lon=" + array[1])   // ブラウザーで表示される
//              webView.loadUrl("https://mapfan.com/map/spots/search?c=" + array[0] + "," + array[1] + "&s=std,pc,ja&p=none")  //　サイトの上部のみ表示。地図部分は表示されない。
//              webView.loadUrl("https://mapfan.com/map/spots/search?c=" + array[0] + "," + array[1] + "&s=std,pc,ja&p=none")  //　サイトの上部のみ表示。地図部分は表示されない。
//              webView.loadUrl("https://www.its-mo.com/maps/address/23223010124000000000200009/?lat=" + array[0] + "&lon=" + array[1] + "&from=map")  //　サイトの左部のみ表示。地図部分は表示されない。
//              webView.loadUrl("https://maps.gsi.go.jp/#18/" + array[0] + "/" + array[1] + "/&base=std&ls=std&disp=1&vs=c1j0h0k0l0u0t0z0r0s0m0f1")  //　表示されない。
//              webView.loadUrl("https://www.mapion.co.jp/m2/" + array[0] + "," + array[1] + ",18")  //　ブラウザーで表示される
//              webView.loadUrl("https://map.goo.ne.jp/map/search/latlon/E" + array[1] + "N" + array[0] + "/q/zoom/12/")  //　狙った場所が表示されない。広告が邪魔
//            }
        }
        else{
            //暫定
            //地図を更新しない
        }
    }
    // 不要な関数？
    // BLE情報を取得したときに、いちいちダイアログが表示されてしまう。
//    private fun showAlertDialogForReadWrite(item: Item) {
//
//        AlertDialog.Builder(this)
//            .setItems(arrayOf("Read", "Cancel")) { _, index ->
//                when (index) {
//                    0 -> reloadingFunc(item)
//                }
//            }
//            .show()
//        reloadingFunc(item)
//    }

    // 書き込み関数
    private fun showAlertDialogForWrite(item: Item) {
        val inputFilter = InputFilter { source, _, _, _, _, _ ->
            val hexChars = listOf(
                '0',
                '1',
                '2',
                '3',
                '4',
                '5',
                '6',
                '7',
                '8',
                '9',
                'A',
                'B',
                'C',
                'D',
                'E',
                'F'
            )
            source.toString()
                .toUpperCase(Locale.ROOT)
                .filter { hexChars.contains(it) }
        }
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            filters = arrayOf(inputFilter)
            hint = "input hex"
            focusable = FOCUSABLE
        }

        // アラートダイアログ表示の設定
        AlertDialog.Builder(this)
            .setMessage("Write to [${item.uuid}]")
            .setView(editText)
            .setPositiveButton("Write") { _, _ ->
                viewModel.writeCharacteristic(item, editText.text.toString())
            }
            .setNeutralButton("Cancel") { _, _ -> }
            .create().apply {
                setOnShowListener {
                    editText.requestFocus()
                }
                editText.onFocusChangeListener = OnFocusChangeListener { _, _ ->
                    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
            .show()
    }
}