var kuraWires = (function() {
	var client = {}; // Holds accessible elements of JS library
	var clientConfig = {}; // Configuration passed from Kura OSGi
	var delCells = []; // Components and Wires to be deleted from OSGi
	// framework on save
	var graph, paper; // JointJS objects
	var initialized = false;
	var xPos = 10;
	var yPos = 10;
	var table;
	var both = {};
	var producers = {};
	var consumers = {};
	var allBlocks = {};

	/*
	 * / Public functions
	 */
	client.render = function(obj) {
		clientConfig = JSON.parse(obj);
		setup();
	};

	var removeCellFunction = function(cell) {
		removeCell(cell);
	};
	
	function funcCallback(factory) {
		return function() {
			createNewWireComponent(factory);
		};
	}
	
	var fillWireComponentsPanel = function() {
		$('#wire-components-panel').append(
				"<div id='wireCompsTableContainer'></div>");
		$('#wireCompsTableContainer').append(
				"<table id='wireCompsTable' width='160'></table>");
		table = $('#wire-components-panel').children();
		fillWireComponentsTableRows();
		allBlocks = both.concat(producers).concat(consumers);
		for (var i = 0; i < allBlocks.length; i++) {
			document.getElementsByName(allBlocks[i])[0].addEventListener('click', funcCallback(allBlocks[i]));
		}
	};

	var intersect = function(a, b) {
		var d = {};
		var results = [];
		for (var i = 0; i < b.length; i++) {
			d[b[i]] = true;
		}
		for (var j = 0; j < a.length; j++) {
			if (d[a[j]])
				results.push(a[j]);
		}
		return results;
	};

	var createNewWireComponent = function(factoryPid) {
		var logicalBlockName = prompt("Please enter logical block name", "");
		// Determine whether component can be producer, consumer, or both
		var cType, newComp, cId;
		if ($.inArray(factoryPid, clientConfig.pFactories) !== -1
				&& $.inArray(factoryPid, clientConfig.cFactories) !== -1) {
			cType = "both";
		} else if ($.inArray(factoryPid, clientConfig.pFactories) !== -1) {
			cType = "producer";
		} else {
			cType = "consumer";
		}
		if (logicalBlockName !== null) {
			newComp = {
				fPid : factoryPid,
				pid : "none",
				name : logicalBlockName,
				type : cType
			};
			// Create the new component and store information in array
			cId = createComponent(newComp);
		}
		return cId;
	};

	var fillWireComponentsTableRows = function() {
		if (typeof clientConfig.cFactories != 'undefined'
				&& typeof clientConfig.pFactories != 'undefined') {
			both = intersect(clientConfig.cFactories, clientConfig.pFactories);
			producers = clientConfig.pFactories.slice(0);
			consumers = clientConfig.cFactories.slice(0);
			var index, name;

			for (var i = 0; i < both.length; i++) {
				index = producers.indexOf(both[i]);
				if (index > -1) {
					producers.splice(index, 1);
				}
			}
			for (i = 0; i < both.length; i++) {
				index = consumers.indexOf(both[i]);
				if (index > -1) {
					consumers.splice(index, 1);
				}
			}
			for (i = 0; i < both.length; i++) {
				var factoryPid = both[i];
				name = splitFactoryName(factoryPid);
				table.append("<tr class='wirecomponent'><td class=centeredImg><img name=" + factoryPid + " src='both.png' alt='' draggable='false' style='width:120px; height:40px;'></td></tr>");
				table.append("<tr class='wirecomponent'><td class=centered>" + name + "<td></tr>");
			}
			for (i = 0; i < producers.length; i++) {
				name = splitFactoryName(producers[i]);
				table.append("<tr class='wirecomponent'><td class=centeredImg><img name=" + producers[i] + " src='emitter.png' draggable='false' style='width:120px; height:40px;'></td></tr>");
				table.append("<tr class='wirecomponent'><td class=centered>"
						+ name + "<td></tr>");
			}
			for (i = 0; i < consumers.length; i++) {
				name = splitFactoryName(consumers[i]);
				table.append("<tr class='wirecomponent'><td class=centeredImg><img name=" + consumers[i] + " src='receiver.png' draggable='false' style='width:120px; height:40px;'></td></tr>");
				table.append("<tr class='wirecomponent'><td class=centered>"+ name + "<td></tr>");
			}
		}
	};

	var splitFactoryName = function(name) {
		var res = name.split(".");
		name = res[res.length - 1];
		if (name === "DbWireRecordStore") {
			name = "Db Store";
		}
		if (name === "DbWireRecordFilter") {
			name = "Db Filter";
		}
		if (name === "WireAsset") {
			name = "Asset";
		}
		if (name === "CloudPublisher") {
			name = "Cloud Publisher";
		}
		return name;
	};

	/*
	 * / Initiate JointJS graph
	 */
	function setup() {
		// fill the wire components panel with list of available wire components
		fillWireComponentsPanel();
		// Setup element events. Cannot be done in ready as we need to wait for
		// GWT entry point to be called

		// Instantiate JointJS graph and paper
		if (!initialized) {
			$("#btn-create-comp").on("click", createNewComponent2);
			$("#btn-config-save").on("click", saveConfig);

			initialized = true;

			// Set up custom elements
			setupElements();

			// Setup graph and paper
			graph = new joint.dia.Graph();

			paper = new joint.dia.Paper({
				el : $('#wires-graph'),
				width : 850,
				height : 400,
				model : graph,
				gridSize : 20,
				snapLinks : true,
				defaultLink : new joint.shapes.customLink.Element(),
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

		graph.off('remove', removeCellFunction);

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
				exists = false;
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
		graph.on('remove', removeCellFunction);
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
			inputPorts = [ 'in' ];
			outputPorts = [ 'out' ];
		} else if (comp.type === 'producer') {
			inputPorts = [];
			outputPorts = [ 'out' ];
		} else {
			inputPorts = [ 'in' ];
			outputPorts = [];
		}

		var rect = new joint.shapes.html.Element({
			position : {
				x : xPos,
				y : yPos
			},
			size : {
				width : 170,
				height : 50
			},
			inPorts : inputPorts,
			outPorts : outputPorts,
			label : comp.name,
			factoryPid : comp.fPid,
			pid : comp.pid,
			cType : comp.type
		});

		graph.addCells([ rect ]);
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
		var newConfig = {
			jointJs : graph,
			deleteCells : delCells
		};
		top.jsniUpdateWireConfig(JSON.stringify(newConfig));
		delCells = [];
	}

	function createWire(link) {
		if ((typeof link.attributes.source.id != 'undefined')
				&& (typeof link.attributes.target.id != 'undefined')) {
			console.log("Creating new wire");
			link.set("producer", link.attributes.source.id);
			link.set("consumer", link.attributes.target.id);
			link.set("newWire", true);
			console.log(JSON.stringify(graph));
		}
	}

	function createNewComponent2() {
		// Determine whether component can be producer, consumer, or both
		var cType, fPid;
		fPid = $("#form-factory-pid").val();
		if ($.inArray(fPid, clientConfig.pFactories) !== -1
				&& $.inArray(fPid, clientConfig.cFactories) !== -1) {
			cType = "both";
		} else if ($.inArray(fPid, clientConfig.pFactories) !== -1) {
			cType = "producer";
		} else {
			cType = "consumer";
		}
		var newComp = {
			fPid : $("#form-factory-pid").val(),
			pid : "none",
			name : $("#form-factory-name").val(),
			type : cType
		};
		// Create the new component and store information in array
		cId = createComponent(newComp);
		$("#form-factory-name").val('');
		$("#factory-select-modal").modal('hide');
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
		} else if (cell.attributes.type === 'html.Element'
				&& cell.attributes.pid !== 'none') {
			delCells.push({
				cellType : 'instance',
				pid : cell.attributes.pid
			});
		}

		console.log(JSON.stringify(delCells));
	}

	/*
	 * / Setup Custom Elements
	 */
	function setupElements() {

		delCells = [];

		joint.shapes.customLink = {};
		joint.shapes.customLink.Element = joint.dia.Link.extend({
			defaults : joint.util.deepSupplement({
				type : 'customLink.Element',
				attrs : {
					'.connection' : {
						'stroke-width' : 2
					},
					'.marker-target' : {
						d : 'M 10 0 L 0 5 L 10 10 z'
					}
				},
				producer : 'producer',
				consumer : 'consumer',
				newWire : false
			}, joint.dia.Link.prototype.defaults)
		});

		// Create a custom element.
		// ------------------------
		joint.shapes.html = {};
		joint.shapes.html.Element = joint.shapes.devs.Model.extend({
			defaults : joint.util.deepSupplement({
				type : 'html.Element',
				attrs : {
					rect : {
						rx : 5,
						ry : 5,
						fill : "#2ECC71",
						stroke : '#27AE60',
						'stroke-width' : 1
					},
					'.inPorts circle' : {
						fill : '#16A085',
						magnet : 'passive',
						type : 'input',
						r : 5
					},
					'.outPorts circle' : {
						fill : '#E74C3C',
						type : 'output',
						r : 5
					}
				}
			}, joint.shapes.devs.Model.prototype.defaults)
		});
		// Create a custom view for that element that displays an HTML div above
		// it.
		// -------------------------------------------------------------------------
		joint.shapes.html.ElementView = joint.shapes.devs.ModelView.extend({

			template : [ '<div class="html-element">',
					'<button class="delete">x</button>', '<label></label>',
					'</div>' ].join(''),

			initialize : function() {
				_.bindAll(this, 'updateBox');
				joint.dia.ElementView.prototype.initialize.apply(this,
						arguments);

				this.$box = $(_.template(this.template)());
				// Prevent paper from handling pointerdown.
				this.$box.find('input,select').on('mousedown click',
						function(evt) {
							evt.stopPropagation();
						});
				this.$box.find('select').val(this.model.get('select'));
				this.$box.find('.delete').on('click',
						_.bind(this.model.remove, this.model));
				// Update the box position whenever the underlying model
				// changes.
				this.model.on('change', this.updateBox, this);
				// Remove the box when the model gets removed from the graph.
				this.model.on('remove', this.removeBox, this);

				this.updateBox();
			},
			render : function() {
				joint.dia.ElementView.prototype.render.apply(this, arguments);
				this.paper.$el.prepend(this.$box);
				this.updateBox();
				return this;
			},
			updateBox : function() {
				// Set the position and dimension of the box so that it covers
				// the JointJS element.
				var bbox = this.model.getBBox();
				// Example of updating the HTML with a data stored in the cell
				// model.
				this.$box.find('label').text(this.model.get('label'));
				this.$box.css({
					width : bbox.width,
					height : bbox.height,
					left : bbox.x,
					top : bbox.y,
					transform : 'rotate(' + (this.model.get('angle') || 0)
							+ 'deg)'
				});
			},
			removeBox : function(evt) {
				this.$box.remove();
			}
		});
	}

	return client;

}());