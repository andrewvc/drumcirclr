var socket = new WebSocket("ws://127.0.0.1:3001/connect");
// Maximum time to establish connection, in s 
var CONNECT_TIMEOUT = 5;

socket.onopen = function (msg) {
    self.postMessage('{"cmd":"log", "message":"io:socket opened"}');

    // Proxy data upstream to server via WebSocket
    self.onmessage = function(msg) {
        socket.send(msg.data);
    }
};
socket.onerror = function (msg) {
    self.postMessage('{"cmd":"log", "message":"io:socket error"}');

};
socket.onclose = function (msg) {
    self.postMessage('{"cmd":"log", "message":"io:socket closed"}');
};

// Proxy messages from server back to main page 
socket.onmessage = function (msg) {
    self.postMessage(msg.data);
}
