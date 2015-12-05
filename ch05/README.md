# RabbitMQ Essentials - Source Code

## Chapter 5

### Java

- Start the application simulator with: `mvn exec:java`,
- Stop the application by striking `Enter`.

### Ruby

- Run: `bundle install`
- Then: `src/main/ruby/send_to_user.rb` to send a test message to a single user.
  Using a known user ID like 1 to 10 should deliver the message to the user inbox queue.
  Using an unknown user ID like 1000 should lead to the message being returned by RabbitMQ.


