
package rmqess.ch05;

import java.util.logging.Logger;

public class UserManager
{
    protected final static Logger LOGGER = Logger.getLogger(UserManager.class.getName());

    public void handleDeadMessage(final long userId, final String jsonMessage)
    {
        // based on user preferences, email or drop message
        LOGGER.info("Handling dead message for user: " + userId);
    }
}
