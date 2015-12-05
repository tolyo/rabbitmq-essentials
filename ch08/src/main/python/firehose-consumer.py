#!/usr/bin/env python
import amqp

connection = amqp.Connection(host='localhost', userid='ccm-dev', password='coney123', virtual_host='ccm-dev-vhost')
channel = connection.channel()

EXCHANGE = 'amq.rabbitmq.trace'
QUEUE = 'firehose-queue'

channel.queue_declare(queue=QUEUE, durable=False, auto_delete=True, exclusive=True)
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='#')

def handle_message(message):
    print message.routing_key, '->', message.properties, message.body
    print '--------------------------------'

channel.basic_consume(callback=handle_message, queue=QUEUE, no_ack=True)

print ' [*] Waiting for messages. To exit press CTRL+C'
while channel.callbacks:
    channel.wait()

channel.close()
connection.close()

