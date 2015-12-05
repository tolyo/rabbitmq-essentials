#!/usr/bin/env python
import amqp

def report_error(message):
    print message.routing_key, message.body 

# --------------------------

connection = amqp.Connection(host='localhost', userid='ccm-dev', password='coney123', virtual_host='ccm-dev-vhost')
channel = connection.channel()

EXCHANGE = 'app-logs'
QUEUE = 'app-logs-error-reporter'

channel.exchange_declare(exchange=EXCHANGE, type='topic', durable=True, auto_delete=False)
channel.queue_declare(queue=QUEUE, durable=True, auto_delete=False)

# bind syslog severities:
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='err.#')
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='crit.#')
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='alert.#')
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='emerg.#')

# bind log4j levels
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='ERROR.#')
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='FATAL.#')

channel.basic_qos(prefetch_count=50, prefetch_size=0, a_global=False)

def handle_message(message):
    report_error(message)
    message.channel.basic_ack(delivery_tag=message.delivery_tag)

channel.basic_consume(callback=handle_message, queue=QUEUE, no_ack=False)

print ' [*] Waiting for messages. To exit press CTRL+C'
while channel.callbacks:
    channel.wait()

channel.close()
connection.close()

