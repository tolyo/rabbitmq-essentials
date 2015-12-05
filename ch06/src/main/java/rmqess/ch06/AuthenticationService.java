
package rmqess.ch06;

import static java.util.logging.Level.SEVERE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import rmqess.ch02.ChannelCallable;
import rmqess.ch03.RabbitMqManager;
import rmqess.ch03.SubscriptionDeliveryHandler;
import rmqess.ch06.model.ErrorV1;
import rmqess.ch06.model.LoginRequestV1;
import rmqess.ch06.model.LoginResponseV1;
import rmqess.ch06.model.LogoutRequestV1;
import rmqess.ch06.model.LogoutResponseV1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class AuthenticationService
{
    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String LOGIN_REQUEST_V1_CONTENT_TYPE = "application/vnd.ccm.login.req.v1+json";
    private static final String LOGIN_RESPONSE_V1_CONTENT_TYPE = "application/vnd.ccm.login.res.v1+json";

    private static final String LOGOUT_REQUEST_V1_CONTENT_TYPE = "application/vnd.ccm.logout.req.v1+json";
    private static final String LOGOUT_RESPONSE_V1_CONTENT_TYPE = "application/vnd.ccm.logout.res.v1+json";

    private static final String ERROR_V1_CONTENT_TYPE = "application/vnd.ccm.error.v1+json";

    public static final String MESSAGE_ENCODING = "UTF-8";

    private static final String INTERNAL_SERVICES_EXCHANGE = "internal-services";
    private static final String AUTHENTICATION_SERVICE_QUEUE = "authentication-service";

    public AuthenticationService(final RabbitMqManager rabbitMqManager)
    {
        rabbitMqManager.call(new ChannelCallable<Void>()
        {
            @Override
            public String getDescription()
            {
                return "Declaring and binding: " + AUTHENTICATION_SERVICE_QUEUE;
            }

            @Override
            public Void call(final Channel channel) throws IOException
            {
                channel.exchangeDeclare(INTERNAL_SERVICES_EXCHANGE, "headers", true, // durable
                    false, // auto-delete
                    null); // arguments

                channel.queueDeclare(AUTHENTICATION_SERVICE_QUEUE, false, // durable
                    false, // exclusive,
                    true, // auto-delete
                    null); // arguments

                final String routingKey = "";
                final Map<String, Object> arguments = new HashMap<>();
                arguments.put("x-match", "all");
                arguments.put("request_type", "login");
                arguments.put("request_version", "v1");
                channel.queueBind(AUTHENTICATION_SERVICE_QUEUE, INTERNAL_SERVICES_EXCHANGE, routingKey,
                    arguments);

                // other arguments unchanged
                arguments.put("request_type", "logout");
                channel.queueBind(AUTHENTICATION_SERVICE_QUEUE, INTERNAL_SERVICES_EXCHANGE, routingKey,
                    arguments);

                return null;
            }
        });

        rabbitMqManager.createSubscription(AUTHENTICATION_SERVICE_QUEUE, new SubscriptionDeliveryHandler()
        {
            @Override
            public void handleDelivery(final Channel channel,
                                       final Envelope envelope,
                                       final BasicProperties requestProperties,
                                       final byte[] requestBody)
            {
                try
                {
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
                catch (final IOException ioe)
                {
                    LOGGER.severe("Failed to acknowledge: "
                                  + reflectionToString(envelope, SHORT_PREFIX_STYLE));
                }

                if (isBlank(requestProperties.getReplyTo()))
                {
                    LOGGER.warning("Received request without reply-to: "
                                   + reflectionToString(envelope, SHORT_PREFIX_STYLE));
                    return;
                }

                handleRequest(channel, envelope, requestProperties, requestBody);
            }

            private void handleRequest(final Channel channel,
                                       final Envelope envelope,
                                       final BasicProperties requestProperties,
                                       final byte[] requestBody)
            {
                try
                {
                    final String contentEncoding = requestProperties.getContentEncoding();

                    switch (requestProperties.getContentType())
                    {
                        case LOGIN_REQUEST_V1_CONTENT_TYPE :
                        {
                            final LoginRequestV1 request = OBJECT_MAPPER.readValue(new String(requestBody,
                                contentEncoding), LoginRequestV1.class);
                            final LoginResponseV1 response = login(request);
                            final byte[] responseBody = OBJECT_MAPPER.writeValueAsString(response).getBytes(
                                MESSAGE_ENCODING);
                            respond(channel, requestProperties, LOGIN_RESPONSE_V1_CONTENT_TYPE, responseBody);
                            break;
                        }

                        case LOGOUT_REQUEST_V1_CONTENT_TYPE :
                        {
                            final LogoutRequestV1 request = OBJECT_MAPPER.readValue(new String(requestBody,
                                contentEncoding), LogoutRequestV1.class);
                            final LogoutResponseV1 response = logout(request);
                            final byte[] responseBody = OBJECT_MAPPER.writeValueAsString(response).getBytes(
                                MESSAGE_ENCODING);
                            respond(channel, requestProperties, LOGOUT_RESPONSE_V1_CONTENT_TYPE, responseBody);
                            break;
                        }

                        default :
                            throw new IllegalArgumentException("Unsupported message type: "
                                                               + requestProperties.getContentType());
                    }
                }
                catch (final Exception e)
                {
                    handleException(channel, envelope, requestProperties, e);
                }
            }

            private void respond(final Channel channel,
                                 final BasicProperties requestProperties,
                                 final String responseContentType,
                                 final byte[] responseBody) throws IOException
            {
                final String messageId = UUID.randomUUID().toString();

                final BasicProperties props = new BasicProperties.Builder().contentType(responseContentType)
                    .contentEncoding(MESSAGE_ENCODING)
                    .messageId(messageId)
                    .correlationId(requestProperties.getCorrelationId())
                    .deliveryMode(1)
                    .build();

                channel.basicPublish("", requestProperties.getReplyTo(), props, responseBody);
            }

            private void handleException(final Channel channel,
                                         final Envelope envelope,
                                         final BasicProperties requestProperties,
                                         final Exception e1)
            {
                LOGGER.log(SEVERE, "Failed to handle: " + reflectionToString(envelope, SHORT_PREFIX_STYLE),
                    e1);

                try
                {
                    final ErrorV1 error = new ErrorV1().withContext(
                        reflectionToString(envelope, SHORT_PREFIX_STYLE)).withMessage(e1.getMessage());
                    final byte[] responseBody = OBJECT_MAPPER.writeValueAsString(error).getBytes(
                        MESSAGE_ENCODING);
                    respond(channel, requestProperties, ERROR_V1_CONTENT_TYPE, responseBody);
                }
                catch (final Exception e2)
                {
                    LOGGER.log(SEVERE,
                        "Failed to respond error for: " + reflectionToString(envelope, SHORT_PREFIX_STYLE),
                        e2);
                }
            }
        });
    }

    private LoginResponseV1 login(final LoginRequestV1 request)
    {
        if ("bob".equals(request.getUsername()) && "s3cr3t".equals(request.getPassword()))
        {
            return new LoginResponseV1().withSuccess(true).withAuthenticationToken(
                UUID.randomUUID().toString());
        }
        else
        {
            return new LoginResponseV1().withSuccess(false);
        }
    }

    private LogoutResponseV1 logout(final LogoutRequestV1 request)
    {
        return new LogoutResponseV1().withSuccess(true);
    }
}
