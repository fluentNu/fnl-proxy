package main

import (
	"flag"
	"go-client/config"
	"go-client/service"
)

func main() {
	//这里使用flag包来获取用户命令行输入的参数
	//返回的均为string指针
	config.IP = flag.String("ip", "127.0.0.1", "代理主机IP")
	config.Port = flag.String("port", "4728", "代理主机端口")
	config.Name = flag.String("name", "go-Client", "客户端别名")
	flag.Parse()
	//声明一个指针指向空的TcpClient结构体
	var c = &service.TcpClient{}
	//连接服务器
	conn := service.Conn(*config.IP, *config.Port)
	//client的import方法将proxy.conf配置文件里的端口写入自己的数组里
	c.ImportProxy()
	//向服务器注册代理
	service.InitProxy(*config.Name, conn, c)
	//挂起
	service.Waiting(*config.IP, *config.Port, *config.Name, conn, c)
}
