# fnl-proxy
一个端口代理程序，你也可以称叫它 “内网穿透”。

server 端使用 netty

client 端使用 golang

web 端使用 Vue + Antd



### 使用方式

1.在client文件夹下的proxy.conf配置文件中填写想要代理的端口号，ip地址和端口号用英文冒号分割

![image-20220220203213824](https://s2.loli.net/2022/02/20/7Kb6Uqd3xGzpuh1.png)

2.启动server端

3.启动client端

4.启动web端

```bash
npm install
npm run serve
```

5.浏览器访问 http://localhost:8080

6.设置代理端口（用16379代理原端口6379）

![image-20220220203447957](https://s2.loli.net/2022/02/20/vCzhsmVytPZ1bl8.png)

7.访问代理端口（我这里代理的是redis）

![image-20220220203610849](https://s2.loli.net/2022/02/20/YNeVLlawZph4GnH.png)

8.代理成功

![image-20220220203706057](https://s2.loli.net/2022/02/20/Lt9JZdC5wsTkSY1.png)



### 几点说明

1.参考自开源项目 [NoBugBoy/EasyProxyPlus](https://github.com/NoBugBoy) 在此基础上进行了魔改，精简了代码设计，增加了一些注释，修改了一点bug，优化了一些用户体验，并没有增添新功能。

2.使用redis作为演示是因为我只成功了redis……其余代理例如mysql和http服务还存在些许bug……
