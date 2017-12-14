var BlinkEffect = function (composer) {
	this.composer = composer
}

BlinkEffect.prototype.setEnabled = function (enabled) {
	if (typeof EventSource == "undefined") {
		return;
	}
	
	if (enabled) {
		this.eventSourceSessionId = new Date().getTime();
		this.eventSource = new EventSource("/sse?session="
				+ this.eventSourceSessionId);
		var self = this
		this.eventSource.onmessage = function(event) {
			var splitted = event.data.split(' ', 2)
			if (splitted.length !== 2) {
				return
			}
			_.each(self.composer.graph.getElements(), function(c) {
				var wireComponent = c.attributes.wireComponent
				if (wireComponent && wireComponent.pid === splitted[0]) {
					self.fireTransition(c, wireComponent.getPortName(splitted[1], 'out'));
				}
			});
		};
	} else if (this.eventSource) {
		this.eventSource.close();
		this.eventSource = null;
		var xmlHttp = new XMLHttpRequest();
		xmlHttp.open("GET", "/sse?session=" + this.eventSourceSessionId
				+ "&logout=" + this.eventSourceSessionId, true);
		xmlHttp.send(null);
	}
}

BlinkEffect.prototype.fireTransition  = function(c, port) {
	var graph = this.composer.graph

	if (!this.composer.blinkEnabled) {
		return
	}

	var links = graph.getConnectedLinks(c, {
		outbound : true
	});

	links = _.filter(links, function(link) {
		return link.get('source').port === port
	})
	
	_.each(links, function(link) {

		var timingFunc = function (t) {
			return t < 0.5 ? t : 1-t
		}
	
		if (link.getTransitions().length) {
			return
		}
		
		link.transition('attrs/.connection/stroke', '#F39C12', {
			duration : 400,
			timingFunction : timingFunc,
			valueFunction : joint.util.interpolate.hexColor
		});

		link.transition('attrs/.connection/stroke-width', 8, {
			duration : 400,
			timingFunction : timingFunc,
			valueFunction : joint.util.interpolate.number
			});
		});
}
	