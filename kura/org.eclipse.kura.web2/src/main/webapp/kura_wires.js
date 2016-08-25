var kuraWires = (function() {
	var client = {};			// Holds accessible elements of JS library
	var clientConfig = {};		// Configuration passed from Kura OSGi
	var delCells = [];			// Components and Wires to be deleted from OSGi framework on save
	var graph, paper;			// JointJS objects
	var initialized = false;
	var xPos = 10;
	var yPos = 10;

	/*
	/ Public functions
	*/
	client.render = function(obj) {
		clientConfig = JSON.parse(obj);
		setup();
	};

   var removeCellFunction = function(cell){
      removeCell(cell);
   }
	/*
	/ Initiate JointJS graph
	*/
	function setup() {
		// Setup element events. Cannot be done in ready as we need to wait for
		// GWT entry point to be called

		// Instantiate JointJS graph and paper
		if (!initialized) {
			$("#btn-create-comp").on("click", createNewComponent);
			$("#btn-config-save").on("click", saveConfig);
			
			initialized = true;

			// Set up custom elements
			setupElements();

			// Setup graph and paper
			graph = new joint.dia.Graph;
			
	        paper = new joint.dia.Paper({
	            el: $('#wires-graph'),
	            width: 850,
	            height: 400,
	            model: graph,
	            gridSize: 20,
				snapLinks: true,
				defaultLink: new joint.shapes.customLink.Element,
				validateConnection: function(cellViewS, magnetS, cellViewT, magnetT, end, linkView) {
			        // Prevent linking from input ports.
			        if (magnetS && magnetS.getAttribute('type') === 'input') return false;
			        // Prevent linking from output ports to input ports within one element.
			        if (cellViewS === cellViewT) return false;
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
			} catch(err) {
				console.log(err.stack);
			}
		}

		// If components exist in the framework but not the graph, create UI elements
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

		graph.on('change:source change:target', function(link) { createWire(link); });
      graph.on('remove', removeCellFunction);
	}

	/*
	/ Create a new component
	*/
	function createComponent(comp) {
		if (comp.name === "") {
			comp.name = comp.pid;
		}

		// Setup allowed ports based on type
		if (comp.type === 'both') {
			inputPorts = ['in'];
			outputPorts = ['out'];
		}
		else if (comp.type === 'producer') {
			inputPorts = [];
			outputPorts = ['out'];
		}
		else {
			inputPorts = ['in'];
			outputPorts = [];
		}

		var rect = new joint.shapes.html.Element({
			position: { x: xPos, y: yPos },
            size: { width: 170	, height: 50 },
			inPorts: inputPorts,
    		outPorts: outputPorts,
			label: comp.name,
			factoryPid: comp.fPid,
			pid: comp.pid,
			cType: comp.type
		});

		graph.addCells([rect]);
		xPos = xPos + 212;
		if (xPos > 500) {
			xPos = 300;
			yPos = 300;
		}

		return rect.attributes.id;
	}

	/*
	/ Event Functions
	*/
	function saveConfig() {
		newConfig = {
			jointJs: graph,
			deleteCells: delCells
		}
		top.jsniUpdateWireConfig(JSON.stringify(newConfig));
      delCells = [];
	}

	function createWire(link) {
		if ((typeof link.attributes.source.id != 'undefined') && (typeof link.attributes.target.id != 'undefined')) {
			console.log("Creating new wire");
			link.set("producer", link.attributes.source.id);
			link.set("consumer", link.attributes.target.id);
			link.set("newWire", true);
			console.log(JSON.stringify(graph));
		}
	}

	function createNewComponent() {
		// Determine whether component can be producer, consumer, or both
		fPid = $("#form-factory-pid").val();
		if ($.inArray(fPid, clientConfig.pFactories) !== -1 && $.inArray(fPid, clientConfig.cFactories) !== -1 ) {
			cType = "both";
		}
		else if ($.inArray(fPid, clientConfig.pFactories) !== -1) {
			cType = "producer";
		}
		else {
			cType = "consumer";
		}
		newComp = {
			fPid: $("#form-factory-pid").val(),
			pid: "none",
			name: $("#form-factory-name").val(),
			type: cType
		}
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
		if (cell.attributes.type === 'customLink.Element' && !cell.attributes.newWire) {
			delCells.push({ cellType: 'wire', p: cell.attributes.producer, c: cell.attributes.consumer });
		}
		else if (cell.attributes.type === 'html.Element' && cell.attributes.pid !== 'none') {
			delCells.push({ cellType: 'instance', pid: cell.attributes.pid });
		}

		console.log(JSON.stringify(delCells));
	}

	/*
    / Setup Custom Elements
    */
    function setupElements() {

      delCells = [];

 	   joint.shapes.customLink = {};
 	   joint.shapes.customLink.Element = joint.dia.Link.extend({
 		   defaults: joint.util.deepSupplement({
 			   type: 'customLink.Element',
 			   attrs: {
 				   '.connection': {'stroke-width': 2},
 				   '.marker-target': {d: 'M 10 0 L 0 5 L 10 10 z'}
 			   },
 			   producer: 'producer',
 			   consumer: 'consumer',
 			   newWire: false
 		   }, joint.dia.Link.prototype.defaults)
 	   });

 	   // Create a custom element.
 	   // ------------------------
 	   joint.shapes.html = {};
 	   joint.shapes.html.Element = joint.shapes.devs.Model.extend({
 		   defaults: joint.util.deepSupplement({
 			   type: 'html.Element',
 			   attrs: {
 				   rect: { stroke: 'none', 'fill-opacity': 0 },
 				   '.inPorts circle': { fill: '#16A085', magnet: 'passive', type: 'input' },
 				   '.outPorts circle': { fill: '#E74C3C', type: 'output' }
 			   }
 		   }, joint.shapes.devs.Model.prototype.defaults)
 	   });
 	   //joint.shapes.html.ElementView = joint.shapes.devs.ModelView;
 	   // Create a custom view for that element that displays an HTML div above it.
 	   // -------------------------------------------------------------------------
 	   joint.shapes.html.ElementView = joint.shapes.devs.ModelView.extend({

 		   template: [
 			   '<div class="html-element">',
 			   '<button class="delete">x</button>',
 			   '<label></label>',
 			   '</div>'
 		   ].join(''),

 		   initialize: function() {
 			   _.bindAll(this, 'updateBox');
 			   joint.dia.ElementView.prototype.initialize.apply(this, arguments);

 			   this.$box = $(_.template(this.template)());
 			   // Prevent paper from handling pointerdown.
 			   this.$box.find('input,select').on('mousedown click', function(evt) { evt.stopPropagation(); });
 			   this.$box.find('select').val(this.model.get('select'));
 			   this.$box.find('.delete').on('click', _.bind(this.model.remove, this.model));
 			   // Update the box position whenever the underlying model changes.
 			   this.model.on('change', this.updateBox, this);
 			   // Remove the box when the model gets removed from the graph.
 			   this.model.on('remove', this.removeBox, this);

 			   this.updateBox();
 		   },
 		   render: function() {
 			   joint.dia.ElementView.prototype.render.apply(this, arguments);
 			   this.paper.$el.prepend(this.$box);
 			   this.updateBox();
 			   return this;
 		   },
 		   updateBox: function() {
 			   // Set the position and dimension of the box so that it covers the JointJS element.
 			   var bbox = this.model.getBBox();
 			   // Example of updating the HTML with a data stored in the cell model.
 			   this.$box.find('label').text(this.model.get('label'));
 			   this.$box.css({ width: bbox.width, height: bbox.height, left: bbox.x, top: bbox.y, transform: 'rotate(' + (this.model.get('angle') || 0) + 'deg)' });
 		   },
 		   removeBox: function(evt) {
 			   //removeComponent(this.model.id);
 			   this.$box.remove();
 		   }
 	   });
    }

   return client;

}());
