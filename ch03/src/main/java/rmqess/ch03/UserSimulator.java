
package rmqess.ch03;

import java.io.IOException;
import java.util.Random;

import javax.inject.Inject;
import javax.websocket.Session;

import rmqess.ch02.model.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UserSimulator implements Runnable
{
    private static final String[] TOPICS = {"science", "politics", "sports", "fashion"};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final long userId, maxUserId;
    private final Session session;
    private volatile boolean running;

    @Inject
    public UserSimulator(final long userId, final long maxUserId, final Session session)
    {
        this.userId = userId;
        this.maxUserId = maxUserId;
        this.session = session;
    }

    @Override
    public void run()
    {
        running = true;

        while (running && session.isOpen() && !Thread.currentThread().isInterrupted())
        {
            try
            {
                if (Math.random() < .25)
                {
                    final long addresseeUserId = 1 + new Random().nextInt((int) maxUserId);
                    if (addresseeUserId != userId)
                    {
                        final String jsonMessage = OBJECT_MAPPER.writeValueAsString(new Message().withSenderId(
                            userId)
                            .withAddresseeId(addresseeUserId)
                            .withSubject("Greetings!")
                            .withContent("Hello from: " + userId)
                            .withTimeSent(System.currentTimeMillis()));

                        session.getBasicRemote().sendText(jsonMessage);
                    }
                }

                if (Math.random() < .05)
                {
                    final String topic = TOPICS[new Random().nextInt(TOPICS.length)];

                    final String jsonMessage = OBJECT_MAPPER.writeValueAsString(new Message().withSenderId(
                        userId)
                        .withTopic(topic)
                        .withSubject("Something about " + topic)
                        .withContent("Great content about " + topic)
                        .withTimeSent(System.currentTimeMillis()));

                    session.getBasicRemote().sendText(jsonMessage);
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                Thread.sleep(1000L);
            }
            catch (final InterruptedException ie)
            {
                Thread.currentThread().interrupt();
            }
        }

        try
        {
            if (session.isOpen())
            {
                session.close();
            }
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    public void stop()
    {
        running = false;
    }
}
