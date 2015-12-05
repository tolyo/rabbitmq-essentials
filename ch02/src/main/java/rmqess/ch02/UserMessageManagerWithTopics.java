
package rmqess.ch02;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.Channel;

@Named
@ApplicationScoped
public class UserMessageManagerWithTopics extends UserMessageManager
{
    public static final String USER_TOPICS_EXCHANGE = "user-topics";

    @PostConstruct
    @Override
    public void onApplicationStart()
    {
        super.onApplicationStart();

        rabbitMqManager.call(new ChannelCallable<DeclareOk>()
        {
            @Override
            public String getDescription()
            {
                return "Declaring topic exchange: " + USER_TOPICS_EXCHANGE;
            }

            @Override
            public DeclareOk call(final Channel channel) throws IOException
            {
                final String exchange = USER_TOPICS_EXCHANGE;
                final String type = "topic";
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

    public void onUserTopicInterestChange(final long userId,
                                          final Set<String> subscribes,
                                          final Set<String> unsubscribes)
    {
        final String queue = getUserInboxQueue(userId);

        rabbitMqManager.call(new ChannelCallable<Void>()
        {
            @Override
            public String getDescription()
            {
                return "Binding user queue: " + queue + " to exchange: " + USER_TOPICS_EXCHANGE + " with: "
                       + subscribes + ", unbinding: " + unsubscribes;
            }

            @Override
            public Void call(final Channel channel) throws IOException
            {
                for (final String subscribe : subscribes)
                {
                    channel.queueBind(queue, USER_TOPICS_EXCHANGE, subscribe);
                }

                for (final String unsubscribe : unsubscribes)
                {
                    channel.queueUnbind(queue, USER_TOPICS_EXCHANGE, unsubscribe);
                }

                return null;
            }
        });
    }

    public String sendTopicMessage(final String topic, final String message)
    {
        return rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "Sending message to topic: " + topic;
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                final String messageId = UUID.randomUUID().toString();

                final BasicProperties props = new BasicProperties.Builder().contentType(MESSAGE_CONTENT_TYPE)
                    .contentEncoding(MESSAGE_ENCODING)
                    .messageId(messageId)
                    .deliveryMode(2)
                    .build();

                // publish the message to the topic exchange
                channel.basicPublish(USER_TOPICS_EXCHANGE, topic, props, message.getBytes(MESSAGE_ENCODING));

                return messageId;
            }
        });
    }
}
