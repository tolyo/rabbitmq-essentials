
package rmqess.ch02;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.AMQP.Queue.BindOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

@Named
public class UserMessageManager
{
    public static final String USER_INBOXES_EXCHANGE = "user-inboxes";
    public static final String MESSAGE_CONTENT_TYPE = "application/vnd.ccm.pmsg.v1+json";
    public static final String MESSAGE_ENCODING = "UTF-8";

    @Inject
    RabbitMqManager rabbitMqManager;

    public void onApplicationStart()
    {
        rabbitMqManager.call(new ChannelCallable<DeclareOk>()
        {
            @Override
            public String getDescription()
            {
                return "Declaring direct exchange: " + USER_INBOXES_EXCHANGE;
            }

            @Override
            public DeclareOk call(final Channel channel) throws IOException
            {
                final String exchange = USER_INBOXES_EXCHANGE;
                final String type = "direct";
                // survive a server restart
                final boolean durable = true;
                // keep it even if not in user
                final boolean autoDelete = false;
                // no special arguments
                final Map<String, Object> arguments = null;

                return channel.exchangeDeclare(exchange, type, durable, autoDelete, arguments);
            }
        });
    }

    public void onUserLogin(final long userId)
    {
        final String queue = getUserInboxQueue(userId);

        rabbitMqManager.call(new ChannelCallable<BindOk>()
        {
            @Override
            public String getDescription()
            {
                return "Declaring user queue: " + queue + ", binding it to exchange: "
                       + USER_INBOXES_EXCHANGE;
            }

            @Override
            public BindOk call(final Channel channel) throws IOException
            {
                return declareUserMessageQueue(queue, channel);
            }
        });
    }

    protected BindOk declareUserMessageQueue(final String queue, final Channel channel) throws IOException
    {
        // survive a server restart
        final boolean durable = true;
        // can be consumed by another connection
        final boolean exclusive = false;
        // keep the queue
        final boolean autoDelete = false;
        // no special arguments
        final Map<String, Object> arguments = null;
        channel.queueDeclare(queue, durable, exclusive, autoDelete, arguments);

        // bind the addressee's queue to the direct exchange
        final String routingKey = queue;
        return channel.queueBind(queue, USER_INBOXES_EXCHANGE, routingKey);
    }

    public String sendUserMessage(final long userId, final String message)
    {
        return rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "Sending message to user: " + userId;
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                final String queue = getUserInboxQueue(userId);

                // it may not exist so declare it
                declareUserMessageQueue(queue, channel);

                final String messageId = UUID.randomUUID().toString();

                final BasicProperties props = new BasicProperties.Builder().contentType(MESSAGE_CONTENT_TYPE)
                    .contentEncoding(MESSAGE_ENCODING)
                    .messageId(messageId)
                    .deliveryMode(2)
                    .build();

                final String routingKey = queue;

                // publish the message to the direct exchange
                channel.basicPublish(USER_INBOXES_EXCHANGE, routingKey, props,
                    message.getBytes(MESSAGE_ENCODING));

                return messageId;
            }
        });
    }

    public List<String> fetchUserMessages(final long userId)
    {
        return rabbitMqManager.call(new ChannelCallable<List<String>>()
        {
            @Override
            public String getDescription()
            {
                return "Fetching messages for user: " + userId;
            }

            @Override
            public List<String> call(final Channel channel) throws IOException
            {
                final List<String> messages = new ArrayList<>();

                final String queue = getUserInboxQueue(userId);
                final boolean autoAck = true;

                GetResponse getResponse;

                while ((getResponse = channel.basicGet(queue, autoAck)) != null)
                {
                    final String contentEncoding = getResponse.getProps().getContentEncoding();
                    messages.add(new String(getResponse.getBody(), contentEncoding));
                }

                return messages;
            }
        });
    }

    protected String getUserInboxQueue(final long userId)
    {
        return "user-inbox." + userId;
    }

    public RabbitMqManager getRabbitMqManager()
    {
        return rabbitMqManager;
    }

    public void setRabbitMqManager(final RabbitMqManager rabbitMqManager)
    {
        this.rabbitMqManager = rabbitMqManager;
    }
}
