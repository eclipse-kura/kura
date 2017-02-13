/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Eurotech
 * 	Amit Kumar Mondal
 * 
 *******************************************************************************/
var kuraWires = (function() {
	var client = {}; // Holds accessible elements of JS library
	var clientConfig = {}; // Configuration passed from Kura OSGi
	// framework on save
	var graph, paper; // JointJS objects
	var initialized = false;
	var xPos = 10;
	var yPos = 10;
	var currentZoomLevel = 1.0;
	var paperScaleMax = 1.5;
	var paperScaleMin = .5;
	var paperScaling = .2;
	var selectedElement, oldSelectedPid;
	var oldCellView;
	var elementsContainerTemp = [];
	// used to disallow adding new instance to the Wire
	// Graph if any instance is recently deleted.
	var isComponentDeleted;

	/*
	 * / Public functions
	 */
	client.render = function(obj) {
		elementsContainerTemp = [];
		clientConfig = JSON.parse(obj);
		sse();
		setup();
		regiterFormInputFieldValidation();
	};
	
	client.resetDeleteComponentState = function() {
		isComponentDeleted = false;
	};

	client.getDriver = function(assetPid) {
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var elem = _elements[i];
			if (elem.attributes.label === assetPid) {
				return elem.attributes.driver;
			}
		}
	};

	var removeCellFunc = function(cell) {
		top.jsniMakeUiDirty();
		var _elements = graph.getElements();
		if (_elements.length == 0) {
			toggleDeleteGraphButton(true);
		} else {
			toggleDeleteGraphButton(false);
		}
	};

	/**
	 * Interaction with OSGi Event Admin through Server Sent Events
	 */
	function sse() {
		var eventSource = new EventSource("/sse?topic=org/eclipse/kura/wires/emit");
		eventSource.onmessage = function(event) {
			var parsedData = JSON.parse(event.data);
			_.each(graph.getElements(), function(c) {
				if (c.attributes.pid === parsedData.emitter) {
					fireTransition(c);
				}
			});
		};
	}

	function toggleDeleteGraphButton(flag) {
		$('#btn-delete-graph').prop('disabled', flag);
	}

	function checkForCycleExistence() {
		var visited = [];
		var isCycleExists;
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var elem = _elements[i];
			if ((graph.getPredecessors(elem).length == 0)
					&& hasCycle(elem, visited)) {
				isCycleExists = true;
				break;
			}
		}
		if (isCycleExists) {
			top.jsniShowCycleExistenceError();
		}
		return isCycleExists;
	}

	function hasCycle(element, visited) {
		var neighbors = graph.getNeighbors(element, {
			outbound : true
		}), i;

		if (visited.indexOf(element.id) > -1)
			return true;

		visited.push(element.id);

		for (i = 0; i < neighbors.length; i++)
			if (hasCycle(neighbors[i], visited.slice()))
				return true;

		return false;
	}

	/*
	 * / Initiate JointJS graph
	 */
	function setup() {
		// Setup element events. Cannot be done in ready as we need to wait for
		// GWT entry point to be called
		// Instantiate JointJS graph and paper
		if (!initialized) {
			$("#btn-create-comp").on("click", createNewComponent);
			$("#btn-create-comp-cancel").on("click", cancelCreateNewComponent);
			$("#btn-save-graph").on("click", saveConfig);
			$("#btn-delete-comp").on("click", deleteComponent);
			$("#btn-delete-graph-confirm").on("click", deleteGraph);
			$("#btn-zoom-in").on("click", zoomInPaper);
			$("#btn-zoom-out").on("click", zoomOutPaper);

			initialized = true;
			elementsContainerTemp = [];

			// Set up custom elements
			setupElements();

			// Setup graph and paper
			graph = new joint.dia.Graph;

			paper = new joint.dia.Paper({
				el : $('#wires-graph'),
				width : '100%',
				height : 400,
				model : graph,
				gridSize : 20,
				snapLinks : true,
				linkPinning : false,
				defaultLink : new joint.shapes.customLink.Element,
				multiLinks : false,
				markAvailable : true,
				restrictTranslate : function(elementView) {
					var parentId = elementView.model.get('parent');
					return parentId && this.model.getCell(parentId).getBBox();
				},
				interactive : {
					vertexAdd : false
				},
				validateConnection : function(cellViewS, magnetS, cellViewT,
						magnetT, end, linkView) {
					// Prevent linking from input ports.
					if (magnetS && magnetS.getAttribute('type') === 'input')
						return false;
					// Prevent linking from output ports to input ports within
					// one element.
					if (cellViewS === cellViewT)
						return false;
					// Prevent linking to input ports.
					return magnetT && magnetT.getAttribute('type') === 'input';
				}
			});
		}

		graph.off('remove', removeCellFunc);
		// Load a graph if it exists
		loadExistingWireGraph();

		// for any position change of the element, make the UI dirty
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var elem = _elements[i];
			elem.on('change:position', function() {
				top.jsniMakeUiDirty();
			})
		}

		// check if there exists any elements. If not, disable delete graph
		// button
		if (_elements.length == 0) {
			toggleDeleteGraphButton(true);
		} else {
			toggleDeleteGraphButton(false);
		}

		graph.on('change:source change:target', function(link) {
			createWire(link);
			top.jsniMakeUiDirty();
		});

		graph.on('remove', removeCellFunc);

		paper.on('cell:pointerdown', function(cellView, evt, x, y) {
			var pid = cellView.model.attributes.label;
			var factoryPid = cellView.model.attributes.factoryPid;
			selectedElement = cellView.model;
			if (oldCellView != null) {
				oldCellView.unhighlight();
				oldCellView = null;
			}
			if (typeof cellView !== 'undefined'
					&& typeof cellView.sourceBBox === 'undefined') {
				if (oldSelectedPid !== pid) {
					top.jsniUpdateSelection(pid, factoryPid);
					oldSelectedPid = pid;
					isUpdateSelectionTriggered = true;
				}
				cellView.highlight();
				oldCellView = cellView;
			}
		});

		paper.on('blank:pointerdown', function(cellView, evt, x, y) {
			top.jsniUpdateSelection("", "");
			selectedElement = "";
			oldSelectedPid = null;
			if (oldCellView != null) {
				oldCellView.unhighlight();
				oldCellView = null;
			}
		});

		// prevent opening context menu on paper
		paper.$el.on('contextmenu', function(evt) {
			evt.stopPropagation();
			evt.preventDefault();
		});
	}

	function loadExistingWireGraph() {
		if (!$.isEmptyObject(clientConfig.wireComponentsJson)) {
			graph.clear();
			var wireComponents = clientConfig.wireComponentsJson;
			for (var i = 0; i < wireComponents.length; i++) {
				var component = wireComponents[i].pid;
				var fPid = wireComponents[i].fPid;
				var type = wireComponents[i].type;
				var driver = wireComponents[i].driver;
				var x = parseInt(getLocationFromJsonByPid(component).split(",")[0]);
				var y = parseInt(getLocationFromJsonByPid(component).split(",")[1]);

				var compConfig = {
					fPid : fPid,
					pid : component,
					name : component,
					type : type,
					driver : driver,
					x : x,
					y : y,
				};
				createComponent(compConfig);
			}
			createExisitingWires();
		}
	}

	function createExisitingWires() {
		var wireConfigs = clientConfig.wireConfigsJson;
		for (var j = 0; j < wireConfigs.length; j++) {
			var emitter = wireConfigs[j].emitter;
			var receiver = wireConfigs[j].receiver;
			createLinkBetween(emitter, receiver);
		}
	}

	function getLocationFromJsonByPid(pid) {
		for (var i = 0; i < Object.keys(clientConfig.pGraph).length; i++) {
			var obj = clientConfig.pGraph[i];
			var componentPid = obj["pid"];
			if (componentPid === pid) {
				return obj["loc"];
			}
		}
		return xPos + "," + yPos;
	}

	function createLinkBetween(emitterPid, receiverPid) {
		var _elements = graph.getElements();
		var emitter = null, receiver = null;
		for (var i = 0; i < _elements.length; i++) {
			var element = _elements[i];
			if (element.attributes.pid === emitterPid) {
				emitter = element;
			}
			if (element.attributes.pid === receiverPid) {
				receiver = element;
			}
		}
		if (emitter != null && receiver != null) {
			var link = new joint.shapes.customLink.Element({
				source : {
					id : emitter.id
				},
				target : {
					id : receiver.id
				}
			});
			graph.addCell(link);
		}
	}

	function zoomInPaper() {
		if (currentZoomLevel <= paperScaleMax) {
			currentZoomLevel = currentZoomLevel + paperScaling;
			paper.scale(currentZoomLevel);
		}
	}

	function zoomOutPaper() {
		if (currentZoomLevel >= paperScaleMin) {
			currentZoomLevel = currentZoomLevel - paperScaling;
			paper.scale(currentZoomLevel);
		}
	}

	function fireTransition(t) {
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
				var link = _.find(outbound, function(l) {
					return l.get('target').id === p.id;
				});

				link.transition('attrs/.connection/stroke', '#F39C12', {
					duration : 400,
					timingFunction : function(t) {
						return t;
					},
					valueFunction : function(a, b) {

						var ca = parseInt(a.slice(1), 16);
						var cb = parseInt(b.slice(1), 16);
						var ra = ca & 0x0000ff;
						var rd = (cb & 0x0000ff) - ra;
						var ga = ca & 0x00ff00;
						var gd = (cb & 0x00ff00) - ga;
						var ba = ca & 0xff0000;
						var bd = (cb & 0xff0000) - ba;

						return function(t) {

							var scale = t < .5 ? t * 2 : 1 - (2 * (t - .5));
							var r = (ra + rd * scale) & 0x000000ff;
							var g = (ga + gd * scale) & 0x0000ff00;
							var b = (ba + bd * scale) & 0x00ff0000;

							var result = '#'
									+ (1 << 24 | r | g | b).toString(16).slice(
											1);
							if (t === 0) {
								result = '#4b4f6a';
							}
							return result;
						};
					}
				});

				link.transition('attrs/.connection/stroke-width', 8, {
					duration : 400,
					timingFunction : joint.util.timing.linear,
					valueFunction : function(a, b) {
						var d = b - a;
						return function(t) {
							var scale = t < .5 ? t * 2 : 1 - (2 * (t - .5));
							if (t === 0) {
								result = 4;
							} else {
								result = a + d * scale;
							}
							return result;
						};
					}
				});
			});
		}
	}

	/*
	 * / Create a new component
	 */
	function createComponent(comp) {

		if (comp.name === "") {
			comp.name = comp.pid;
		}

		// validate all the existing elements' PIDs with the new element PID. If
		// any of the existing element already has a PID which matches with the
		// PID with the new element then it would show an error modal
		var isFoundExistingElementWithSamePid;
		_.each(elementsContainerTemp, function(c) {
			if (c.toUpperCase() === comp.name.toUpperCase()) {
				isFoundExistingElementWithSamePid = true;
			}
		});

		if (isFoundExistingElementWithSamePid) {
			top.jsniShowDuplicatePidModal(name);
			return;
		}

		elementsContainerTemp.push(comp.name);

		// Setup allowed ports based on type
		if (comp.type === 'both') {
			inputPorts = [ '' ];
			outputPorts = [ '' ];
		} else if (comp.type === 'producer') {
			inputPorts = [];
			outputPorts = [ '' ];
		} else {
			inputPorts = [ '' ];
			outputPorts = [];
		}

		attrib = {
			'.label' : {
				text : joint.util.breakText(comp.name, {
					width : 100
				}),
			}
		};

		var rect = new joint.shapes.devs.Atomic({
			position : {
				x : comp.x,
				y : comp.y
			},
			attrs : attrib,
			inPorts : inputPorts,
			outPorts : outputPorts,
			label : comp.name,
			factoryPid : comp.fPid,
			pid : comp.pid,
			cType : comp.type,
			driver : comp.driver
		});

		graph.addCells([ rect ]);

		/* rounded corners */
		rect.attr({
			'.body' : {
				'rx' : 6,
				'ry' : 6
			}
		});

		rect.on('change:position', function() {
			top.jsniMakeUiDirty();
		})

		/* custom highlighting for ports */
		/*
		 * Custom Highlighting doesn't work efficiently for logical blocks as
		 * the custom highlighting functionality in jointJS creates an overlay
		 * element on top of the selected one. So, such extra element on the
		 * paper which makes us feel that the item is selected is not what we
		 * need. As it creates a problem while dragging the item after select.
		 * The newly created element which is used for highlighting remains at
		 * the same position but the element can be dragged anywhere.
		 */
		var portHighlighter = V('circle', {
			'r' : 14,
			'stroke' : '#ff7e5d',
			'stroke-width' : '6px',
			'fill' : 'transparent',
			'pointer-events' : 'none'
		});

		paper.off('cell:highlight cell:unhighlight').on({
			'cell:highlight' : function(cellView, el, opt) {
				var bbox = V(el).bbox(false, paper.viewport);

				if (opt.connecting) {
					portHighlighter.attr(bbox);
					portHighlighter.translate(bbox.x + 10, bbox.y + 10, {
						absolute : true
					});
					V(paper.viewport).append(portHighlighter);
				}
			},

			'cell:unhighlight' : function(cellView, el, opt) {

				if (opt.connecting) {
					portHighlighter.remove();
				}
			}
		});

		xPos = xPos + 212;
		if (xPos > 500) {
			xPos = 300;
			yPos = 300;
		}

		return rect.attributes.id;
	}

	/*
	 * / Event Functions
	 */
	function saveConfig() {
		graphToSave = prepareJsonFromGraph();
		newConfig = {
			wiregraph : graphToSave
		}
		elementsContainerTemp = [];
		isComponentDeleted = false;
		if (!checkForCycleExistence()) {
			top.jsniUpdateWireConfig(JSON.stringify(newConfig));
		}
	}

	function prepareJsonFromGraph() {
		var _links = graph.getLinks();
		var _elements = graph.getElements();
		var wiregraph = {};
		var wires = {};
		for (var i = 0; i < _links.length; i++) {
			var link = _links[i];
			var entry = {
				"producer" : getPidById(link.attributes.source.id),
				"consumer" : getPidById(link.attributes.target.id)
			}
			wires[i] = entry;
		}
		for (var j = 0; j < _elements.length; j++) {
			var element = _elements[j];
			entry = {
				"pid" : getPidById(element.id),
				"loc" : getElementLocation(element.id),
				"driver" : getDriverById(element.id),
				"fpid" : getFactoryPidById(element.id),
				"type" : element.attributes.cType
			}
			wiregraph[j] = entry;
		}
		wiregraph['wires'] = wires;
		return wiregraph;
	}

	function getPidById(id) {
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var element = _elements[i];
			if (element.id === id) {
				return element.attributes.label;
			}
		}
	}

	function getDriverById(id) {
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var element = _elements[i];
			if (element.id === id) {
				return element.attributes.driver;
			}
		}
		return null;
	}

	function getFactoryPidById(id) {
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var element = _elements[i];
			if (element.id === id) {
				return element.attributes.factoryPid;
			}
		}
		return null;
	}

	function getElementLocation(id) {
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var element = _elements[i];
			if (element.id === id) {
				var x = element.attributes.position.x;
				var y = element.attributes.position.y;
				return x + "," + y;
			}
		}
	}

	function createWire(link) {
		if ((typeof link.attributes.source.id != 'undefined')
				&& (typeof link.attributes.target.id != 'undefined')) {
			link.set("producer", link.attributes.source.id);
			link.set("consumer", link.attributes.target.id);
			link.set("newWire", true);
		}
	}

	function deleteComponent() {
		if (selectedElement !== ""
				&& selectedElement.attributes.type === 'devs.Atomic') {
			selectedElement.remove();
			var pid = selectedElement.attributes.label;
			var i = elementsContainerTemp.indexOf(pid);
			if (i != -1) {
				elementsContainerTemp.splice(i, 1);
			}
			top.jsniUpdateSelection("", "");
			if (clientConfig.wireComponentsJson.length !== "0") {
				isComponentDeleted = true;
			}
		}
	}

	function deleteGraph() {
		_.each(graph.getElements(), function(c) {
			c.remove();
		});
		graph.clear();
	}

	function regiterFormInputFieldValidation() {
		$("#componentName").bind(
				'keypress',
				function(event) {
					var regex = new RegExp("^[0-9a-zA-Z \b]+$");
					var key = String.fromCharCode(!event.charCode ? event.which
							: event.charCode);
					if (!regex.test(key)) {
						event.preventDefault();
						return false;
					}
				});
	}

	function cancelCreateNewComponent() {
		top.jsniDeactivateNavPils();
	}

	function createNewComponent() {
		if (isComponentDeleted) {
			top.jsniShowAddNotAllowedModal();
			return;
		}
		var newComp;
		// Determine whether component can be producer, consumer, or both
		fPid = $("#factoryPid").val();
		driverPid = $("#driverPids").val();
		name = $("#componentName").val();

		// validate all the existing elements' PIDs with the new element PID. If
		// any of the existing element already has a PID which matches with the
		// PID with the new element then it would show an error modal
		var isFoundExistingElementWithSamePid;
		_.each(graph.getElements(), function(c) {
			if (c.attributes.pid === name) {
				isFoundExistingElementWithSamePid = true;
			}
		});

		if (isFoundExistingElementWithSamePid) {
			top.jsniShowDuplicatePidModal(name);
			return;
		}

		if ($.inArray(fPid, clientConfig.pFactories) !== -1
				&& $.inArray(fPid, clientConfig.cFactories) !== -1) {
			cType = "both";
		} else if ($.inArray(fPid, clientConfig.pFactories) !== -1) {
			cType = "producer";
		} else {
			cType = "consumer";
		}
		if (name !== '') {
			if (fPid === "org.eclipse.kura.wire.WireAsset") {
				if (driverPid === "--- Select Driver ---") {
					return;
				}
				newComp = {
					fPid : fPid,
					pid : "none",
					name : name,
					driver : driverPid,
					type : cType,
					x : xPos,
					y : yPos
				}
			} else {
				newComp = {
					fPid : fPid,
					pid : "none",
					name : name,
					type : cType,
					x : xPos,
					y : yPos
				}
			}
			top.jsniMakeUiDirty();
			top.jsniDeactivateNavPils();
			toggleDeleteGraphButton(false);
			// Create the new component and store information in array
			createComponent(newComp);
			$("#componentName").val('');
			$("#driverPids").val('--- Select Driver ---');
			$("#factoryPid").val('');
			$("#asset-comp-modal").modal('hide');
		}
	}

	/*
	 * / Setup Custom Elements
	 */
	function setupElements() {

		joint.shapes.customLink = {};
		joint.shapes.customLink.Element = joint.dia.Link.extend({
			defaults : joint.util.deepSupplement({
				type : 'customLink.Element',
				router : {
					name : 'metro',
					args : {
						startDirections : [ 'right' ],
						endDirections : [ 'left' ]
					}
				},
				connector : {
					name : 'rounded'
				},
				attrs : {
					'.connection' : {
						'stroke' : "#4b4f6a",
						'stroke-width' : 4
					},
					'.marker-target' : {
						d : 'M 10 0 L 0 5 L 10 10 z'
					},
				},
				producer : 'producer',
				consumer : 'consumer',
				newWire : false
			}, joint.dia.Link.prototype.defaults)
		});
	}

	return client;

}());