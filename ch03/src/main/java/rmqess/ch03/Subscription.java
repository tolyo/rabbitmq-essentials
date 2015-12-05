
package rmqess.ch03;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Subscription
{
    private final static Logger LOGGER = Logger.getLogger(Subscription.class.getName());

    private final String queue;
    private final SubscriptionDeliveryHandler handler;

    private volatile DefaultConsumer consumer;

    public Subscription(final String queue, final SubscriptionDeliveryHandler handler)
    {
        this.queue = queue;
        this.handler = handler;
    }

    public String start(final Channel channel) throws IOException
    {
        consumer = null;

        if (channel != null)
        {
            try
            {
                consumer = new DefaultConsumer(channel)
                {
                    @Override
                    public void handleDelivery(final String consumerTag,
                                               final Envelope envelope,
                                               final BasicProperties properties,
                                               final byte[] body) throws IOException
                    {
                        handler.handleDelivery(channel, envelope, properties, body);
                    }

                };

                final boolean autoAck = false;
                final String consumerTag = channel.basicConsume(queue, autoAck, consumer);

                LOGGER.info("Consuming queue: " + queue + ": with tag: " + consumerTag + " on channel: "
                            + channel);

                return consumerTag;
            }
            catch (final Exception e)
            {
                LOGGER.log(Level.SEVERE, "Failed to start consuming queue: " + queue, e);
                consumer = null;
            }
        }

        return null;
    }

    public void stop()
    {
        final Channel channel = getChannel();
        if (channel == null)
        {
            return;
        }

        LOGGER.log(Level.INFO, "Stopping subscription: " + this);

        try
        {
            channel.basicCancel(consumer.getConsumerTag());
        }
        catch (final Exception e)
        {
            LOGGER.log(Level.SEVERE, "Failed to cancel subscription: " + this, e);
        }

        try
        {
            channel.close();
        }
        catch (final Exception e)
        {
            LOGGER.log(Level.SEVERE, "Failed to close channel: " + channel, e);
        }
        finally
        {
            consumer = null;
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        stop();
    }

    @Override
    public String toString()
    {
        final ToStringHelper tsh = Objects.toStringHelper(this).addValue(hashCode()).add("queue", queue);
        if (consumer != null)
        {
            tsh.add("channel", getChannel());
            tsh.add("consumerTag", consumer.getConsumerTag());
        }
        return tsh.toString();
    }

    public Channel getChannel()
    {
        return consumer == null ? null : consumer.getChannel();
    }
}
