import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;

/**
 *  A class for managing multiple threads.  
 *  Created by <a href="http://www.people.fas.harvard.edu/~ekwan/">Eugene E. Kwan</a>.  
 */
public class GeneralThreadService {

    /**
     *  The unique instance of this class.  
     */
    public static final GeneralThreadService INSTANCE = new GeneralThreadService();
    /**
     *  The number of threads available for use on this machine.  
     */
    public final int NUMBER_OF_THREADS         = Runtime.getRuntime().availableProcessors();
    /**
     *  How many jobs can wait in the queue at a time?  
     */
    public static final int JOB_CAPACITY       = 10000000;

    private final CustomThreadPoolExecutor executorService;

    private GeneralThreadService()
    {
        // create a thread pool that has exactly NUMBER_OF_THREADS threads
        // when started, the executor service will create threads up to NUMBER_OF_THREADS
        // these threads will be kept running until the service is shut down
        // if more than JOB_CAPACITY jobs is placed in the queue, then the rejected execution policy will decide what happens
        // the "caller runs policy" returns the excess work to the calling thread

        // threads only created on demand

        executorService = new CustomThreadPoolExecutor(NUMBER_OF_THREADS,                                     // core pool size
                                                       NUMBER_OF_THREADS,                                     // maximum pool size
                                                       1L,                                                    // keep alive time
                                                       TimeUnit.MINUTES,                                      // keep alive time unit
                                                       new ArrayBlockingQueue<Runnable>(JOB_CAPACITY,true),   // work queue
                                                       new CustomThreadFactory("thread pool"),                // thread factory
                                                       new ThreadPoolExecutor.CallerRunsPolicy());            // rejected execution policy
    }

    public CustomThreadPoolExecutor getExecutor()
    {
        return executorService;
    }

    protected static class CustomThreadPoolExecutor extends ThreadPoolExecutor
    {
        public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        ArrayBlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler)
        {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        protected void beforeExecute(Thread t, Runnable r)
        {
            super.beforeExecute(t,r);
        }

        protected void afterExecute(Runnable r, Throwable t)
        {
            super.afterExecute(r,t);
        }
    }

    public String toString()
    {
        return "This is a GeneralThreadService with a " + NUMBER_OF_THREADS + " thread pool with a maximum job capacity of " + JOB_CAPACITY;
    }

    // this creates customized threads
    private static class CustomThreadFactory implements ThreadFactory
    {
        private final String poolName;

        public CustomThreadFactory(String poolName)
        {
            this.poolName = poolName;
        }

        // instead of creating Threads, create WorkerThreads (a descendent of Thread)
        public Thread newThread(Runnable runnable)
        {
            return new WorkerThread(runnable, poolName);
        }
    }

    /**
     *  The subclasses of Thread that will do the calculations.  
     */
    public static class WorkerThread extends Thread
    {
        private static final AtomicInteger created = new AtomicInteger();     // thread safe integer

        public WorkerThread(Runnable runnable, String name)
        {
            super(runnable, "thread " + created.incrementAndGet());
        }

        public void run()
        {
            super.run();
        }
    }
} // end of ThreadService
