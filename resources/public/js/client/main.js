
/**
 *  Namespace  
 */
var dc = dc = dc || {};

dc.canvas;
dc.stage;
dc.io;

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

    var sequenceView = new dc.SequenceView();
    var sequenceDiv = sequenceView.render().el;
    $('#sequencer-holder').append(sequenceDiv);
}

dc.SequenceNote = Backbone.Model.extend({

    toggle: function() {
        this.set({instrument: !this.get("instrument")});
    },

    play: function() {
        dc.testSound.play();
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
    }
})

dc.SequenceView = Backbone.View.extend({

    tagName: "div",

    className: "sequence",

    initialize: function() {
        if (!this.collection) {
            this.collection = new dc.Sequence();
        }
        this.collection.each(this.addNote.bind(this));
    },

    addNote: function(note) {
        var view = new dc.SequenceNoteView({ model: note});
        $(this.el).append(view.render().el);
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
        return this;
    },

    toggleNote: function() {
        this.model.toggle();
    }
});
