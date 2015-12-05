
package rmqess.ch03;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@ClientEndpoint
public class WebsocketClientSimulator
{
    private final long userId, maxUserId;

    public WebsocketClientSimulator(final long userId, final long maxUserId)
    {
        this.userId = userId;
        this.maxUserId = maxUserId;
    }

    @OnOpen
    public void run(final Session session)
    {
        session.setMaxIdleTimeout(0);

        final UserSimulator userSimulator = new UserSimulator(userId, maxUserId, session);
        final Thread thread = new Thread(userSimulator, "user-thread-" + userId);

        session.getUserProperties().put("userSimulator", userSimulator);
        session.getUserProperties().put("userThread", thread);
        thread.start();
    }

    @OnMessage
    public void receivedServerMessage(final String payload)
    {
        System.out.printf("User: %d has received:%s%n", userId, payload);
    }

    @OnError
    public void onError(final Throwable t)
    {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(final Session session, final CloseReason closeReason)
    {
        ((UserSimulator) session.getUserProperties().get("userSimulator")).stop();

        try
        {
            ((Thread) session.getUserProperties().get("userThread")).join();
        }
        catch (final InterruptedException e)
        {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
