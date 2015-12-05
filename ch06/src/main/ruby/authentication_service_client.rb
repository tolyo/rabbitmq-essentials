#!/usr/bin/env ruby

require "amqp"
require "json"
require "securerandom"

def handle_response(content_type, json_message)
  puts "Response type #{content_type}: #{json_message}"
  Process.exit(0)
end

if ARGV.size != 2
  puts 'Two parameters needed: <user_name> <password> (Hint: bob s3cr3t)'
  Process.exit(-1)
end

user_name = ARGV[0].chomp
password = ARGV[1].chomp

EventMachine.run do
  connection = AMQP.connect(:host => '127.0.0.1',
               :username => 'ccm-dev',
               :password => 'coney123',
               :vhost => 'ccm-dev-vhost')

  channel  = AMQP::Channel.new(connection)
  channel.on_error do |ch, channel_close|
    connection.close { EventMachine.stop }
    raise "Channel error: #{channel_close.inspect()}"
  end

  channel.headers(
         'internal-services',
         :durable     => true,
         :auto_delete => false,
         :passive     => true) do |exchange|

    channel.queue('',
      :exclusive => true,
      :durable => false,
      :auto_delete => true) do |response_queue|

      response_queue.subscribe do |metadata, payload|
        handle_response(metadata.content_type, payload)
      end

      puts "Response queue created: #{response_queue.name}"

      message_id = SecureRandom.uuid
      message_json = JSON.generate({
         :username => user_name,
         :password => password})

      exchange.publish(
         message_json,
         :content_type     => 'application/vnd.ccm.login.req.v1+json',
         :content_encoding => 'UTF-8',
         :message_id       => message_id,
         :correlation_id   => message_id,
         :reply_to         => response_queue.name,
         :headers          => { :request_type => 'login',
                                :request_version => 'v1' })
    end

    EventMachine.add_timer(3) do
      puts 'No response after 3 seconds'
      connection.close { EventMachine.stop }
    end
  end
end

