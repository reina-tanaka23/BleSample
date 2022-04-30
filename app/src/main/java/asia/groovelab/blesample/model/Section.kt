package asia.groovelab.blesample.model

import android.bluetooth.BluetoothGattService
import android.os.Parcel
import android.os.Parcelable

// Sectionのデータを保持するクラス
data class Section(val title: String) : Parcelable {
    constructor(parcel: Parcel) : this(requireNotNull(parcel.readString()))
    constructor(source: BluetoothGattService) : this(source.uuid.toString())


    /*
    Parcelとは、他のプロセスに転送するための、データの固まりのこと。（転送するオブジェクトをParcelに保存する）
     */
    // Parcelに情報を書き込む
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Section> {
        override fun createFromParcel(parcel: Parcel): Section = Section(parcel)
        override fun newArray(size: Int): Array<Section?> = arrayOfNulls(size)
    }
}