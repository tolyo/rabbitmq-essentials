#!/usr/bin/env ruby

require "amqp"
require "json"
require "securerandom"

def email_message_to_user(json_message)
  puts "RETURNED: #{json_message}"
end

if ARGV.empty?
  puts 'Missing user ID'
  Process.exit(-1)
end
user_id = ARGV[0].chomp.to_i

EventMachine.run do
  AMQP.connect(:host => '127.0.0.1',
               :username => 'ccm-dev',
               :password => 'coney123',
               :vhost => 'ccm-dev-vhost') do |connection|

    channel  = AMQP::Channel.new(connection)

    exchange = channel.direct(
         'user-inboxes',
         :durable => true,
         :auto_delete => false) do |exchange, declare_ok|

      exchange.on_return do |basic_return, metadata, payload|
        email_message_to_user(payload)
      end

      message_id = SecureRandom.uuid
      message_json = JSON.generate({
         :time_sent => (Time.now.to_f*1000).to_i,
         :sender_id => -1, # special value for support
         :addressee_id => user_id,
         :subject   => 'Direct message from CS',
         :content   => 'A private message from customer support...'})

      routing_key = "user-inbox.#{user_id}"

      exchange.publish(
         message_json,
         :routing_key      => routing_key,
         :content_type     => 'application/vnd.ccm.pmsg.v1+json',
         :content_encoding => 'UTF-8',
         :message_id       => message_id,
         :persistent       => true,
         :nowait           => false,
         :mandatory        => true) do

         puts "Published message ID: #{message_id} to: #{routing_key}"
      end

      EventMachine.add_timer(1) { connection.close { EventMachine.stop } }
    end
  end
end

