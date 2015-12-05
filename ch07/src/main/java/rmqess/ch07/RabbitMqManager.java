
package rmqess.ch07;

import java.util.Arrays;
import java.util.logging.Level;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMqManager extends rmqess.ch03.RabbitMqManager
{
    private final Address[] addresses;

    public RabbitMqManager(final ConnectionFactory factory, final Address[] addresses)
    {
        super(factory);
        this.addresses = addresses;
    }

    @Override
    public void start()
    {
        try
        {
            connection = factory.newConnection(addresses);
            connection.addShutdownListener(this);
            LOGGER.info("Connected to " + connection.getAddress().getHostName() + ":" + connection.getPort());
            restartSubscriptions();
        }
        catch (final Exception e)
        {
            LOGGER.log(Level.SEVERE, "Failed to connect to " + Arrays.toString(addresses), e);
            asyncWaitAndReconnect();
        }
    }
}
