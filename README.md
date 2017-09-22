# Order-Sys
Order-sys is a prototype of orders processing system. This prototype is implemented as a CLI
application and will not expose any REST APIs. The architecture of this app consists of two parts:
1. Client
2. Server

Client and Server communicate with each other via thru Netty client-server pattern. And to simplify
the design and implementation, order is not serialized and persisted into database, but juse serialized
into redis. 

And in the prototype transaction is also not fully supported.

