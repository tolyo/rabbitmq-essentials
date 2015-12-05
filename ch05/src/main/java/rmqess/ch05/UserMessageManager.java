
package rmqess.ch05;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import rmqess.ch02.ChannelCallable;
import rmqess.ch03.SubscriptionDeliveryHandler;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.BindOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.LongString;

public class UserMessageManager extends rmqess.ch03.UserMessageManager
{
    protected final static Logger LOGGER = Logger.getLogger(UserMessageManager.class.getName());

    public static final String USER_DL_EXCHANGE = "user-dlx";
    public static final String USER_DL_QUEUE = "user-dlq";

    private final UserManager userManager = new UserManager();

    @Override
    public void onApplicationStart()
    {
        super.onApplicationStart();

        rabbitMqManager.call(new ChannelCallable<BindOk>()
        {
            @Override
            public String getDescription()
            {
                return "Declaring dead-letter exchange: " + USER_DL_EXCHANGE + " and queue: " + USER_DL_QUEUE;
            }

            @Override
            public BindOk call(final Channel channel) throws IOException
            {
                final boolean durable = true;
                final boolean autoDelete = false;

                final String exchange = USER_DL_EXCHANGE;
                final String type = "fanout";
                final Map<String, Object> arguments = null;

                channel.exchangeDeclare(exchange, type, durable, autoDelete, arguments);

                final String queue = USER_DL_QUEUE;
                final boolean exclusive = false;
                channel.queueDeclare(queue, durable, exclusive, autoDelete, arguments);

                final String routingKey = "";
                return channel.queueBind(queue, exchange, routingKey);
            }
        });

        rabbitMqManager.createSubscription(USER_DL_QUEUE, new SubscriptionDeliveryHandler()
        {
            @Override
            public void handleDelivery(final Channel channel,
                                       final Envelope envelope,
                                       final BasicProperties properties,
                                       final byte[] body)
            {
                @SuppressWarnings("unchecked")
                final List<Map<String, LongString>> deathInfo = (List<Map<String, LongString>>) properties.getHeaders()
                    .get("x-death");

                if (deathInfo.get(0).get("exchange").toString().equals("user-inboxes"))
                {
                    final long userId = Long.valueOf(StringUtils.substringAfter(envelope.getRoutingKey(),
                        "user-inbox."));

                    final String contentEncoding = properties.getContentEncoding();

                    try
                    {
                        final String jsonMessage = new String(body, contentEncoding);
                        userManager.handleDeadMessage(userId, jsonMessage);
                    }
                    catch (final UnsupportedEncodingException uee)
                    {
                        LOGGER.severe("Failed to handle dead message: " + envelope.getRoutingKey()
                                      + ", encoding: " + contentEncoding + ", entry: "
                                      + Base64.encodeBase64(body));
                    }
                }

                try
                {
                    final boolean multiple = false;
                    channel.basicAck(envelope.getDeliveryTag(), multiple);
                }
                catch (final IOException ioe)
                {
                    LOGGER.severe("Failed to acknowledge: "
                                  + ToStringBuilder.reflectionToString(envelope,
                                      ToStringStyle.SHORT_PREFIX_STYLE));
                }
            }
        });
    }
}
