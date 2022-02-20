package service

import (
	"encoding/json"
	"fmt"
	"go-client/config"
	"go-client/protocol"
	"io"
	"net"
	"strconv"
	"time"
)

type register struct {
	Name string
	Port string
	IP   string
}

// Conn 连接远程服务器,返回句柄
func Conn(ip, port string) net.Conn {
	//JoinHostPort函数将host和port合并为一个网络地址
	//一般格式为"host:port";如果host含有冒号或百分号,格式为"[host]:port"
	//Ipv6的文字地址或者主机名必须用方括号括起来,如"[::1]:80"/"[ipv6-host]:http"/"[ipv6-host%zone]:80"
	target := net.JoinHostPort(ip, port)
	//远程连接服务器，得到一个句柄
	conn, err := net.Dial("tcp4", target)
	for err != nil {
		fmt.Printf("连接%s:%s失败：%s \n正在尝试重连...\n", ip, port, err)
		time.Sleep(10 * time.Second)
		conn, err = net.Dial("tcp4", target)
	}
	fmt.Printf("连接%s:%s成功\n", ip, port)
	return conn
}

// InitProxy 向服务器注册代理
func InitProxy(targetName string, conn net.Conn, client *TcpClient) {
	//遍历那个Proxy数组
	for _, proxy := range client.ProxyArray {
		//把用户指定的客户端别名赋予他们
		proxy.TargetName = targetName
		//conn是与服务器连接的句柄
		proxy.ServerConn = conn
		reg := &register{
			Name: proxy.TargetName,
			Port: proxy.Port,
			IP:   proxy.IP,
		}
		//对象转JSON
		jsonObj, _ := json.Marshal(reg)
		//string转int，待会再转int32
		port, _ := strconv.Atoi(*config.Port)
		sm := &protocol.ServerMsg{
			Magic: int32(port),
			Typ:   protocol.Conn,
			Info:  jsonObj,
		}
		_, err := conn.Write(sm.Encode())
		if err != nil {
			fmt.Println("向服务器注册端口错误：", err)
			continue
		} else {
			fmt.Println("成功注册了", proxy.IP, "的", proxy.Port, "端口")
		}
	}
	//数组全部处理完毕，结束
	fmt.Println("服务器注册完毕，共计", len(client.ProxyArray), "个端口")
}

func Waiting(ip, port, targetName string, conn net.Conn, list *TcpClient) {
	for {
		//开10M缓存
		buf := make([]byte, 10240)
		//读取服务器返回的数据，如果没有就阻塞
		offset, err := conn.Read(buf)
		if err != nil {
			//判断EOF
			if err == io.EOF {
				fmt.Println("与服务器断开连接，尝试重连...")
				for {
					//不断尝试重连
					conn = Conn(ip, port)
					if conn != nil {
						InitProxy(targetName, conn, list)
						break
					}
					//等待3s后再重连
					time.Sleep(3 * time.Second)
				}
			}
			//如果不是io.EOF错误，那么继续下一次循环
			continue
		}
		//offset>12表明读到了完整的消息
		if offset > 12 {
			//开辟内存
			sm := &protocol.ServerMsg{}
			//解码,[]byte转结构体
			s := sm.Decode(buf[:offset])
			//如果类型为ping
			//fmt.Println(string(s.Data))
			if s.Typ == protocol.Ping {
				//回应pong
				pong(targetName, conn)
				//如果只是为了ping一下保持连接，那么就结束吧
				continue
			}
			//pd用来存储解析后的服务器发来的数据
			pd := &ProxyData{}
			if s.Info != nil {
				err := json.Unmarshal(s.Info, pd)
				if err != nil {
					panic(err)
				}
			}
			for _, data := range list.ProxyArray {
				//寻找数据是针对哪一个端口的
				if data.Port == pd.Port {
					switch s.Typ {
					//本地连接请求
					case protocol.LocalConn:
						if data.Conn == nil {
							//连接本地服务
							go LocalConn(data)
						} else {
							//监听本地服务，返回给服务端数据
							go data.ListenLocal()
						}
						//有数据转发（本地业务处理）
					case protocol.Data:
						if data.Conn == nil {
							LocalConn(data)
						}
						data.Send(s.Data)
					}
				}
			}
		}
	}
}

//回应客户端发来的ping
func pong(targetName string, conn net.Conn) {
	fmt.Println("收到来自服务器的ping...")
	data := &ProxyData{
		//还有一个port,这里不重要了
		TargetName: targetName,
	}
	jsonObj, err := json.Marshal(data)
	if err != nil {
		panic(err)
	}
	sm := &protocol.ServerMsg{
		Typ:  protocol.Pong,
		Info: jsonObj,
	}
	_, _ = conn.Write(sm.Encode())
}

// LocalConn 连接本地服务
func LocalConn(data *Proxy) {
	for {
		fmt.Println("开始连接本地服务")
		clientConn := Conn(data.IP, data.Port)
		if clientConn != nil {
			data.Conn = clientConn
			//监听本地服务，返回给服务端数据
			data.ListenLocal()
			break
		} else {
			time.Sleep(1 * time.Second)
		}
	}
}
