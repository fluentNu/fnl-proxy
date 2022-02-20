<template>
  <div>
    <HeadBar></HeadBar>
    <div style="position: absolute;margin-top: 65px;width: 100%;">
      <a-table
          :columns="columns"
          :data-source="data"
          :rowClassName="backColor"
          :pagination="false"
      >
        <span slot="action" slot-scope="text, record">
          <a-button type="primary" v-on:click="showModal(record)">
              设置端口
          </a-button>
          <a-button type="danger" v-on:click="close(record)" style="margin-left: 20px;">
              关闭端口
          </a-button>
        </span>
      </a-table>
      <a-modal
          title="代理设置"
          :visible="visible"
          :confirm-loading="confirmLoading"
          @ok="handleOk"
          @cancel="handleCancel"
          width="370px"
      >
        <a-input placeholder="请输入代理端口号" v-model="port" style="width: 280px;margin: auto"/>
        <br>
        <a-input placeholder="请输入代理描述" v-model="desc" style="width: 280px;margin-top: 20px;"/>
      </a-modal>
    </div>
  </div>
</template>
<script>
import HeadBar from "@/components/HeadBar"
import {url} from '@/path.js'

const columns = [
  {
    title: '主机名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: 'IP',
    dataIndex: 'ip',
    key: 'ip',
  },
  {
    title: '端口',
    key: 'port',
    dataIndex: 'port',
  },
  {
    title: '描述',
    key: 'desc',
    dataIndex: 'desc',
  },
  {
    title: '代理端口',
    key: 'proxyPort',
    dataIndex: 'proxyPort',
  },
  {
    title: '已读字节',
    key: 'readBytes',
    dataIndex: 'readBytes',
  },
  {
    title: '已写字节',
    key: 'writeBytes',
    dataIndex: 'writeBytes',
  },
  {
    title: '连接时长(分)',
    key: 'createTime',
    dataIndex: 'createTime',
  },
  {
    title: '状态',
    key: "status",
    dataIndex: 'status',
  },
  {
    title: '操作',
    key: 'action',
    scopedSlots: {customRender: 'action'},
  },
]
const data = []
export default {
  components: {HeadBar},
  data() {
    return {
      data,
      columns,
      inv: null,
      visible: false,
      confirmLoading: false,
      record: null,
      port: '',
      desc: 'default',
    };
  },
  methods: {
    // 展示对话框
    showModal(re) {
      this.record = re
      this.port = '1' + re.port
      this.visible = true
    },
    // 对话框点击确认
    handleOk() {
      this.confirmLoading = true
      this.create(this.record)
      this.visible = false
      this.confirmLoading = false
    },
    // 关闭对话框
    handleCancel() {
      this.visible = false
    },
    autoReload() {
      // 每3s自动刷新一次状态
      this.inv = setInterval(this.getStatus, 3000)
    },
    // 更新端口状态
    getStatus: function () {
      this.$http.post(url, {
        "method": "select",
      }).then((response) => {
        for (let i = 0; i < response.data.length; i++) {
          response.data[i].createTime = new Date(new Date().getTime() - response.data[i].createTime).getMinutes();
          if (response.data[i].conn.active) {
            response.data[i].status = '已连接'
          } else {
            response.data[i].status = '已断开'
          }
          if (response.data[i].proxyPort === '0') {
            response.data[i].proxyPort = "未配置"
            response.data[i].desc = "无"
          }
        }
        this.data = response.data
      }).catch((response) => {
        console.log('刷新失败', response)
        this.$message.error("获取端口状态异常")
      })
    },
    // 关闭端口代理
    close(e) {
      this.$http.post(url, {
        "method": "delete",
        "params": {
          "targetName": e.name,
          "targetPort": e.port
        }
      }).then(() => {
        this.$message.success('关闭成功')
        this.getStatus()
      }).catch(() => {
        this.$message.error('关闭异常')
        this.getStatus()
      })
    },
    // 设定背景颜色
    backColor(row) {
      if (row.status === '已断开') {
        return 'red'
      } else if (row.proxyPort === '未配置' && row.status === '已连接') {
        return 'yellow'
      } else {
        return 'green'
      }
    },
    // 创建端口代理
    create(e) {
      this.$http.post(url, {
        "method": "create",
        "params": {
          "proxyPort": this.port,
          "targetName": e.name,
          "targetPort": e.port,
          "desc": this.desc,
        }
      }).then(() => {
        this.$message.success("代理端口设置成功")
        this.getStatus()
      }).catch(() => {
        this.$message.error("请求超时")
      })
    },
  },
  created() {
    this.getStatus();
    this.autoReload();
  },
  destroyed: function () {
    clearInterval(this.inv)
  },
};
</script>
<style>
.yellow {
  background-color: yellow;
}

.green {
  background-color: yellowgreen;
}

.red {
  background-color: red;
}
</style>