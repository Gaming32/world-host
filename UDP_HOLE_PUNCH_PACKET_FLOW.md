Client A refers to the client hosting the world. Client B refers to the connecting client. Server refers to the World Host server. In some situations, Client A and Client B may be the same client.

1. Client B asks Server over main channel for Client A to open a port for a specified purpose. A unique "cookie" is included.
2. Server passes the request to Client A.
3. Client A verifies the request comes from a valid party.
4. If verification is successful, Client A sends a message over UDP signaling channel with the same cookie.
5. Client A retransmits this message every tick.
6. If Server doesn't receive the cookie over the signalling channel within 10 seconds, it assumes the request was invalid and drops the request.
7. When Server receives the cookie, it notifies Client A that the message was received. Client A stops retransmitting.
8. Server notifies Client B of the IP and external port of Client A.
