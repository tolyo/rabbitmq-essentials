#!/usr/bin/env python
import amqp
import time
import sys

# test related code --------
current_milli_time = lambda: int(round(time.time() * 1000))
expected_count = 50000
received_count = 0
start_time = current_milli_time()

def store_log_data(message):
    global received_count

    received_count += 1

    if received_count == expected_count:
        print current_milli_time() - start_time
        message.channel.basic_cancel(message.consumer_tag)

# --------------------------

connection = amqp.Connection(host='localhost', userid='ccm-dev', password='coney123', virtual_host='ccm-dev-vhost')
channel = connection.channel()

EXCHANGE = 'app-logs'
QUEUE = 'app-logs-archiver'

channel.exchange_declare(exchange=EXCHANGE, type='topic', durable=True, auto_delete=False)
channel.queue_declare(queue=QUEUE, durable=True, auto_delete=False)
channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key='#')

channel.basic_qos(prefetch_count=50, prefetch_size=0, a_global=False)

def handle_message(message):
    store_log_data(message)
    message.channel.basic_ack(delivery_tag=message.delivery_tag)

channel.basic_consume(callback=handle_message, queue=QUEUE, no_ack=False)

print ' [*] Waiting for messages. To exit press CTRL+C'
while channel.callbacks:
    channel.wait()

channel.close()
connection.close()

