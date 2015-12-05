# RabbitMQ Essentials - Source Code

## Chapter 4

### Python

- Install dependencies with `pip`, for example on Linux:

    sudo pip install -r requirements.txt

- To start the log archiver:

    `src/main/python/logs-archiver.py`

- To start the error reporter:

    `src/main/python/logs-error-reporter.py`


### JMeter

The `jmeter/lib_ext` directory contains JAR files that you need to drop in your JMeter's installation, under the `lib/ext` directory.

The `jmeter/logs-load-test.jmx` file contains a load test that sends 50,000 simulated Apache2 log messages to the `access-logs` exchange.

