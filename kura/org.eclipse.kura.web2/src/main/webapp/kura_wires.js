/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 	Eurotech 
 * 	Amit Kumar Mondal (admin@amitinside.com)
 */
var kuraWires = (function() {
	var client = {}; // Holds accessible elements of JS library
	var clientConfig = {}; // Configuration passed from Kura OSGi
	var delCells = []; // Components and Wires to be deleted from OSGi
	// framework on save
	var graph, paper; // JointJS objects
	var initialized = false;
	var xPos = 10;
	var yPos = 10;
	var selectedElement;

	/*
	 * / Public functions
	 */
	client.render = function(obj) {
		clientConfig = JSON.parse(obj);
		setup();
	};

	var removeCellFunc = function(cell) {
		removeCell(cell);
	};

	/*
	 * / Initiate JointJS graph
	 */
	function setup() {
		// Setup element events. Cannot be done in ready as we need to wait for
		// GWT entry point to be called

		// Instantiate JointJS graph and paper
		if (!initialized) {
			$("#btn-create-comp").on("click", createNewComponent);
			$("#btn-config-save").on("click", saveConfig);
			$("#btn-config-delete").on("click", deleteComponent);

			initialized = true;

			// Set up custom elements
			setupElements();

			// Setup graph and paper
			graph = new joint.dia.Graph;

			paper = new joint.dia.Paper({
				el : $('#wires-graph'),
				width : 850,
				height : 400,
				model : graph,
				gridSize : 1,
				snapLinks : true,
				linkPinning : false,
				markAvailable : true,
				defaultLink : new joint.shapes.customLink.Element,
				restrictTranslate: function(elementView) {
				    var parentId = elementView.model.get('parent');
				    return parentId && this.model.getCell(parentId).getBBox();
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
		if (!$.isEmptyObject(clientConfig.pGraph)) {
			graph.clear();
			try {
				graph.fromJSON(clientConfig.pGraph);
			} catch (err) {
				console.log(err.stack);
			}
		}

		// If components exist in the framework but not the graph, create UI
		// elements
		if (typeof clientConfig.components != 'undefined') {
			$.each(clientConfig.components, function(index, component) {
				var exists = false;
				$.each(graph.getCells(), function(index, cell) {
					if (cell.attributes.pid === component.pid) {
						exists = true;
					}
				});
				if (!exists) {
					createComponent(component);
				}
			});
		}
		graph.on('change:source change:target', function(link) {
			createWire(link);
		});
		graph.on('remove', removeCellFunc);
	}

	/*
	 * / Create a new component
	 */
	function createComponent(comp) {
		if (comp.name === "") {
			comp.name = comp.pid;
		}

		// Setup allowed ports based on type
		if (comp.type === 'both') {
			inputPorts = [ '' ];
			outputPorts = [ '' ];
			body = 'salmon';
			attrib = {
				'.body1' : {
					fill : body
				},
				'.label' : {
					text : comp.name,
					fill : body
				}
			};
		} else if (comp.type === 'producer') {
			inputPorts = [];
			outputPorts = [ '' ];
			body = 'seaGreen';
			attrib = {
				'.body2' : {
					fill : body
				},
				'.label' : {
					text : comp.name,
					fill : body
				}
			};
		} else {
			inputPorts = [ '' ];
			outputPorts = [];
			body = 'PaleGreen';
			attrib = {
				'.body1' : {
					fill : body
				},
				'.label' : {
					text : comp.name
				}
			};
		}

		var rect = new joint.shapes.devs.Atomic({
			position : {
				x : xPos,
				y : yPos
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

		paper.on('cell:pointerdown', function(cellView, evt, x, y) {
			var pid = cellView.model.attributes.label;
			top.jsniUpdateDeleteButton(pid);
			selectedElement = cellView.model;
		});

		paper.on('blank:pointerdown', function(cellView, evt, x, y) {
			top.jsniUpdateDeleteButton("");
			selectedElement = "";
		});

		graph.addCells([ rect ]);

		/* rounded corners */
		rect.attr({
			'.body' : {
				'rx' : 6,
				'ry' : 6
			}
		});

		/* custom highlighting */
		var highlighter = V('circle', {
			'r' : 14,
			'stroke' : '#ff7e5d',
			'stroke-width' : '6px',
			'fill' : 'transparent',
			'pointer-events' : 'none'
		});

		paper.off('cell:highlight cell:unhighlight').on({
			'cell:highlight' : function(cellView, el, opt) {
				if (opt.embedding) {
					V(el).addClass('highlighted-parent');
				}

				if (opt.connecting) {
					var bbox = V(el).bbox(false, paper.viewport);
					highlighter.translate(bbox.x + 10, bbox.y + 10, {
						absolute : true
					});
					V(paper.viewport).append(highlighter);
				}
			},

			'cell:unhighlight' : function(cellView, el, opt) {
				if (opt.embedding) {
					V(el).removeClass('highlighted-parent');
				}

				if (opt.connecting) {
					highlighter.remove();
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
		newConfig = {
			jointJs : graph,
			deleteCells : delCells
		}
		top.jsniUpdateWireConfig(JSON.stringify(newConfig));
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
		if (selectedElement !== "")
			selectedElement.remove();
	}

	function createNewComponent() {
		var newComp;
		// Determine whether component can be producer, consumer, or both
		fPid = $("#factoryPid").val();
		driverPid = $("#driverPids").val();
		name = $("#componentName").val();

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
					type : cType
				}
			} else {
				newComp = {
					fPid : fPid,
					pid : "none",
					name : name,
					type : cType
				}
			}
			// Create the new component and store information in array
			createComponent(newComp);
			$("#componentName").val('');
			$("#driverPids").val('--- Select Driver ---');
			$("#factoryPid").val('');
			$("#asset-comp-modal").modal('hide');
		}
	}

	function removeCell(cell) {
		// If the cell only exists in JointJS, no need to delete from
		// OSGi framework. For components this is determined by the
		// PID equalling 'none'. For wires, the neWire value will be true.

		// Delete Wire
		if (cell.attributes.type === 'customLink.Element'
				&& !cell.attributes.newWire) {
			delCells.push({
				cellType : 'wire',
				p : cell.attributes.producer,
				c : cell.attributes.consumer
			});
		} else if (cell.attributes.type === 'devs.Model'
				&& cell.attributes.pid !== 'none') {
			delCells.push({
				cellType : 'instance',
				pid : cell.attributes.pid
			});
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