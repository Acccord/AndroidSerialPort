package com.vi.vioserial.impl;

import com.vi.vioserial.bean.*;
import com.vi.vioserial.listener.OnSerialDataParse;
import com.vi.vioserial.listener.OnVioDataListener;
import com.vi.vioserial.util.SerialDataUtils;

import java.util.List;

/**
 * @author Vi
 * @date 2019-07-17 17:30
 * @e-mail cfop_f2l@163.com
 */

public class Data427ReviImpl implements OnSerialDataParse {

    @Override
    public void parseData(String data, List<OnVioDataListener> mVioDataListener) {
        if (!data.contains("232A") && !data.contains("0D0A")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).error("串口数据读取不完整，请检查是否有其他程序占用该串口！");
            }
            return;
        }

        String dataStr = SerialDataUtils.hexStringToString(data);

        if (dataStr.contains("Version")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).version(parseVersion(dataStr));
            }
        } else if (dataStr.contains("sensor")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).springResult(parseLight(dataStr));
            }
        } else if (dataStr.contains("ChannelopenStatus")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).springResult(parseSpring(dataStr));
            }
        }

        if (dataStr.contains("CoinErr")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).coin(parseCoin(dataStr));
            }
        } else if (dataStr.contains("billErr")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).bill(parseBill(dataStr));
            }
        } else if (dataStr.contains("Coinmoney")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).money(parseMoney(dataStr));
            }
        } else if (dataStr.contains("MDBCoinReturn")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).returnCoin(parseRest(dataStr));
            }
        }
    }

    private DataVersion parseVersion(String dataStr) {
        dataStr = dataStr.trim();
        DataVersion versionBean = new DataVersion();

        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("Version")) {
                String version;
                if (s.contains("=")) {
                    version = s.substring(s.indexOf("=") + 1);
                } else {
                    version = "0";
                }
                versionBean.setVersion(version);
            }
            if (s.contains("Testmode")) {
                String statusStr = s.replaceAll("[^0-9]", "");
                int model = Integer.parseInt(statusStr);
                versionBean.setTestmode(model);
            }
        }
        return versionBean;
    }

    private DataStatus parseLight(String dataStr) {
        dataStr = dataStr.trim();

        int lightStatus = 0;
        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("sensor")) {
                String statusStr = s.replaceAll("[^0-9]", "");
                lightStatus = Integer.parseInt(statusStr);
            }
        }

        if (lightStatus != 1) {
            lightStatus = 0;
        }

        DataStatus dataStatus = new DataStatus();
        dataStatus.setLightResult(lightStatus);
        dataStatus.setChannelResult(0);
        return dataStatus;
    }

    private DataStatus parseSpring(String dataStr) {
        dataStr = dataStr.trim();
        int springStatus = 9;
        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("ChannelopenStatus")) {
                String statusStr = s.replaceAll("[^0-9]", "");
                springStatus = Integer.parseInt(statusStr);
                switch (springStatus) {
                    case 2:
                    case 21:
                    case 22:
                    case 23:
                        springStatus = 6;
                        break;
                    case 3:
                        springStatus = 0;
                        break;
                    case 1:
                    default:
                        springStatus = 9;
                        break;
                }
            }
        }

        DataStatus dataStatus = new DataStatus();
        dataStatus.setChannelResult(springStatus);
        dataStatus.setLightResult(0);
        return dataStatus;
    }

    private CoinBean parseCoin(String dataStr) {
        dataStr = dataStr.trim();
        CoinBean coinBean = new CoinBean();
        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("Coin50")) {
                coinBean.setCoin50(Integer.parseInt(s.substring(7)));
            }
            if (s.contains("Coin100")) {
                coinBean.setCoin100(Integer.parseInt(s.substring(8)));
            }
            if (s.contains("CoinErr")) {
                coinBean.setCoinErr(Integer.parseInt(s.substring(8)));
            }
        }
        return coinBean;
    }

    private BillBean parseBill(String dataStr) {
        dataStr = dataStr.trim();
        BillBean billBean = new BillBean();
        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("Billcapacity")) {
                billBean.setBillcapacity(Integer.parseInt(s.substring(13)));
            }
            if (s.contains("MDBReturnN")) {
                billBean.setMDBReturnN(Integer.parseInt(s.substring(11)));
            }
            if (s.contains("Billfull")) {
                billBean.setBillfull(Integer.parseInt(s.substring(9)));
            }
            if (s.contains("billErr")) {
                billBean.setBillErr(Integer.parseInt(s.substring(8)));
            }
        }
        return billBean;
    }

    private MoneyBean parseMoney(String dataStr) {
        dataStr = dataStr.trim();
        MoneyBean moneyBean = new MoneyBean();
        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("MDBcoin")) {
                moneyBean.setMDBcoin(Integer.parseInt(s.substring(8)));
            }
            if (s.contains("MDBbill")) {
                moneyBean.setMDBbill(Integer.parseInt(s.substring(8)));
            }
            if (s.contains("Coinmoney")) {
                moneyBean.setCoinmoney(Integer.parseInt(s.substring(10)));
            }
            if (s.contains("billmoney")) {
                moneyBean.setBillmoney(Integer.parseInt(s.substring(10)));
            }
            if (s.contains("BillEscrow")) {
                moneyBean.setBillEscrow(Integer.parseInt(s.substring(11)));
            }
        }
        return moneyBean;
    }

    private RestBean parseRest(String dataStr) {
        RestBean mdbCoinReturn = new RestBean();
        String str[] = dataStr.split(",");
        for (String s : str) {
            s = s.trim();
            if (s.contains("MDBCoinReturn")) {
                mdbCoinReturn.setMDBCoinReturn(Integer.parseInt(s.substring(14)));
            }
            if (s.contains("MDBBillReturn")) {
                mdbCoinReturn.setMDBBillReturn(Integer.parseInt(s.substring(14)));
            }
        }
        return mdbCoinReturn;
    }

}
