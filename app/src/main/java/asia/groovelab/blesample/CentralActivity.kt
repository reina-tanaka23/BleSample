package asia.groovelab.blesample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import asia.groovelab.blesample.adapter.PeripheralListAdapter
import asia.groovelab.blesample.databinding.ActivityCentralBinding
import asia.groovelab.blesample.extension.isDisabled
import asia.groovelab.blesample.extension.toast
import asia.groovelab.blesample.viewmodel.CentralViewModel


class CentralActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_ACCESS_FINE_LOCATION = 1000
        private const val REQUEST_ENABLE_BT = 1001
    }

    // viewModelのインスタンスを取得
    /* lazyは、プロパティの初期化を遅らせる方法の一つ。対象のプロパティが初めて呼び出されたときに初期化され、
    初期化されたら次回以降は必ず同じ値を返す */
    private val viewModel: CentralViewModel by lazy {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ViewModelProvider(this, factory).get(CentralViewModel::class.java)
    }

    // 画面が生成されたときに呼ばれる
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  setup binding
        val binding: ActivityCentralBinding = DataBindingUtil.setContentView(this, R.layout.activity_central)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.listView.adapter = PeripheralListAdapter(this)
        binding.listView.setOnItemClickListener { _, _, position, _ ->
            viewModel.getPeripheral(position)?.let {
                // CentralPeripheralActivityを生成し、画面遷移する。
                startActivity(CentralPeripheralActivity.intent(this, it))
            }
        }
        // isScanFailedを監視し、Scanできない場合は、トースト表示する。
        viewModel.failedToScan.observe(this, Observer { isScanFailed ->
            if (isScanFailed) {
                toast(getString(R.string.error_scan))
            }
        })

        //  toolbar（Sort, ReScanメニューボタンを表示）
        setSupportActionBar(binding.toolbar)

        // savedInstanceStateには、Activityのインスタンス状態が含まれている。
        if (savedInstanceState == null) {
            //  BLE利用のため、BLEを利用できるかどうかチェックする。
            enableBleIfNeed()
            viewModel.startToScan()
        }
    }

    // 画面が（バックグラウンドから）再表示されたときに呼ばれる
    override fun onRestart() {
        super.onRestart()

        viewModel.reScan()
    }

    // 画面が見えなくなったときに呼ばれる
    override fun onStop() {
        super.onStop()

        viewModel.stopToScan()
    }

    // メニューバーを生成する
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_central, menu)
        return true
    }

    // メニューバー内の項目をタップした時に実行される
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // ReScanボタン押下時の処理
            R.id.scan_button -> {
                viewModel.reScan()
                true
            }
            // Sortボタン押下時の処理
            R.id.sort_button -> {
                viewModel.sortPeripherals()
                true
            }
            // listに表示された項目を押下時の処理
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 位置情報取得を許可するかについての、ダイアログの結果を受け取る関数
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        // 位置情報取得の権限が許可されているか
        when (requestCode) {
            // 位置情報取得の権限が許可されている場合、トーストを表示する。
            REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    toast(getString(R.string.error_start_to_scan))
                }
                return
            }
            // 位置情報取得の権限が許可されていない場合、権限許可を要求する。
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun enableBleIfNeed() {
        //　位置情報取得権限が与えられていなければ、位置情報（GPS）取得の許可を求める。
        if (!isLocationPermissionsGranted(this)) {
            requestLocationPermission()
        }
        // BLEが利用できなければ、BLEを利用できるよう要求する。
        if (!isBleEnabled()) {
            requestBleEnable()
        }
    }

    // GPS取得の許可を求める。
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_ACCESS_FINE_LOCATION
        )
    }

    // BLEの接続要求のダイアログを表示する。
    private fun requestBleEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    // BluetoothAdapterオブジェクトを取得できるか判定する。
    private fun isBleEnabled() =
        BluetoothAdapter.getDefaultAdapter()?.isDisabled ?: false

    // GPS情報取得が許可されているかどうかを判定する。
    private fun isLocationPermissionsGranted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
