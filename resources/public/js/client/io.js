var socket = new WebSocket("ws://127.0.0.1:3001/connect");


// Maximum time to establish connection, in s 
var CONNECT_TIMEOUT = 5;

// Proxy data upstream to server via WebSocket
self.onmessage = function(event) {
    if (socket.readyState !== WebSocket.OPEN) {
        if (socket.readyState === WebSocket.CLOSING || socket.readyState === WebSocket.CLOSED) {
            // TODO: Send invalid connection state error message
            return;
        }
    }

    // whew, stuff is working...
    socket.send(event.data);
}

socket.onopen = function () {
};
socket.onerror = function (msg) {
};
socket.onclose = function (msg) {
};

// Proxy messages from server back to main page 
socket.onmessage = function (msg) {
    self.postMessage(msg.data);
}
