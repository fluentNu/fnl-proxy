package core;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Description 实体类
 */
@Data
@ToString
public class GoClientTable implements Serializable {
    private String name;
    private String ip;
    private String port;
    private String desc;
    private Long writeBytes = 0L;
    private Long readBytes = 0L;
    private Long createTime;
    private String proxyPort;
    private Channel conn;
}
