
package rmqess.ch03;

import org.glassfish.tyrus.server.TyrusServerEndpointConfigurator;

public class CdiSimulatorEndpointConfigurator extends TyrusServerEndpointConfigurator
{
    @Override
    public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException
    {
        final T endpointInstance = super.getEndpointInstance(endpointClass);

        if (endpointInstance instanceof UserMessageServerEndpoint)
        {
            final UserMessageServerEndpoint userMessageServerEndpoint = (UserMessageServerEndpoint) endpointInstance;
            userMessageServerEndpoint.userMessageManager = Main.userMessageManager;
        }

        return endpointInstance;
    }
}
