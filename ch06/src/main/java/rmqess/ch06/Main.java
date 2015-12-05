
package rmqess.ch06;

import rmqess.ch03.RabbitMqManager;

public class Main extends rmqess.ch05.Main
{
    public static void main(final String[] args) throws Exception
    {
        final RabbitMqManager rabbitMqManager = connectRabbitMqManager();

        new AuthenticationService(rabbitMqManager);
        System.out.println("Authentication service is running...");

        waitForEnter();
        shutdownRabbitMqManager(rabbitMqManager);
    }
}
