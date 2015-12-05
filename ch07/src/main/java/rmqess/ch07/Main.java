
package rmqess.ch07;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;

public class Main extends rmqess.ch05.Main
{
    public static void main(final String[] args) throws Exception
    {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("ccm-dev");
        factory.setPassword("coney123");
        factory.setVirtualHost("ccm-dev-vhost");

        final Address[] addresses = new Address[]{new Address("localhost", 5672),
            new Address("localhost", 5673)};

        // simulate dependency management creation and wiring
        final RabbitMqManager rabbitMqManager = new RabbitMqManager(factory, addresses);
        rabbitMqManager.start();

        System.out.println("RabbitMQ Manager is connected to a cluster...");

        waitForEnter();
        shutdownRabbitMqManager(rabbitMqManager);
    }
}
