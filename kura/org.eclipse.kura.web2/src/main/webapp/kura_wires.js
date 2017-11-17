/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Eurotech Amit Kumar Mondal
 * 
 ******************************************************************************/
var kuraWires = (function() {
	var client = {}; // Holds accessible elements of JS library
	var clientConfig = {}; // Configuration passed from Kura OSGi
	// framework on save
	var graph, paper, viewport; // JointJS objects
	var initialized = false;
	var currentZoomLevel = 1.0;
	var selectedElement;
	var oldCellView;
	var eventSourceSessionId; // Server Sent Events Session ID
	// used to disallow adding new instance to the Wire
	// Graph if any instance is recently deleted.
	var eventSource;
	var selectionRefreshPending = false;
	var blinkEnabled = true
	var blinkEnableTimeout = null

	/*
	 * / Public functions
	 */
	client.render = function(obj) {
		clientConfig = JSON.parse(obj);
		sse();
		setup();
		regiterFormInputFieldValidation();
	};

	client.unload = function() {
		if (typeof (EventSource) !== "undefined") {
			eventSource.close();
			eventSource = null;
			var xmlHttp = new XMLHttpRequest();
			xmlHttp.open("GET", "/sse?session=" + eventSourceSessionId
					+ "&logout=" + eventSourceSessionId, true);
			xmlHttp.send(null);
		}
	};

	function generateId() {
		return new Date().getTime()
	}

	client.selectionCompleted = function() {
		selectionRefreshPending = false;
	}

	$(document).ready(
			function() {
				$(window).bind(
						"beforeunload",
						function() {
							if (typeof (EventSource) !== "undefined") {
								eventSource.close();
								eventSource = null;
								var xmlHttp = new XMLHttpRequest();
								xmlHttp.open("GET", "/sse?session="
										+ eventSourceSessionId + "&logout="
										+ eventSourceSessionId, true);
								xmlHttp.send(null);
							}
						});
			});

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
		jsniMakeUiDirty();
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
		if (typeof (EventSource) !== "undefined" && eventSource == null) {
			eventSourceSessionId = generateId();
			eventSource = new EventSource("/sse?session="
					+ eventSourceSessionId);
			eventSource.onmessage = function(event) {
				_.each(graph.getElements(), function(c) {
					if (c.attributes.label === event.data) {
						fireTransition(c);
					}
				});
			};
		}
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
			jsniShowCycleExistenceError();
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
			$("#btn-select-asset-cancel").on("click", cancelCreateNewComponent);
			$("#btn-create-comp-cancel").on("click", cancelCreateNewComponent);
			$("#btn-save-graph").on("click", saveConfig);
			$("#btn-delete-comp").on("click", deleteComponent);
			$("#btn-delete-graph-confirm").on("click", deleteGraph);
			$("#btn-zoom-in").on("click", zoomInPaper);
			$("#btn-zoom-out").on("click", zoomOutPaper);
			$("#btn-zoom-fit").on("click", function () { fitContent(true) })
			initialized = true;

			// Set up custom elements
			setupElements();

			// Setup graph and paper
			graph = new joint.dia.Graph;
			
			paper = new joint.dia.Paper({
				el : $('#wires-graph'),
				width : '100%',
				height : '100%',
				model : graph,
				gridSize : 20,
				snapLinks : true,
				linkPinning : false,
				defaultLink : new joint.shapes.customLink.Element,
				multiLinks : false,
				markAvailable : true,
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
				jsniMakeUiDirty();
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
			jsniMakeUiDirty();
		});

		graph.on('remove', removeCellFunc);

		viewport = V(paper.viewport)
		
		paper.on('cell:pointerdown', function(cellView, evt, x, y) {
			var pid = cellView.model.attributes.label;
			var factoryPid = cellView.model.attributes.factoryPid;
			selectedElement = cellView.model;
			if (oldCellView != null) {
				jsniUpdateSelection("", "");
				oldCellView.unhighlight();
				oldCellView = null;
			}
		});

		paper.on('cell:pointerup', function(cellView, evt, x, y) {
			var pid = cellView.model.attributes.label;
			var factoryPid = cellView.model.attributes.factoryPid;
			if (typeof cellView.sourceBBox === 'undefined') {
				if (!selectionRefreshPending) {
					selectionRefreshPending = true;
					jsniUpdateSelection(pid, factoryPid);
					isUpdateSelectionTriggered = true;
				}
				cellView.highlight();
				oldCellView = cellView;
			}
		});
		
		paper.on('blank:pointerdown', function(cellView, evt, x, y) {
			client.scroller.begin()
			jsniUpdateSelection("", "");
			selectedElement = "";
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
		
		client.dragHandler = new DragHandler(paper.options.gridSize) 
		
		client.scroller = new Scroller()
		client.scroller.onMove(function (dx, dy) {
				viewport.translate(dx, dy)
		})
		
		fitContent()
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
				createComponent(compConfig, false);
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
		var coords = getNewComponentCoords()
		return coords.x + "," + coords.y;
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
		scale(1.2)
	}

	function zoomOutPaper() {
		scale(0.8)
	}
	
	function scale(factor) {
		var translation = viewport.translate()
		var cx = $("#wires-graph").width()/2
		var cy = $("#wires-graph").height()/2
		var tx = cx*(1-factor)+translation.tx*factor
		var ty = cy*(1-factor)+translation.ty*factor
		transformTransition(tx, ty, currentZoomLevel*factor, 0.25)
	}
	
	function fireTransition(t) {
		
		if (!blinkEnabled) {
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

	/**
	 * Create a new component
	 */
	function createComponent(comp, flag) {

		if (comp.name === "") {
			comp.name = comp.pid;
		}

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
			jsniMakeUiDirty();
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

		if (moveToFreeSpot(rect)) {
			centerOnComponent(rect)
		}

		if (flag) {
			selectedElement = rect;
			jsniUpdateSelection(comp.name, comp.fPid);
		}
		return rect.attributes.id;
	}

	/**
	 * Event Functions
	 */
	function saveConfig() {
		graphToSave = prepareJsonFromGraph();
		newConfig = {
			wiregraph : graphToSave
		}
		if (!checkForCycleExistence()) {
			jsniUpdateWireConfig(JSON.stringify(newConfig));
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
		jsniDeactivateNavPils();
	}

	function createNewComponent() {
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
			jsniShowDuplicatePidModal(name);
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
			
			var coords = getNewComponentCoords()
			
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
					x : coords.x,
					y : coords.y
				}
			} else {
				newComp = {
					fPid : fPid,
					pid : "none",
					name : name,
					type : cType,
					x : coords.x,
					y : coords.y
				}
			}
			jsniMakeUiDirty();
			jsniDeactivateNavPils();
			toggleDeleteGraphButton(false);
			// Create the new component and store information in array
			createComponent(newComp, true);
			$("#componentName").val('');
			$("#factoryPid").val('');
			$("#generic-comp-modal").modal('hide');
		}
	}

	client.createNewComponent = createNewComponent;

	function createNewAssetComponent(assetPid, driverPid) {
		var newComp;

		name = assetPid;

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
			jsniShowDuplicatePidModal(name);
			return;
		}

		cType = "both";
		
		var coords = getNewComponentCoords()

		newComp = {
			fPid : "org.eclipse.kura.wire.WireAsset",
			pid : "none",
			name : name,
			driver : driverPid,
			type : cType,
			x : coords.x,
			y : coords.y
		}

		jsniMakeUiDirty();
		jsniDeactivateNavPils();
		toggleDeleteGraphButton(false);
		// Create the new component and store information in array
		createComponent(newComp, true);
		$("#select-asset-modal").modal('hide');
	}

	client.createNewAssetComponent = createNewAssetComponent;

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
	
	var Scroller = function () {
		this.last = null;
		this.onMouseMove = this.onMouseMove.bind(this)
		this.onMouseUp = this.onMouseUp.bind(this)
	}
	
	Scroller.prototype.onMouseMove = function (e) {
		var x = e.clientX
		var y = e.clientY
		if (!this.last) {
			this.last = {x: x, y: y}
			return
		}
		var dx = x - this.last.x;
		var dy = y - this.last.y;
		
		this.last.x = x
		this.last.y = y
		
		if (this.callback) {
			this.callback(dx, dy)
		}
	}
	
	Scroller.prototype.onMouseUp = function () {
		document.removeEventListener('mousemove', this.onMouseMove)
		document.removeEventListener('mouseup', this.onMouseUp)
		this.last = null
		enableBlinking()
	}
	
	Scroller.prototype.begin = function () {
		document.addEventListener('mousemove', this.onMouseMove)
		document.addEventListener('mouseup', this.onMouseUp)
		disableBlinking()
	}
	
	Scroller.prototype.onMove = function (callback) {
		this.callback = callback
	}
	
	var DragHandler = function (gridSize) {
		this.dndHelper = DropSupport.addIfSupported(document.getElementById('composer'))
		this.dropCoords = null
		this.gridSize = gridSize
		var self = this
		if (this.dndHelper) {
			this.dndHelper.dragOverHandler = function (event) {
				self.movePreview(event.clientX, event.clientY)
				return true
			}
			this.dndHelper.dragExitHandler = function (event) {
				self.abort()
			}
			this.dndHelper.dropHandler = function(event) { 
				var factoryPid = DragHandler.getFactoryPidFromDropUrl(event.dataTransfer.getData('text'))
				if (!factoryPid) {
					return false
				}
				self.onDrop(event.clientX, event.clientY, factoryPid)
				return true
			}
		}
	}
	
	DragHandler.getFactoryPidFromDropUrl = function (dropUrl) {
        if (!dropUrl) {
            return null
        }
        if (!dropUrl.startsWith('factory://')) {
            return null
        }
        return dropUrl.substring('factory://'.length, dropUrl.length);
    }
	
	DragHandler.prototype.toLocalCoords = function (clientX, clientY) {
		var offset = $('#wires-graph').offset()
		clientX -= offset.left - $(window).scrollLeft()
		clientY -= offset.top - $(window).scrollTop()
		return clampToGrid(clientToLocal(clientX, clientY), this.gridSize)
	}
	
	DragHandler.prototype.movePreview = function (clientX, clientY) {
		if (this.rect == null) {
			this.initTempElement()
		}
		var pos = this.toLocalCoords(clientX, clientY)
		var oldPos = this.rect.position()
		if (pos.x != oldPos.x || pos.y != oldPos.y) {
			this.rect.position(pos.x, pos.y)
		}
	}
	
	DragHandler.prototype.onDrop = function (clientX, clientY, factoryPid) {
		var pos = this.toLocalCoords(clientX, clientY)
		this.dropCoords = pos
		jsniShowComponentCreationDialog(factoryPid)
	}
	
	DragHandler.prototype.abort = function () {
		this.dropCoords = null;
		if (this.rect) {
			graph.removeCells([ this.rect ])
			this.rect = null
		}
	}
	
	DragHandler.prototype.hasDropCoords = function () {
		return this.dropCoords != null
	}
	
	DragHandler.prototype.getDropCoords = function () {
		var temp = this.dropCoords
		this.abort();
		return temp
	}
	
	DragHandler.prototype.initTempElement = function () {
		this.rect = new joint.shapes.devs.Atomic({
			position : {
				x : 0,
				y : 0
			}
		});

		graph.addCells([ this.rect ]);
		
		this.rect.attr({
			'.label' : {
				text : "",
			},
			'.body' : {
				'rx' : 6,
				'ry' : 6,
				'class': 'body temporary'
			}
		});
	}
	
	function disableBlinking() {
		blinkEnabled = false
		if (blinkEnableTimeout) {
			clearTimeout(blinkEnableTimeout)
			blinkEnableTimeout = null
		}
	}
	
	function enableBlinking() {
		if (!blinkEnabled && !blinkEnableTimeout) {
			blinkEnableTimeout = setTimeout(function () {
				blinkEnabled = true
				blinkEnableTimeout = null
			}, 1000)
		} 
	}
	
	function moveToFreeSpot(comp) {
		var moved = false
		while (graph.findModelsUnderElement(comp).length) {
			moved = true
			comp.translate(100, 0)
		}
		return moved
	}
	
	function getNewComponentCoords() {
		if (client.dragHandler.hasDropCoords()) {
			return client.dragHandler.getDropCoords()
		} else {
			var cx = $('#wires-graph').width() / 2
			var cy = $('#wires-graph').height() / 2
			return clampToGrid(clientToLocal(cx, cy), paper.options.gridSize)
		}
	}
	
	function centerOnComponent(comp) {
		var pos = comp.get('position')
		var scale = currentZoomLevel < 1 ? 1 : currentZoomLevel
		centerOnLocalPoint(pos.x, pos.y, scale, transformTransition)
	}
	
	function fitContent(transition) {
		var bbox = getLocalContentBBox()
		var cx = bbox.x + bbox.width/2
		var cy = bbox.y + bbox.height/2
		var vw = $('#wires-graph').width()
		var vh = $('#wires-graph').height()
		var factor = Math.min(vw/bbox.width, vh/bbox.height)
		if (factor > 1) {
			factor = 1
		}
		centerOnLocalPoint(cx, cy, factor, transition ? transformTransition : transform, 0.5)
	}
	
	function clientToLocal(clientX, clientY) {
		var translation = viewport.translate()
		var x = (clientX-translation.tx)/currentZoomLevel
		var y = (clientY-translation.ty)/currentZoomLevel
		return {x: x, y: y}
	}
	
	function clampToGrid(pos, gridSize) {
		pos.x = Math.floor(pos.x/gridSize)*gridSize
		pos.y = Math.floor(pos.y/gridSize)*gridSize
		return pos
	}
	
	var transitionRunning = false
	
	function transformTransition(translationx, translationy, scale, durationSeconds) {
		if (transitionRunning) {
			return
		}
		transitionRunning = true
		var initialTranslation = viewport.translate()
		var initialScale = currentZoomLevel
		var start = null
		disableBlinking()
		var step = function (timestamp) {
			start = start || timestamp
			var alpha = Math.min((timestamp-start)/durationSeconds/1000, 1)
			var calpha = 1-alpha
			var translationX = initialTranslation.tx*calpha+translationx*alpha
			var translationY = initialTranslation.ty*calpha+translationy*alpha
			var currentScale = initialScale*calpha+scale*alpha
			transform(translationX, translationY, currentScale)
			if (alpha < 1) {
				window.requestAnimationFrame(step)
			} else {
				transitionRunning = false
				enableBlinking()
			}
		}
		window.requestAnimationFrame(step)
	}
	
	var transform = function (translationX, translationY, scale) {
		viewport.translate(translationX, translationY, {absolute: true})
		viewport.scale(scale)
		currentZoomLevel = scale
	}
	
	function getLocalContentBBox() {
		var bbox = paper.getContentBBox() // this is in client coordinates
		var tl = clientToLocal(bbox.x, bbox.y)
		var br = clientToLocal(bbox.x + bbox.width, bbox.y + bbox.height)
		return { x: tl.x, y: tl.y, width: br.x-tl.x, height: br.y-tl.y }
	}
	
	function centerOnLocalPoint(cx, cy, scale, transformFunc, transitionSpeed) {
		var transitionSpeed = transitionSpeed || 0.5
		var vw = $('#wires-graph').width()
		var vh = $('#wires-graph').height()
		transformFunc(vw/2-scale*cx, vh/2-scale*cy, scale, transitionSpeed)
	}
	
	return client;

}());