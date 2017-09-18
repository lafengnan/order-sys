# Order-Sys
Order-sys is a prototype of orders processing system. This prototype is implemented as a CLI
application and will not expose any REST APIs. The architecture of this app consists of two parts:
1. Client
2. Server

Client and server nodes talk with each other by a simple message queue based on redis. That means each
request from client will be sent to MQ firstly, and server nodes pull requests from the MQ to process. In one word,
the arch looks like:

client --push--> |req_1|req_2|req_3|...|req_n|
server <--pull-- |req_1|req_2|req_3|...|req_n|

