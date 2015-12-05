
package rmqess.ch08;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMqManager extends rmqess.ch07.RabbitMqManager
{
    private long reconnectDelaySeconds = 15;

    public RabbitMqManager(final ConnectionFactory factory, final Address[] addresses)
    {
        super(factory, addresses);
    }

    @Override
    protected void asyncWaitAndReconnect()
    {
        executor.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                start();
            }
        }, reconnectDelaySeconds, TimeUnit.SECONDS);
    }

    // exposed for unit testing
    void setReconnectDelaySeconds(final long reconnectDelaySeconds)
    {
        this.reconnectDelaySeconds = reconnectDelaySeconds;
    }

    void setConnection(final Connection connection)
    {
        this.connection = connection;
    }

    ScheduledExecutorService getExecutor()
    {
        return executor;
    }
}
