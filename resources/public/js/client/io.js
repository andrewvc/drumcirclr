var socket = new WebSocket("ws://127.0.0.1:3001/connect");

self.onmessage = function(event) {
    if (event.data !== "init") {
        socket.send('test');
    }
}

socket.onopen = function () {
};
socket.onerror = function (msg) {
};
socket.onclose = function (msg) {
};
socket.onmessage = function (msg) {
    self.postMessage(msg.data);
}
