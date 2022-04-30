package asia.groovelab.blesample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import asia.groovelab.blesample.databinding.ActivityMainBinding
import asia.groovelab.blesample.extension.isDisabled
import asia.groovelab.blesample.extension.toast
import asia.groovelab.blesample.viewmodel.MainViewModel


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_ACCESS_FINE_LOCATION = 1000
        private const val REQUEST_ENABLE_BT = 1001
    }

    // viewModelのインスタンスを取得
    /* lazyは、プロパティの初期化を遅らせる方法の一つ。対象のプロパティが初めて呼び出されたときに初期化され、
    初期化されたら次回以降は必ず同じ値を返す */
    private val viewModel: MainViewModel by lazy {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ViewModelProvider(this, factory).get(MainViewModel::class.java)
    }

    // 画面が生成されたときに呼ばれる
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  setup binding
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        //  setup view model
        viewModel.action.observe(this, Observer { action ->
            when (action) {
                // Centralボタン押下時、CentralActivityを生成する。
                MainViewModel.Action.Central -> Intent(this, CentralActivity::class.java)
                // Peripheralボタン押下時、PeripheralActivityを生成する。
                MainViewModel.Action.Peripheral -> Intent(this, PeripheralActivity::class.java)
                else -> null
            }?.let {
                // 画面遷移する。
                startActivity(it)
            }
        })

        // savedInstanceStateには、Activityのインスタンス状態が含まれている。
        if (savedInstanceState == null) {
            //  BLE利用可能かどうかチェック
            enableBleIfNeed()
        }
    }

    // アプリ初回起動時、ユーザーに位置情報取得の許可を求める。
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    toast("BLEを利用できません")
                }
                return
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // BLEが使用できるかどうか
    private fun enableBleIfNeed() {
        // 位置情報取得権限が許可されているかどうか
        if (!isLocationPermissionsGranted(this)) {
            requestLocationPermission()
        }
        // BLEが使用できるかどうか
        if (!isBleEnabled()) {
            requestBleEnable()
        }
    }

    // 位置情報取得権限を要求
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_ACCESS_FINE_LOCATION
        )
    }

    // BLE利用権限を要求
    private fun requestBleEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    // BLE通信できるか
    private fun isBleEnabled() =
        // BluetoothAdapter.getDefaultAdapter()?.isDisabledがnullの場合、falseを返す。
        BluetoothAdapter.getDefaultAdapter()?.isDisabled ?: false

    // 位置情報取得権限が与えられているか
    private fun isLocationPermissionsGranted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
