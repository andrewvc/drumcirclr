
/**
 *  Namespace  
 */
var dc = dc = dc || {};

dc.canvas;
dc.stage;
dc.io;

window.requestAnimationFrame = (function(){
    return (
        window.requestAnimationFrame       ||
        window.webkitRequestAnimationFrame ||
        window.mozRequestAnimationFrame    ||
        window.oRequestAnimationFrame      ||
        window.msRequestAnimationFrame     ||
        function(callback, element){
            window.setTimeout(callback, 1000 / 60);
        }
    );
})();

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

        dc.sequences = [];
        _.each(_.range(0, 1), function() {
            var sequenceView = new dc.SequenceEditor();
            dc.sequences.push(sequenceView.getSequence());
            var sequenceDiv = sequenceView.render().el;
            $('#sequencer-holder').append(sequenceDiv);
        });

        dc.Metronome.start();
        var metronome = new dc.Metronome(240);
        metronome.listen(function(beat) {
            _.invoke(dc.sequences, 'play', [beat]);
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
          dc.io.postMessage('{"cmd":"getUserId"}');
        } else if (pm.evt === "message") {
            var msg = JSON.parse(pm.data);
            if (msg.cmd === "play") {
                if (msg.userId !== dc.userId) {
                    dc.testSound.play();
                }
            } else if (msg.cmd === "setUserId") {
                dc.userId = msg.userId;
            }
            log.info("Rx: " + pm.data);
        }
    }
}

/**
 *  Metronome 
 *
 *  Responsible for keeping track of the current beat.  Multiple instances can be created, all tracking different BPMs.
 */


/**
 * Constructor
 *
 * Creates new metronome given the BPM.
 */
dc.Metronome = function(bpm) {
    this.bpm = bpm;
    this.beat = 1;
    this.nextBeat = 0;
    this.listeners = [];
    dc.Metronome.instances.push(this);
}

/**
 *  STATIC 
 */
dc.Metronome.instances = [];
dc.Metronome.start = function() {
    (function loop() {
        var currentTimestamp = (+new Date()) / 1000;
        for (var i=0; i < dc.Metronome.instances.length; i++) {
            dc.Metronome.instances[i].tick(currentTimestamp);
        }
        window.requestAnimationFrame(loop);
    })();
}

/**
 *  PUBLIC 
 */
dc.Metronome.prototype = {
    tick: function(currentTimestamp) {
        if (currentTimestamp >= this.nextBeat) {
            this.beat = ~~(((currentTimestamp / 60) * this.bpm) % 16)+1;
            this.nextBeat = currentTimestamp + (60 / this.bpm);
            for (var i=0; i < this.listeners.length; i++) {
                this.listeners[i](this.beat);
            }
        }
    },

    /**
     * Calls back on every beat 
     */
    listen: function(listener) {
        this.listeners.push(listener);
    }
}

dc.SequenceNote = Backbone.Model.extend({

    toggle: function() {
        this.set({instrument: !this.get("instrument")});
    },

    play: function() {
        if (this.get("instrument")) {
            this.set({ playing: true });
            var that = this;
            console.log("playing " + this.cid);
            dc.testSound.play({ onfinish: function() { that.stop(); } });
        }
    },

    stop: function() {
        console.log("stopping " + this.cid);
        this.set({ playing: false });
    }
});

dc.Sequence = Backbone.Collection.extend({

    model: dc.SequenceNote,

    initialize: function() {
        if (this.models.length !== 16) {
            // 16 empty beats
            this.models = _.map(_.range(1, 17), function(beat) {
                return new dc.SequenceNote({
                    beat: beat,
                    instrument: false
                });
            });
        }
    },

    play: function(beat) {
        this.at(beat - 1).play();
    },

    comparator: function(note) {
        return note.get("beat");
    }
})

dc.SequenceEditor = Backbone.View.extend({

    tagName: "div",

    className: "sequence",

    initialize: function() {
        if (!this.collection) {
            this.collection = new dc.Sequence();
        }
        this.collection.each(this.addNote.bind(this));
        $(this.el).addClass('clearfix');
    },

    addNote: function(note) {
        var view = new dc.SequenceNoteView({ model: note});
        $(this.el).append(view.render().el);
    },

    getSequence: function() {
        return this.collection;
    }
});

dc.SequenceNoteView = Backbone.View.extend({

    tagName: "div",

    className: "note",
    
    initialize: function() {
        this.model.bind('change', this.render, this);
    },

    events: {
        "click" : "toggleNote"
    },

    render: function() {
        $(this.el).addClass('beat' + this.model.get("beat")); 
        if (this.model.get("instrument")) {
            $(this.el).removeClass('disabled').addClass('enabled');
        } else {
            $(this.el).removeClass('enabled').addClass('disabled');
        }
        if (this.model.get("playing")) {
            $(this.el).addClass('playing');
        } else {
            $(this.el).removeClass('playing');
        }
        return this;
    },

    toggleNote: function() {
        this.model.toggle();
    }
});
