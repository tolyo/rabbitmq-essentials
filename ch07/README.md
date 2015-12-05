# RabbitMQ Essentials - Source Code

## Chapter 7

### Java

- Start the application simulator with: `mvn exec:java`,
- While it's running stop/restart one the RabbitMQ brokers in the cluster to see the application reconnecting and recovering,
- Stop the application by striking `Enter`.


### Ruby

- Run: `bundle install`
- Then: `src/main/ruby/connection_failover.rb` to connect to one broker and fail over the the other


