
package rmqess.ch03;

import static java.util.Collections.synchronizedSet;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.inject.Named;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

@Named
public class RabbitMqManager extends rmqess.ch02.RabbitMqManager
{
    private final Set<Subscription> subscriptions;

    public RabbitMqManager(final ConnectionFactory factory)
    {
        super(factory);
        subscriptions = synchronizedSet(new HashSet<Subscription>());
    }

    @Override
    public void start()
    {
        try
        {
            connection = factory.newConnection();
            connection.addShutdownListener(this);
            LOGGER.info("Connected to " + factory.getHost() + ":" + factory.getPort());
            restartSubscriptions();
        }
        catch (final Exception e)
        {
            LOGGER.log(Level.SEVERE, "Failed to connect to " + factory.getHost() + ":" + factory.getPort(), e);
            asyncWaitAndReconnect();
        }
    }

    protected void restartSubscriptions()
    {
        LOGGER.info("Restarting " + subscriptions.size() + " subscriptions");

        for (final Subscription subscription : subscriptions)
        {
            startSubscription(subscription);
        }
    }

    public Subscription createSubscription(final String queue, final SubscriptionDeliveryHandler handler)
    {
        final Subscription subscription = new Subscription(queue, handler);
        subscriptions.add(subscription);
        startSubscription(subscription);
        return subscription;
    }

    private void startSubscription(final Subscription subscription)
    {
        final Channel channel = createChannel();

        if (channel != null)
        {
            try
            {
                subscription.start(channel);
            }
            catch (final Exception e)
            {
                LOGGER.log(Level.SEVERE, "Failed to start subscription: " + subscription + " on channel: "
                                         + channel, e);
            }
        }
    }
}
