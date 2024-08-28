## Port Lookup

Client refers to the client performing a port lookup. Server refers to the World Host server.

1. Client asks Server to begin port lookup with a randomly generated UUID.
2. Client sends Server UUID over UDP signaling channel (retransmitted every 3 ticks).
3. If Server doesn't receive the UUID within 10 seconds, it drops the request.
4. Server sends Client identified IP and outer port.

## Hole Punching

Client A refers to the client hosting the world. Client B refers to the connecting client. Server refers to the World Host server.

1. Client B performs Port Lookup to receive an open outer port.
2. Client B asks Server for Client A to open a port for a specified purpose. A unique UUID, the IP, and the outer port are included.
3. Server passes the request to Client A.
4. Client A verifies the request comes from a valid party.
5. If verification is successful, Client A performs Port Lookup and sends an empty packet to Client B's port until Port Lookup finishes.
6. Server notifies Client B of the IP and external port of Client A.
