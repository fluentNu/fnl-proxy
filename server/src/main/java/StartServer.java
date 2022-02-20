import control.HttpServerInitializer;
import core.Keepalive;
import core.NettyServer;
import core.ServerInitializer;
import org.apache.commons.cli.*;

/**
 * @Description 启动类
 */
public class StartServer {
    public static void main(String[] args) throws ParseException {
        //解析命令行的工具
        Options options = new Options();
        //其中 addOption() 方法有三个参数，第一个参数设定这个 option 的单字符名字，第二个参数指明这个 option 是否需要输入数值，第三个参数是对这个 option 的简要描述。
        options.addOption("h", false, "Help");
        //本服务开启的端口
        options.addOption("port", true, "server port");
        //web服务开启的端口
        options.addOption("web", true, "web port");

        //CLI 解析阶段的目标结果就是创建 CommandLine 实例
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            // 这里显示简短的帮助信息
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {
            int port = Integer.parseInt(cmd.getOptionValue("port", "4728"));
            int webPort = Integer.parseInt(cmd.getOptionValue("web", "18888"));
            //是否同步
            boolean sync = cmd.hasOption("sync");
            //开启netty，负责与客户端通信
            NettyServer.start(new ServerInitializer(sync), port);
            System.out.println("服务开启端口为" + (sync ? "(sync)" : "") + port);
            //开启一个线程负责向客户端ping
            new Keepalive().ping();
            //再开启一个netty负责与web通信
            NettyServer.start(new HttpServerInitializer(), webPort);
            System.out.println("web监听端口为" + webPort);
        }
    }
}
