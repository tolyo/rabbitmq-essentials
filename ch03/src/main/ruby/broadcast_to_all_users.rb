#!/usr/bin/env ruby

require "amqp"
require "json"
require "securerandom"

EventMachine.run do
  AMQP.connect(:host => '127.0.0.1',
               :username => 'ccm-dev',
               :password => 'coney123',
               :vhost => 'ccm-dev-vhost') do |connection|

    channel  = AMQP::Channel.new(connection)

    exchange = channel.fanout(
         'user-fanout',
         :durable => true,
         :auto_delete => false)

    message_id = SecureRandom.uuid
    message_json = JSON.generate({
         :time_sent => (Time.now.to_f*1000).to_i,
         :sender_id => -1, # special value for support
         :subject   => 'Love from CS',
         :content   => 'A message from customer support...'})

    exchange.publish(
         message_json,
         :routing_key      => '',
         :content_type     => 'application/vnd.ccm.pmsg.v1+json',
         :content_encoding => 'UTF-8',
         :message_id       => message_id,
         :persistent       => true,
         :nowait           => false) do

        puts "Published message ID: #{message_id}"
        connection.close { EventMachine.stop }
    end

  end
end

