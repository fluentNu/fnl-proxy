package service

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"go-client/config"
	"go-client/protocol"
	"io"
	"io/ioutil"
	"net"
	"os"
	"strconv"
	"strings"
	"time"
)

type TcpClient struct {
	ProxyArray []*Proxy //存放每个代理的连接，因为可能映射多个端口，所以使用数组
}

// Proxy 代理类
type Proxy struct {
	IP         string   //ip地址
	Port       string   //端口号
	TargetName string   //客户端别名
	Conn       net.Conn //句柄1，负责客户端与本地穿透端口的连接
	ServerConn net.Conn //句柄2，负责客户端与服务器的连接
}

// ImportProxy 导入欲穿透的端口
func (cli *TcpClient) ImportProxy() {
	//获取当前工作目录
	path, err := os.Getwd()
	if err != nil {
		fmt.Println("获取当前路径错误：", err)
		return
	}
	//读取同级目录下的proxy.conf配置文件
	file, err := ioutil.ReadFile(path + "/proxy.conf")
	if err != nil {
		fmt.Println("读取配置文件错误：", err)
		return
	}
	//用scan读取配置文件
	scanner := bufio.NewScanner(bytes.NewReader(file))
	for scanner.Scan() {
		if scanner.Text() != "" {
			//key和value之间用英文冒号 ':' 分割
			s := strings.Split(scanner.Text(), ":")
			if len(s) == 2 {
				cli.ProxyArray = append(cli.ProxyArray, &Proxy{
					IP:   s[0],
					Port: s[1],
				})
			}
		}
	}
}

// ProxyData 服务器发送的数据
type ProxyData struct {
	Port       string `json:"port"`
	TargetName string `json:"targetName"`
}

// ListenLocal 监听本地服务，返回给服务端数据
func (pro *Proxy) ListenLocal() {
	//为了防止长时间无响应的情况,设置读写超时
	_ = pro.Conn.SetReadDeadline(time.Now().Add(time.Second * 5))
	logger := config.InitLogger("receive")
	defer logger.Sync()
	for {
		//下面一段都是重复代码
		buf := make([]byte, 10240)
		n, err := pro.Conn.Read(buf)
		if err != nil {
			if err == io.EOF {
				//这个Conn是与本地服务连接，所以pro.IP其实就是127.0.0.1
				pro.Conn = Conn(pro.IP, pro.Port)
				continue
			}
			_ = pro.Conn.SetReadDeadline(time.Now().Add(time.Second * 3))
			continue
		}
		//data是写给服务器的数据,最后是info
		data := &ProxyData{
			Port:       pro.Port,
			TargetName: pro.TargetName,
		}
		//结构体转json
		jsonObj, err := json.Marshal(data)
		if err != nil {
			fmt.Println(err)
		}
		port, _ := strconv.Atoi(*config.Port)
		sm := &protocol.ServerMsg{
			Magic: int32(port),
			Typ:   protocol.Back,
			Info:  jsonObj,
			//buf是函数开头接收的Read
			Data: buf[:n],
		}
		fmt.Println("收到本地客户端数据：")
		fmt.Println(string(sm.Data))
		logger.Info(fmt.Sprintf("收到本地客户端数据：%s", string(sm.Data)))
		_, err = pro.ServerConn.Write(sm.Encode())
		if err != nil {
			fmt.Println(err)
		}
	}
}

func (pro *Proxy) Send(data []byte) {
	logger := config.InitLogger("send")
	if pro.Conn == nil {
		fmt.Println("等待本地端口", pro.Port, "应答...")
		return
	}
	fmt.Println("向本地客户端发送：")
	fmt.Println(string(data))
	logger.Info(fmt.Sprintf("向本地客户端发送：%s", string(data)))
	_, err := pro.Conn.Write(data)
	if err != nil {
		fmt.Println("调用本地服务失败：", err)
		fmt.Println("尝试重新连接")
		pro.Conn, err = net.Dial("tcp4", pro.Conn.RemoteAddr().String())
		if err != nil {
			fmt.Println("重新连接失败")
		}
	}
}
