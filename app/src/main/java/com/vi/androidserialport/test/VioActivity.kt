package com.vi.androidserialport.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.vi.androidserialport.R
import com.vi.vioserial.VioSerial
import com.vi.vioserial.listener.OnConnectListener
import com.vi.vioserial.listener.OnSerialDataListener
import kotlinx.android.synthetic.main.activity_vio.*

/**
 * @author Vi
 * @date 2019-07-18 10:47
 * @e-mail cfop_f2l@163.com
 */

class VioActivity : AppCompatActivity() {

    var isOpenSerial = false //串口是否打开

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var dataStr = msg.obj as String
            dataStr = dataStr.trim { it <= ' ' }.replace(" ".toRegex(), "")
            when (msg.what) {
                0 -> {
                    mTvSendData.append("\n$dataStr")
                }
                else -> {
                    mTvReviData.append("\n$dataStr")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vio)

        initClick()

        mEtCK.setText("/dev/ttyS1")
    }

    override fun onDestroy() {
        super.onDestroy()
        VioSerial.instance().close()
    }

    private fun initClick() {
        //连接/断开串口
        mBtnConnect.setOnClickListener {
            val ck = mEtCK.text.toString()
            val btl = mEtBTL.text.toString()
            if (ck.isBlank() || btl.isBlank()) {
                Toast.makeText(this@VioActivity, "请输入串口和波特率", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOpenSerial) {
                //初始化
                VioSerial.instance().init(VioSerial.SERIAL_101, ck, btl.toInt(), object : OnConnectListener {
                    override fun onSuccess() {
                        isOpenSerial = true
                        mBtnConnect.text = "断开连接"
                        Toast.makeText(this@VioActivity, "串口打开成功", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(errorData: String?) {
                        isOpenSerial = false
                        mBtnConnect.text = "连接串口"
                        Toast.makeText(this@VioActivity, "串口打开失败：$errorData", Toast.LENGTH_SHORT).show()
                    }

                })

                //监听串口数据回调
                VioSerial.instance().setSerialDataListener(object : OnSerialDataListener {
                    override fun onSend(hexData: String?) {
                        val message = Message.obtain()
                        message.what = 0
                        message.obj = hexData
                        mHandler.sendMessage(message)
                    }

                    override fun onReceive(hexData: String?) {

                    }

                    override fun onReceiveFullData(hexData: String?) {
                        val message = Message.obtain()
                        message.what = 1
                        message.obj = hexData
                        mHandler.sendMessage(message)
                    }

                })
            } else {
                isOpenSerial = false
                mBtnConnect.text = "连接串口"
                VioSerial.instance().close()
            }
        }

        //转动
        mBtnTurn.setOnClickListener {
            val ck = mEtInput.text.toString()
            if (ck.isBlank() || ck.length != 3) {
                Toast.makeText(this@VioActivity, "请输入正确的货道号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            VioSerial.instance().openChannel(ck, 0)
        }

        //读取版本号
        mBtnVs.setOnClickListener {
            VioSerial.instance().readVersion()
        }

        //读取结果
        mBtnResult.setOnClickListener {
            VioSerial.instance().readSpring(0)
        }

        mTvSendData.setOnClickListener { mTvSendData.text = "" }
        mTvReviData.setOnClickListener { mTvReviData.text = "" }
    }

}
