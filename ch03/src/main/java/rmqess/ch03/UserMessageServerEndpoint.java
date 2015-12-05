
package rmqess.ch03;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import rmqess.ch02.model.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

@ServerEndpoint(value = "/user-message/{user-id}", configurator = CdiSimulatorEndpointConfigurator.class)
public class UserMessageServerEndpoint
{
    private static final Logger LOGGER = Logger.getLogger(UserMessageServerEndpoint.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RABBITMQ_SUBSCRIPTION = "rabbitmq.subscription";

    @Inject
    UserMessageManager userMessageManager;

    @OnOpen
    public void startSubscription(@PathParam("user-id") final long userId, final Session session)
    {
        session.setMaxIdleTimeout(0);

        final Subscription subscription = userMessageManager.subscribeToUserInbox(userId,
            new SubscriptionDeliveryHandler()
            {
                @Override
                public void handleDelivery(final Channel channel,
                                           final Envelope envelope,
                                           final BasicProperties properties,
                                           final byte[] body)
                {
                    try
                    {
                        final String contentEncoding = properties.getContentEncoding();
                        session.getBasicRemote().sendText(new String(body, contentEncoding));
                        final boolean multiple = false;
                        channel.basicAck(envelope.getDeliveryTag(), multiple);
                    }
                    catch (final Exception e)
                    {
                        LOGGER.log(Level.SEVERE,
                            "Failed to push over websocket message ID: " + properties.getMessageId(), e);

                        try
                        {
                            final boolean requeue = true;
                            channel.basicReject(envelope.getDeliveryTag(), requeue);
                        }
                        catch (final Exception e2)
                        {
                            LOGGER.log(Level.SEVERE,
                                "Failed to reject and requeue message ID: " + properties.getMessageId(), e);
                        }
                    }
                }
            });

        session.getUserProperties().put(RABBITMQ_SUBSCRIPTION, subscription);
    }

    @OnMessage
    public void publishMessage(final String jsonMessage, final Session session)
        throws IOException, EncodeException
    {
        final Subscription subscription = (Subscription) session.getUserProperties().get(
            RABBITMQ_SUBSCRIPTION);

        final Channel channel = subscription == null ? null : subscription.getChannel();
        if (channel == null)
        {
            LOGGER.log(Level.SEVERE, "No active channel to dispatch message: " + jsonMessage);
            return;
        }

        // inspect the message to find out where to route it
        final Message message = OBJECT_MAPPER.readValue(jsonMessage, Message.class);
        if (message.getAddresseeId() != null)
        {
            userMessageManager.sendUserMessage(message.getAddresseeId(), jsonMessage, channel);
        }
        else if (!Strings.isNullOrEmpty(message.getTopic()))
        {
            userMessageManager.sendTopicMessage(message.getTopic(), jsonMessage, channel);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Received unroutable message: " + jsonMessage);
        }
    }

    @OnClose
    public void stopSubscription(final Session session)
    {
        final Subscription subscription = (Subscription) session.getUserProperties().get(
            RABBITMQ_SUBSCRIPTION);

        if (subscription != null)
        {
            subscription.stop();
        }
    }

    @OnError
    public void handleError(final Throwable t, final Session session)
    {
        final Subscription subscription = (Subscription) session.getUserProperties().get(
            RABBITMQ_SUBSCRIPTION);

        LOGGER.log(Level.SEVERE, "Web-socket error with subscription: " + subscription, t);
    }
}
