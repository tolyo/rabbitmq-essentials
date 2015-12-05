
package rmqess.ch02;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import rmqess.ch02.model.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

public class UserSimulator implements Runnable
{
    private static final String[] TOPICS = {"science", "politics", "sports", "fashion"};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final long userId, maxUserId;
    private final UserMessageManagerWithTopics userMessageManager;

    @Inject
    public UserSimulator(final long userId,
                         final long maxUserId,
                         final UserMessageManagerWithTopics userMessageManager)
    {
        this.userId = userId;
        this.maxUserId = maxUserId;
        this.userMessageManager = userMessageManager;
    }

    @Override
    public void run()
    {
        userMessageManager.onUserLogin(userId);

        userMessageManager.onUserTopicInterestChange(userId, Sets.newHashSet("science", "politics"),
            Collections.<String> emptySet());

        System.out.printf("User login: %d%n", userId);

        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                final List<String> messages = userMessageManager.fetchUserMessages(userId);
                if (messages != null)
                {
                    for (final String message : messages)
                    {
                        System.out.printf("User %d received: %s%n", userId, message);
                    }
                }

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

                        userMessageManager.sendUserMessage(addresseeUserId, jsonMessage);
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

                    userMessageManager.sendTopicMessage(topic, jsonMessage);
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
    }
}
