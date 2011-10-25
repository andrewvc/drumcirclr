
/**
 *  Namespace  
 */
var dc = dc = dc || {};

dc.init = function () {
    soundManager.url = '/js/soundmanager/swf/';
    soundManager.flashVersion = 9; // optional: shiny features (default = 8)
    soundManager.useFlashBlock = false; // optionally, enable when you're ready to dive in
    soundManager.useHighPerformance = true;
    soundManager.useFastPolling = true;
    /*
    * read up on HTML5 audio support, if you're feeling adventurous.
    * iPad/iPhone and devices without flash installed will always attempt to use it.
    */
    soundManager.onready(function() {
        dc.testSound = soundManager.createSound({
            id: 'testSound',
            url: '/sounds/amen_snare.mp3'
            // onload: myOnloadHandler,
            // other options here..
        });
    });

    dc.logTarget = $('#log');
    dc.logMessage = function (level,msg) {
        dc.logTarget.append("<div class='debug-log " + level + "'>" + msg + "</div>")
    };
    dc.log = {
        debug: function (msg) {
            dc.logMessage("debug", msg);
        },
        info: function (msg) {
            dc.logMessage("info", msg);
        },
        warn: function (msg) {
            dc.logMessage("info", msg);
        },
        fatal: function (msg) {
            dc.logMessage("info", msg);
        }
    };
    var log = dc.log;
    dc.userId = null;
    dc.io = new Worker('js/client/io.js');
    dc.io.onmessage = function (pmRaw) {
        var pm = pmRaw.data;
        console.log(pm.evt);
        if (pm.evt === "open") {
          dc.io.postMessage('{"cmd":"get-user-id"}');
        } else if (pm.evt === "message") {
            var msg = JSON.parse(pm.data);
            if (msg.cmd === "play") {
                if (msg["user-id"] !== dc.userId) {
                    dc.testSound.play();
                }
            } else if (msg.cmd === "set-user-id") {
                dc.userId = msg["user-id"];
            }
            log.info("Rx: " + pm.data);
        }
    }
}
