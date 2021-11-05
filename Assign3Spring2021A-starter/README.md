# Description
This is a picture guessing game. The server sends pictures to the client. The client gets a guess from the user and sends it back to the server. The server evaluates the answer and returns a response.
Requirements met: 1-4, 6-18

## Running the Program

Use gradle to run both the server with optional port specification and client with optional port specification and hostIP. If you specify a port, they must be the same on both server and client. Dedault port = 8080 and default host = localhost.

Run server first.
```bash
gradle runServer -Pport=9000
```
Then connect the client.
```bash
gradle runClient -Pport=9000 -Phost=localhost
```

## Protocol
I sent the same json back and forth between server and client, adding more as the program progressed. Then it would check if the json had a certain value and decide what to do from there.

Example json protocol:
```json
{
    "error":-1,
    "name":"Megan",
    "questions":3,
    "start":true,
    "image":byte encoded image,
    "answer":"the correct answer",
    "guess":"puppy",
    "points":10
    "done":true
}
```

## Error Handling
The program is robust because it is enclosed in try/catch blocks that handle errors appropriately.
