package utils;

/**
 * @Description 状态码
 */
public interface Status {
    int conn = 1;       //表示客户端正在注册欲穿透的端口
    int localConn = 2;  //表示客户端连接本地服务
    int data = 3;       //给本地端口发送数据
    int back = 4;       //回应服务器结果
    int ping = 5;       //服务器->客户端:ping
    int pong = 6;       //客户端->服务器:pong
}
