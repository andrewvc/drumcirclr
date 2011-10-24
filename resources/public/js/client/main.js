
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
}
