package utils;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * @Description 实体类，通讯的消息体
 */
@Data
public class Message {
    private int magic;
    private int type;
    private int check;
    private int length;
    private JSONObject info;
    private byte[] data;

    @Override
    public String toString() {
        return "utils.Message{" + "magic=" + magic + ", type=" + type + ", length=" + length + ", data='" + data + '\'' + '}';
    }
}
