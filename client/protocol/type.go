package protocol

//封装了一些状态码
const (
	//Conn 表示用户正在注册欲穿透的端口
	Conn = iota + 1
	// LocalConn 连接本地服务
	LocalConn
	// Data 给本地端口发送数据
	Data
	//Back 回应服务器结果的标识
	Back
	// Ping 服务器->客户端:ping
	Ping
	// Pong 客户端->服务器:pong
	Pong
)
