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
			_.each(self.composer.graph.getElements(), function(c) {
				if (c.attributes.wireComponent && c.attributes.wireComponent.pid === event.data) {
					self.fireTransition(c);
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

BlinkEffect.prototype.fireTransition  = function(t) {
		var graph = this.composer.graph

		if (!this.composer.blinkEnabled) {
			return
		}
		
		var inbound = graph.getConnectedLinks(t, {
			inbound : false
		});
		var outbound = graph.getConnectedLinks(t, {
			outbound : true
		});

		var placesBefore = _.map(inbound, function(link) {
			return graph.getCell(link.get('source').id);
		});
		var placesAfter = _.map(outbound, function(link) {
			return graph.getCell(link.get('target').id);
		});

		var isFirable = true;
		_.each(placesBefore, function(p) {
			if (p.get('tokens') === 0)
				isFirable = false;
		});

		if (isFirable) {

			_.each(placesAfter, function(p) {
				if (!p) {
					return
				}
				
				var link = _.find(outbound, function(l) {
					return l.get('target').id === p.id;
				});

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
	}