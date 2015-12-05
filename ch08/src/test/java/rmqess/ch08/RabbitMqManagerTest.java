
package rmqess.ch08;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rmqess.ch02.ChannelCallable;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@RunWith(MockitoJUnitRunner.class)
public class RabbitMqManagerTest
{
    private static final Address[] TEST_ADDRESSES = {new Address("fake")};

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    private RabbitMqManager rabbitMqManager;

    @Before
    public void initialize() throws Exception
    {
        rabbitMqManager = new RabbitMqManager(connectionFactory, TEST_ADDRESSES);

        when(connection.getAddress()).thenReturn(InetAddress.getLocalHost());
    }

    @Test
    public void startFailure() throws Exception
    {
        when(connectionFactory.newConnection(TEST_ADDRESSES)).thenThrow(
            new RuntimeException("simulated failure"));

        rabbitMqManager.start();

        final List<Runnable> scheduleReconnection = rabbitMqManager.getExecutor().shutdownNow();

        assertThat(scheduleReconnection.size(), is(1));
    }

    @Test
    public void startSuccess() throws Exception
    {
        when(connectionFactory.newConnection(TEST_ADDRESSES)).thenReturn(connection);

        rabbitMqManager.start();

        verify(connection).addShutdownListener(rabbitMqManager);
    }

    @Test
    public void startFailureThenSuccess() throws Exception
    {
        when(connectionFactory.newConnection(TEST_ADDRESSES)).thenThrow(
            new RuntimeException("simulated connection failure")).thenReturn(connection);

        // we want to try to reconnect right away
        rabbitMqManager.setReconnectDelaySeconds(0);

        rabbitMqManager.start();

        Thread.sleep(100L);

        verify(connectionFactory, times(2)).newConnection(TEST_ADDRESSES);
        verify(connection).addShutdownListener(rabbitMqManager);
        verifyNoMoreInteractions(connectionFactory);
    }

    // Exercise for the reader:
    // TODO test startWithSubscriptionSuccess
    // TODO test createSubscription

    @Test
    public void createChannelNotConnected() throws Exception
    {
        assertThat(rabbitMqManager.createChannel(), is(nullValue()));
    }

    @Test
    public void createChannelFailure() throws Exception
    {
        rabbitMqManager.setConnection(connection);

        when(connection.createChannel()).thenThrow(new RuntimeException("simulated channel creation failure"));

        assertThat(rabbitMqManager.createChannel(), is(nullValue()));
    }

    @Test
    public void createChannelSuccess() throws Exception
    {
        rabbitMqManager.setConnection(connection);

        when(connection.createChannel()).thenReturn(channel);

        assertThat(rabbitMqManager.createChannel(), is(channel));
    }

    @Test
    public void closeNullChannel()
    {
        rabbitMqManager.closeChannel(null);
    }

    @Test
    public void closeClosedChannel()
    {
        when(channel.isOpen()).thenReturn(false);

        rabbitMqManager.closeChannel(channel);

        verify(channel).isOpen();
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void closeOpenedChannelFailure() throws Exception
    {
        when(channel.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("simulated channel closure failure")).when(channel).close();

        rabbitMqManager.closeChannel(channel);

        verify(channel).close();
    }

    @Test
    public void closeOpenedChannelSuccess() throws Exception
    {
        when(channel.isOpen()).thenReturn(true);

        rabbitMqManager.closeChannel(channel);

        verify(channel).close();
    }

    @Test
    public void callNullChannel()
    {
        final String result = rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                throw new UnsupportedOperationException("should not be called");
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                throw new UnsupportedOperationException("should not be called");
            }
        });

        assertThat(result, is(nullValue()));
    }

    @Test
    public void callFailure() throws Exception
    {
        rabbitMqManager.setConnection(connection);
        when(channel.isOpen()).thenReturn(true);
        when(connection.createChannel()).thenReturn(channel);

        final String result = rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "failure";
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                throw new RuntimeException("simulated channel callable failure");
            }
        });

        assertThat(result, is(nullValue()));

        verify(channel).close();
    }

    @Test
    public void callSuccess() throws Exception
    {
        when(channel.isOpen()).thenReturn(true);
        when(connection.createChannel()).thenReturn(channel);
        rabbitMqManager.setConnection(connection);

        final Channel expectedChannel = channel;

        final String result = rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "success";
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                return channel == expectedChannel ? "ok" : "ko";
            }
        });

        assertThat(result, is("ok"));

        verify(channel).close();
    }
}
