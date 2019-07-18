package com.vi.androidserialport

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.vi.vioserial.NormalSerial
import com.vi.vioserial.listener.OnConnectListener
import com.vi.vioserial.listener.OnNormalDataListener
import kotlinx.android.synthetic.main.activity_main.*

/**
 * 快速使用
 * @author Vi
 * @date 2019-07-17 16:50
 * @e-mail cfop_f2l@163.com
 */
class MainActivity : AppCompatActivity(), OnNormalDataListener {

    var isOpenSerial = false //串口是否打开

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initClick()

    }

    override fun onDestroy() {
        super.onDestroy()
        NormalSerial.instance().removeDataListener(this)
        NormalSerial.instance().close()
    }

    private fun initClick() {
        //连接/断开串口
        mBtnConnect.setOnClickListener {
            val ck = mEtCK.text.toString()
            val btl = mEtBTL.text.toString()
            if (ck.isBlank() || btl.isBlank()) {
                Toast.makeText(this@MainActivity, "请输入串口和波特率", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOpenSerial) {
                //初始化
                NormalSerial.instance().init(ck, btl.toInt(), object : OnConnectListener {
                    override fun onSuccess() {
                        isOpenSerial = true
                        mBtnConnect.text = "断开连接"
                        Toast.makeText(this@MainActivity, "串口打开成功", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(errorData: String?) {
                        isOpenSerial = false
                        mBtnConnect.text = "连接串口"
                        Toast.makeText(this@MainActivity, "串口打开失败：$errorData", Toast.LENGTH_SHORT).show()
                    }

                })

                //添加数据接收回调
                NormalSerial.instance().addDataListener(this)
            } else {
                isOpenSerial = false
                mBtnConnect.text = "连接串口"
                NormalSerial.instance().close()
            }
        }

        //往串口发送数据
        mBtnSend.setOnClickListener {
            val input = mEtInput.text.toString()
            if (input.isBlank()) {
                Toast.makeText(this@MainActivity, "发送内容不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOpenSerial) {
                Toast.makeText(this@MainActivity, "串口还未成功打开", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            NormalSerial.instance().sendData(input)
            mTvSendData.append("\n$input")
        }

        mTvSendData.setOnClickListener { mTvSendData.text = "" }
        mTvReviData.setOnClickListener { mTvReviData.text = "" }
    }

    /**
     * 串口返回数据
     */
    override fun normalDataBack(data: String?) {
        mTvReviData.append("\n$data")
    }

}
