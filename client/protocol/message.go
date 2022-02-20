package protocol

import (
	"bytes"
	"go-client/utils"
)

// ServerMsg 通信时的消息结构体
type ServerMsg struct {
	Magic  int32  //魔数，用来第一时间判定是否是无效数据包
	Typ    int32  //指令类型，对应protocol包下的type枚举
	Length int32  //长度
	Info   []byte //信息
	Data   []byte //数据
}
type Transfer interface {
	Encode() []byte
	Decode() *ServerMsg
}

// Encode 自己封装编码方式
func (sm *ServerMsg) Encode() []byte {
	//new一个byte类型的缓冲器
	dataBuff := bytes.NewBuffer([]byte{})
	//魔数是4728
	dataBuff.Write(utils.IntToBytes(4728))
	dataBuff.Write(utils.IntToBytes(int(sm.Typ)))
	//计算size
	var size = 0
	if sm.Data != nil {
		size += len(sm.Data)
	}
	if sm.Info != nil {
		size += len(sm.Info)
	}
	if sm.Info != nil {
		dataBuff.Write(utils.IntToBytes(len(sm.Info)))
	} else {
		dataBuff.Write(utils.IntToBytes(0))
	}
	//把长度写入
	dataBuff.Write(utils.IntToBytes(size))
	//写入info
	if sm.Info != nil {
		dataBuff.Write(sm.Info)
	}
	//写入data
	if sm.Data != nil {
		dataBuff.Write(sm.Data)
	}
	return dataBuff.Bytes()
}

// Decode 自己封装解码方式
func (sm *ServerMsg) Decode(b []byte) *ServerMsg {
	//头四位是魔数
	sm.Magic = utils.BytesToInt(b[0:4])
	//检查魔数(4728)
	if sm.Magic == 4728 {
		//继续解析type、length、info和data
		sm.Typ = utils.BytesToInt(b[4:8])
		sm.Length = utils.BytesToInt(b[12:16])
		if sm.Length > 0 {
			sm.Info = b[16 : sm.Length+16]
		}
		sm.Data = b[sm.Length+16:]
	}
	return sm
}
