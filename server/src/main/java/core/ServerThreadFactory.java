package core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerThreadFactory implements ThreadFactory {
    private final String groupName;

    //构造器
    public ServerThreadFactory(String groupName) {
        this.groupName = groupName;
    }

    //++i和i++操作并不是线程安全的,所以使用线程安全的AtomicInteger
    //由于其是由硬件提供原子操作指令实现的。在非激烈竞争的情况下，开销更小，速度更快
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    //实现ThreadFactory接口
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(groupName + "-" + threadNumber.incrementAndGet());
        return thread;
    }
}
