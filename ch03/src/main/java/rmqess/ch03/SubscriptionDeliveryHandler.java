
package rmqess.ch03;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public interface SubscriptionDeliveryHandler
{
    void handleDelivery(Channel channel, Envelope envelope, AMQP.BasicProperties properties, byte[] body);
}
