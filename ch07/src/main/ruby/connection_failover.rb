#!/usr/bin/env ruby

require "amqp"
require "json"
require "securerandom"

def run_with_connection(settings, &action)
  broker = settings[:brokers].shift

  raise "Impossible to connect to any broker" if broker.nil?
  
  settings.merge!(broker)

  settings.merge!({
    :on_tcp_connection_failure => Proc.new {
      run_with_connection(settings, &action)
    }
  })

  EventMachine.run do
    AMQP.connect(settings) do |connection|
      action.call(connection)
    end
  end
end

settings = {
  :brokers  => [
                {:host => 'localhost', :port=> 5672},
                {:host => 'localhost', :port=> 5673}
               ],
  :vhost    => "ccm-dev-vhost",
  :user     => "ccm-dev",
  :password => "coney123"
}

# example usage
run_with_connection(settings) do |connection|
  # do something useful here
  puts "Using #{connection.settings}"
  connection.close { EventMachine.stop }
end
