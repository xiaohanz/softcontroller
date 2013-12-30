/** COMMON * */
var labelType, useGradients, nativeTextSupport, animate;

(function() {
    var ua = navigator.userAgent, iStuff = ua.match(/iPhone/i)
            || ua.match(/iPad/i), typeOfCanvas = typeof HTMLCanvasElement, nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'), textSupport = nativeCanvasSupport
            && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
    // I'm setting this based on the fact that ExCanvas provides text support
    // for IE
    // and that as of today iPhone/iPad current text support is lame
    labelType = (!nativeCanvasSupport || (textSupport && !iStuff)) ? 'Native'
            : 'HTML';
    nativeTextSupport = labelType == 'Native';
    useGradients = nativeCanvasSupport;
    animate = !(iStuff || !nativeCanvasSupport);
})();

/** TOPOLOGY * */
one.topology = {};
one.topology.graph=null;
one.topology.refreshInterval = null; 
var auto_refresh=null;
var result = [];
one.topology.hostSelectConfig = {
    id: {
	    modal: {
		    hosts : "one_topology_hostSelectConfig_id_modal_hosts",
			submit: "one_topology_hostSelectConfig_id_modal_submit",
			cancel: "one_topology_hostSelectConfig_id_modal_cancel",
			modal: "one_topology_hostSelectConfig_id_modal_modal",
			form: {
			    srcHostId : "one_topology_hostSelectConfig_id_modal_form_srcHostId",
			    srcHostName: "one_topology_hostSelectConfig_id_modal_form_srcHostName",
			    srcHostDescription: "one_topology_hostSelectConfig_id_modal_form_srcHostIpAddress",
			    targetHostName: "one_topology_hostSelectConfig_id_modal_form_targetHostName",
		     }
		}
    },
    registry: {},
	modal: {
        initialize: {
		    hosts: function(node) {
			    var h3 = "Host Select";
			    var footer = one.topology.hostSelectConfig.modal.footer();
			    var $modal = one.lib.modal.spawn(one.topology.hostSelectConfig.id.modal.hosts, h3, "", footer);
			    $('#' + one.topology.hostSelectConfig.id.modal.submit, $modal).click(function() {
			        one.topology.hostSelectConfig.modal.submit.hosts($modal);
			    });
			    // bind cancel button
			    $('#'+one.topology.hostSelectConfig.id.modal.cancel, $modal).click(function() {
			        $modal.modal('hide');
			    });
			    var $body = one.topology.hostSelectConfig.modal.body.hosts(node);
		        one.lib.modal.inject.body($modal, $body);
			    return $modal;
			}   //hosts end
	    },   //initialize end
	    body: {
			hosts: function(node){
			    var $form = $(document.createElement('form'));
			    var $fieldset = $(document.createElement('fieldset'));			              			               
			                   		                 		                           
                // Src host name 
		 	    var $label = one.lib.form.label("Src Host Name");
                var $input = one.lib.form.input();
                $input.attr('value', node.name);
                $input.attr("disabled", true);
				$input.attr('id', one.topology.hostSelectConfig.id.modal.form.srcHostName);
	            $fieldset.append($label).append($input);

				// Src host id 
			    var $label = one.lib.form.label("Src Host Id");
                var $input = one.lib.form.input();
                $input.attr('value', node.id);
                $input.attr("disabled", true);
				$input.attr('id', one.topology.hostSelectConfig.id.modal.form.srcHostId);
	            $fieldset.append($label).append($input);
			           
			    // Src host description
	            var srcHostDescription = (node.data['$desc'] === 'None' || node.data['$desc'] === '' ? node.name : node.data['$desc']);
				var $label = one.lib.form.label("Src Host Description");
                var $input = one.lib.form.input();
                $input.attr('value', srcHostDescription);
                $input.attr("disabled", true);
				$input.attr('id', one.topology.hostSelectConfig.id.modal.form.srcHostDescription);
	            $fieldset.append($label).append($input);
			                
			    //select a target host name 
			    var $label = one.lib.form.label("Target Host Name");
				var targetHostsName={};
				for ( var idx in one.topology.graph.graph.nodes){
				    var currentNode = one.topology.graph.graph.nodes[idx];
				    var currentNodeName = (currentNode.data['$desc'] === 'None' || currentNode.data['$desc'] === '' ? currentNode.name : currentNode.data['$desc']);
					if(currentNode.name==node.name){
					    continue;
					}
					if(currentNode.data["$type"] == "host"){
                        targetHostsName[currentNodeName]=currentNodeName;
					}
				}

				var $select = one.lib.form.select.create(targetHostsName);
				$select.attr('id', one.topology.hostSelectConfig.id.modal.form.targetHostName);
                one.lib.form.select.prepend($select, { '' : 'Please Select a host' });
                $select.val($select.find("option:first").val());
                $fieldset.append($label).append($select);
				$form.append($fieldset);
			    return $form;
			}//hosts end
		},//body end
		       	
	    submit: {
		    hosts: function($modal) {
		        var result = {};
		        result['srcHostName'] = $('#' + one.topology.hostSelectConfig.id.modal.form.srcHostName, $modal).val();
		        result['targetHostName'] = $('#' + one.topology.hostSelectConfig.id.modal.form.targetHostName, $modal).val();
		        if(result['targetHostName']==""||result['targetHostName']=='Please Select a host'){
		            alert("Please Select a host!")
		        }
		        var srcHostNode =one.topology.graph.graph.getByName(result['srcHostName']);
		        var targetHostNode =one.topology.graph.graph.getByName(result['targetHostName']);
		         		    		         		    
		        var srcSwitchNode;
		        var targetSwitchNode;
		        srcHostNode.eachAdjacency(function(adj) {
		            srcSwitchNode=adj.nodeTo;
		         	adj.setDataset('end', {
		         	    lineWidth: 5	  
		         	});
		        });
		        targetHostNode.eachAdjacency(function(adj) {
		            targetSwitchNode=adj.nodeTo;
		         	adj.setDataset('end', {
		         	    lineWidth: 5
		         	});
		        });
		        one.topology.graph.graph.computeLevels(srcHostNode.id);
		        var nodeDepth=targetSwitchNode._depth;
		        var currentNode = targetSwitchNode;
		        srcHostNode.selected = true;
		        for(i=0;i<nodeDepth-1;i++){
		            var parents=currentNode.getParents();
		            var adj=parents[0].getAdjacency(currentNode.id);
		                       
		            adj[0].setDataset('end', {
		    		    lineWidth: 5
		            });
		            var adj=currentNode.getAdjacency(parents[0].id);
			                       
		            adj[0].setDataset('end', {
		    		    lineWidth: 5,
		            });
		                     
		            currentNode = parents[0];
		        }
		        $modal.modal('hide');
		        //trigger animation to final styles
		        one.topology.graph.fx.animate({
		            modes: ['edge-property:lineWidth'],
		            duration: 500
		        });
		    }
	    },
		footer : function() {
	        var footer = [];
		    var submitButton = one.lib.dashlet.button.single("Submit", one.topology.hostSelectConfig.id.modal.submit, "btn-primary", "");
			var $submitButton = one.lib.dashlet.button.button(submitButton);
			footer.push($submitButton);
			             
			var cancel = one.lib.dashlet.button.single('Cancel', one.topology.hostSelectConfig.id.modal.cancel, '', '');
			var $cancel = one.lib.dashlet.button.button(cancel);
			footer.push($cancel);
			return footer;
		}//footer end
    }//modal end 
},
			
one.topology.setTimeConfig = {
    id: {
	    modal: {
		    time : "one_topology_portStatisticConfig_id_modal_time",
			submit: "one_topology_portStatisticConfig_id_modal_submit",
			cancel: "one_topology_portStatisticConfig_id_modal_cancel",
			modal: "one_topology_portStatisticConfig_id_modal_modal",
			form: {
				Time: "one_topology_portStatisticConfig_id_modal_form_Time"
			}
		}
	},
	registry: {},
	modal: {
	    initialize: {
		    time: function() {
			    var h3 = "Set a Time to Refresh Topography Automatically";
				var footer = one.topology.setTimeConfig.modal.footer();
				// bind submit button	           
				var $modal = one.lib.modal.spawn(one.topology.setTimeConfig.id.modal.time, h3, "", footer);
				$('#' + one.topology.setTimeConfig.id.modal.submit, $modal).click(function() {
				    one.topology.setTimeConfig.modal.submit($modal);
				});
				// bind cancel button
				$('#'+one.topology.setTimeConfig.id.modal.cancel, $modal).click(function() {
				    $modal.modal('hide');
				});
				var $body = one.topology.setTimeConfig.modal.body.time();
				one.lib.modal.inject.body($modal, $body);
				return $modal;
			}  
		},  
		body: {
		    time: function(){
			    var $form = $(document.createElement('form'));
				var $fieldset = $(document.createElement('fieldset'));			              			               
		                          
			    var $label = one.lib.form.label("Please Input a Time(unit:second)");
				var $input = one.lib.form.input((one.topology.refreshInterval===null||(one.topology.refreshInterval==0))?"Please input a time!":(one.topology.refreshInterval/1000));
				$input.attr('id', one.topology.setTimeConfig.id.modal.form.Time);
				$fieldset.append($label).append($input);
			    $form.append($fieldset);
			    return $form;   
			}
		},
				       	
		submit: function($modal) {
			$time = $('#' + one.topology.setTimeConfig.id.modal.form.Time, $modal).val();
			if(($time!="")&&($time!=0)){
				result.length=0;
				result.push($time);
			    if(isNaN(result[0])||(result[0]=="")){
				    alert("Please input a valid number!");
			    }
			    else{
					one.topology.refreshInterval =result[0]*1000;
					$modal.modal('hide');
				}
			}
			else{
				if((one.topology.refreshInterval!=0)&&(one.topology.refreshInterval!=null)){
					$modal.modal('hide');
				}
				else{
					alert("Please input a valid number!");
				}
			}
		},
			
		footer : function() {
		    var footer = [];
			var submitButton = one.lib.dashlet.button.single("Submit", one.topology.setTimeConfig.id.modal.submit, "btn-primary", "");
			var $submitButton = one.lib.dashlet.button.button(submitButton);
			footer.push($submitButton);
					             
			var cancel = one.lib.dashlet.button.single('Cancel', one.topology.setTimeConfig.id.modal.cancel, '', '');
			var $cancel = one.lib.dashlet.button.button(cancel);
			footer.push($cancel);
			return footer;
		}//footer end
	}//modal end 
}, 

one.topology.option = {
    navigation : function(enable, panning, zooming) {
        var option = {};
        option["enable"] = enable;
        option["panning"] = panning;
        option["zooming"] = zooming;
        return option;
    },
    node : function(overridable, color, height, dim) {
        var option = {};
        option["overridable"] = overridable;
        option["color"] = color;
        option["height"] = height;
        option["dim"] = dim;
        return option;
    },
    edge : function(overridable, color, lineWidth, epsilon) {
        var option = {};
        option["overridable"] = overridable;
        option["color"] = color;
        option["lineWidth"] = lineWidth;
        if (epsilon != undefined)
            option["epsilon"] = epsilon;
        return option;
    },
    label : function(style, node) {
        var marginTop, minWidth;
        if (node.data["$type"] == "switch") {
            marginTop = "42px";
            minWidth = "65px";
        } else if (node.data["$type"] == "host") {
            marginTop = "48px";
            minWidth = "";
        } else if (node.data["$type"].indexOf("monitor") == 0) {
            marginTop = "52px";
            minWidth = "";
        }
        style.marginTop = marginTop;
        style.minWidth = minWidth;
        style.background = "rgba(68,68,68,0.7)";
        style.borderRadius = "4px";
        style.color = "#fff";
        style.cursor = "default";
    }
};

one.topology.init = function(json) {
    if (json.length == 0) {
        $div = $(document.createElement('div'));
        $img = $(document.createElement('div'));
        $img.css('height', '90px');
        $img.css('width', '160px');
        $img.css('background-image', 'url(/img/topology_view_1033_128.png)');
        $img.css('clear', 'both');
        $img.css('margin', '0 auto');
        $p = $(document.createElement('p'));
        $p.addClass('text-center');
        $p.addClass('text-info');
        $p.css('width', '100%');
        $p.css('padding', '10px 0');
        $p.css('cursor', 'default');
        $p.append('No Network Elements Connected');
        $div.css('position', 'absolute');
        $div.css('top', '25%');
        $div.css('margin', '0 auto');
        $div.css('width', '100%');
        $div.css('text-align', 'center');
        $div.append($img).append($p);
        $("#topology").append($div);
        return false;
    }
    one.topology.graph = new $jit.MultiTopology(
            {
                injectInto : 'topology',
                Navigation : one.topology.option.navigation(true,
                        'avoid nodes', 10),
                Node : one.topology.option.node(true, '#444', 25, 27),
                Edge : one.topology.option.edge(true, '23A4FF', 1.5),
                Tips : {
                    enable : true,
                    type : 'Native',
                    offsetX: 15,
                    offsetY: 15,
                    onShow : function(tip, node) {
                        if (node.name != undefined)
                            { // node tooltip
        		            one.topology.tooltip.node.tooltip(tip, node);
        		        } 
                        else { // edge tooltip
        		             one.topology.tooltip.edge.tooltip(tip, node);
        		        }
                    }
                },
                Events : {
                    enable : true,
                    type : 'Native',
                    onMouseEnter : function(node, eventInfo, e) {
                        // if node
                        if (node.id != undefined) {
                            one.topology.graph.canvas.getElement().style.cursor = 'move';
                        } else if (eventInfo.edge != undefined
                                && eventInfo.edge.nodeTo.data["$type"] == "switch"
                                && eventInfo.edge.nodeFrom.data["$type"] == "switch") {
                            one.topology.graph.canvas.getElement().style.cursor = 'pointer';
                        }
                    },
                    onMouseLeave : function(node, eventInfo, e) {
                        one.topology.graph.canvas.getElement().style.cursor = '';
                    },
                    // Update node positions when dragged
                    onDragMove : function(node, eventInfo, e) {
                        var pos = eventInfo.getPos();
                        node.pos.setc(pos.x, pos.y);
                        one.topology.graph.plot();
                        one.topology.graph.canvas.getElement().style.cursor = 'crosshair';
                    },
                    // Implement the same handler for touchscreens
                    onTouchMove : function(node, eventInfo, e) {
                        $jit.util.event.stop(e); // stop default touchmove
                        // event
                        this.onDragMove(node, eventInfo, e);
                    },
                    onDragEnd : function(node, eventInfo, e) {
                        var ps = eventInfo.getPos();
                        var did = node.id;
                        var data = {};
                        data['x'] = ps.x;
                        data['y'] = ps.y;
                        $.post('/controller/web/topology/node/' + did, data);
                    },
                    onClick : function(node, eventInfo, e) {
                        if(node.data["$type"] == "host"){
                             one.topology.graph.graph.eachNode(function(n) {
                                 if(n.id != node.id) delete n.selected;
                    	         n.eachAdjacency(function(adj) {
                    	             adj.setDataset('end', {
                    	                 lineWidth: 1.5
                    	             });
                    	          });
                              });
                             one.topology.graph.fx.animate({
 		                         modes: ['edge-property:lineWidth'],
 		                         duration: 500
 		                     });
                             if(!node.selected){
                	             var $modal = one.topology.hostSelectConfig.modal.initialize.hosts(node);
                	             $modal.modal();
                             }
                             else {
                                 delete node.selected;
                             }
                        }
				        else {
                            if (one.f.topology === undefined) {
                                 return false;
                            } 
                            else {
                                one.f.topology.Events.onClick(node, eventInfo);
                            }
                        }
                    }
                },
                iterations : 200,
                levelDistance : 130,
                onCreateLabel : function(domElement, node) {
                    var nameContainer = document.createElement('span'), closeButton = document
                            .createElement('span'), style = nameContainer.style;
                    nameContainer.className = 'name';
                    var nodeDesc = node.data["$desc"];
                    if (nodeDesc == "None" || nodeDesc == ""
                            || nodeDesc == "undefined" || nodeDesc == undefined) {
                        nameContainer.innerHTML = "<small>" + node.name
                                + "</small>";
                    } else {
                        nameContainer.innerHTML = nodeDesc;
                    }
                    domElement.appendChild(nameContainer);
                    style.fontSize = "1.0em";
                    style.color = "#000";
                    style.fontWeight = "bold";
                    style.width = "100%";
                    style.padding = "1.5px 4px";
                    style.display = "block";

                    one.topology.option.label(style, node);
                },
                onPlaceLabel : function(domElement, node) {
                    var style = domElement.style;
                    var left = parseInt(style.left);
                    var top = parseInt(style.top);
                    var w = domElement.offsetWidth;
                    style.left = (left - w / 2) + 'px';
                    style.top = (top - 15) + 'px';
                    style.display = '';
                    style.minWidth = "28px";
                    style.width = "auto";
                    style.height = "28px";
                    style.textAlign = "center";
                }
            });

    one.topology.graph.loadJSON(json);
    // compute positions incrementally and animate.
    one.topology.graph.computeIncremental({
        iter : 40,
        property : 'end',
        onStep : function(perc) {
            console.log(perc + '% loaded');
        },
        onComplete : function() {
            for ( var idx in one.topology.graph.graph.nodes) {
                var node = one.topology.graph.graph.nodes[idx];
                if (node.getData("x") && node.getData("y")) {
                    var x = node.getData("x");
                    var y = node.getData("y");
                    node.setPos(new $jit.Complex(x, y), "end");
                }
            }
            console.log('done');
            one.topology.graph.animate({
                modes : [ 'linear' ],
                transition : $jit.Trans.Elastic.easeOut,
                duration : 0
            });
        }
    });
    one.topology.graph.canvas.setZoom(0.8, 0.8);
}

function startRefreshTopology() {
    $.getJSON(one.global.remoteAddress + "controller/web/topology/visual.json",
	function(data) {
	    one.topology.checkAndRefreshTopology(data); 	    	
	});
}
one.topology.checkAndRefreshTopology= function(data){
    if(one.topology.graph!=null){
	    var  nodesNumber=0;
		for ( var idx in one.topology.graph.graph.nodes){
		    nodesNumber++;
		}
	    if(data.length !=nodesNumber){
	        delete one.topology.graph;
	    	one.topology.graph=null;
	        $('#topology').empty();
		    one.topology.init(data);
        }
	}
	else
	{
		if(data.length !=0)
		{
			$('#topology').empty();
			one.topology.init(data);	
		}
	}
}

one.topology.update = function() {
    $('#topology').empty();
    $.getJSON(one.global.remoteAddress + "controller/web/topology/visual.json",
            function(data) {
                one.topology.init(data);
            });
}

/** INIT */
$.getJSON(one.global.remoteAddress + "controller/web/topology/visual.json",
        function(data) {
            one.topology.init(data);
        });

/** TOOLTIP **/
one.topology.tooltip = {};

/** TOOLTIP UTIL **/
one.topology.tooltip.util = {
	label : function(type, text) {
		var $span = $(document.createElement('span'));
		$span
			.addClass('label')
			.addClass(type)
			.append(text);
		return $span;
	}
}

/** NODE TOOLTIP **/
one.topology.tooltip.node = {
	id : {
		tooltip : 'one-topology-tooltip-node-id-tooltip',
		title : 'one-topology-tooltip-node-id-title'
	},
	tooltip : function(tip, node) {
		
		if (node.data['$type'] == 'host') {
			tip.innerHTML = "Name : " + node.name + "<br>";
            if(node.data["$type"]!=undefined)
                tip.innerHTML = tip.innerHTML + "Type : " + node.data["$type"] + "<br>";
            if(node.data["$desc"]!=undefined)
                tip.innerHTML = tip.innerHTML + "Description : " + node.data["$desc"];
			return false;
		}
		// tooltip
		var $tooltip = $(document.createElement('div'));
		$tooltip
			.addClass(one.topology.tooltip.node.id.tooltip)
			.addClass('well')
			.addClass('well-small');
		var attributes = ['table-striped', 'table-bordered', 'table-hover', 'table-condensed', 'table-cursor'];
		var $table = one.lib.dashlet.table.table(attributes);
		var headers = ['Source', 'Destination'];
		var $thead = one.lib.dashlet.table.header(headers);
		$table.append($thead);
		// title - node name/id
		(function() {
			var nodeName = (node.data['$desc'] === 'None' || node.data['$desc'] === '' ? node.name : node.data['$desc']);
			var $span = one.topology.tooltip.util.label('label-default', nodeName);
			$tooltip.append($span);
		})();
		// adjancencies
		var body = [];
		node.eachAdjacency(function(adj) {
			var fromType = adj.nodeFrom.data['$type'],
				toType = adj.nodeTo.data['$type'];
			var nodeFrom = (adj.data['$descFrom'] === '' ? adj.nodeFrom.name : adj.data['$descFrom']);
			var nodeTo = (adj.data['$descTo'] === '' ? adj.nodeTo.name : adj.data['$descTo']);
			var portFrom = (adj.data['$nodeFromPortDescription'] === '' ? adj.portFrom : adj.data['$nodeFromPortDescription']+' ('+adj.portFrom+')');
			var portTo = (adj.data['$nodeToPortDescription'] === '' ? adj.portTo : adj.data['$nodeToPortDescription']+' ('+adj.portTo+')');

			// create table row entry
			var tr = {};
			var entry = [];

			// node type matters due to ordinal nature of src/dest
			if (fromType === 'switch' && toType === 'switch') {
				var $sourceEntryNode = one.topology.tooltip.util.label('label-info', nodeFrom);
				var $sourceEntryPort = one.topology.tooltip.util.label('label-warning', portFrom);
				var $destinationEntryNode = one.topology.tooltip.util.label('label-info', nodeTo);
				var $destinationEntryPort = one.topology.tooltip.util.label('label-warning', portTo);

				entry.push($sourceEntryNode[0].outerHTML+' '+$sourceEntryPort[0].outerHTML);
				entry.push($destinationEntryNode[0].outerHTML+' '+$destinationEntryPort[0].outerHTML);
			}

			tr.entry = entry;
			body.push(tr);
		});
		var $tbody = one.lib.dashlet.table.body(body, headers);
		$table.append($tbody);
		$tooltip.append($table);

		tip.innerHTML = $tooltip[0].outerHTML;
	}
}

/** EDGE TOOLTIP **/
one.topology.tooltip.edge = {
	id : {
		tooltip : 'one-topology-tooltip-edge-id-tooltip',
		title : 'one-topology-tooltip-edge-id-title'
	},
	tooltip : function(tip, node) {
		var $tooltip = $(document.createElement('div'));
		$tooltip
			.addClass(one.topology.tooltip.edge.id.tooltip)
			.addClass('well')
			.addClass('well-small');
		var attributes = ['table-striped', 'table-bordered', 'table-hover', 'table-condensed', 'table-cursor'];
		var $table = one.lib.dashlet.table.table(attributes);
		var headers = ['SourcePort', 'RxPkts', 'TxPkts'];
		var $thead = one.lib.dashlet.table.header(headers);
		
		$table.append($thead);
		var $span = $(document.createElement('span'));
		var fromType = node.nodeFrom.data['$type'],
				toType = node.nodeTo.data['$type'];
		var nodeFrom = (node.data['$descFrom'] === '' ? node.nodeFrom.name : node.data['$descFrom']);
		var nodeTo = (node.data['$descTo'] === '' ? node.nodeTo.name : node.data['$descTo']);
		var portTo=null;
		if(node.data['$nodeToPortDescription']){
			portTo=node.data['$nodeToPortDescription']+' ('+node.portTo+')';
		}
		else{
			portTo=node.nodeTo.name+":"+node.portTo;
		}
		
		if(node.data['$nodeFromPortDescription']){
			portFrom=node.data['$nodeFromPortDescription']+' ('+node.portFrom+')';
		}
		else{
			portFrom=node.nodeFrom.name+":"+node.portFrom;
		}
		
		var srcNode=one.topology.graph.graph.getByName(nodeFrom);
		var destNode=one.topology.graph.graph.getByName(node.nodeTo.name);
		var nodeId=(srcNode.data['$type']=="switch" ? srcNode.id : destNode.id);
		var subFrom=nodeFrom.substring(3,nodeFrom.length);
		var subTo=nodeTo.substring(3,nodeTo.length);
		var src=("OF|"+node.portFrom+"@"+subFrom);
		var dest=("OF|"+node.portTo+"@"+subTo);

		var $sourceEntryNode = one.topology.tooltip.util.label('label-info', nodeFrom);
		var $sourceEntryPort = one.topology.tooltip.util.label('label-warning', portFrom);
		var $destinationEntryNode = one.topology.tooltip.util.label('label-info', nodeTo);
		var $destinationEntryPort = one.topology.tooltip.util.label('label-warning', portTo);

		var $link = one.topology.tooltip.util.label('label-default', '-');

		$tooltip.append($sourceEntryNode[0].outerHTML+' '+$sourceEntryPort[0].outerHTML+' '+$link[0].outerHTML+' '+"<br>"
				+$destinationEntryNode[0].outerHTML+' '+$destinationEntryPort[0].outerHTML);
		
	    var body = [];
	    // properties
	    // only get one nodeconnector, because that's sufficient enough
	    // to assume the property for an edge
	    var sRPkt=null;
	    var sTPkt=null;
	    $.getJSON(one.main.constants.address.prefix + "/troubleshoot/portStats?nodeId=" + nodeId, function(content) {
	        var i=0;
		    for(i=0;i<content.nodeData.length;i++){
		        if(content.nodeData[i].nodeConnector==src){
			        var sRPkt=content.nodeData[i].rxPkts;
				    var sTPkt=content.nodeData[i].txPkts;
				    break;
			    }
		        if(content.nodeData[i].nodeConnector==dest){
		        	var sTPkt=content.nodeData[i].rxPkts;
				    var sRPkt=content.nodeData[i].txPkts;
				    break;
		        }
		    }
		    var sRPkts=sRPkt;
            var sTPkts=sTPkt;
            var tr = {};
		    var entry = [];
		    entry.push(portFrom);
		    entry.push(sRPkts);
		    entry.push(sTPkts);
		    tr.entry = entry;
		    body.push(tr);
		    var $tbody = one.lib.dashlet.table.body(body, headers);
		    $table.append($tbody);
	  	    $tooltip.append($table);
		
		    tip.innerHTML = $tooltip[0].outerHTML;
		});
	}
}


$(function(){
    $.contextMenu({
       selector: '#topology', 
       items: {
    	   "refresh": {name: "immediateRefresh", icon: "refresh",
    	       callback: function(key, options) {
    			   one.topology.update();
    	       }
           },
           "sep1": "---------",
           "startRefresh": {
        	   name: "startRefresh", icon: "refresh",
        	   disabled: function(key, opt) { 
                   return this.data('startRefreshDisabled'); 
               },
        	   callback: function(key, opt){
        		   if(one.topology.refreshInterval==null){
        			   var $modal = one.topology.setTimeConfig.modal.initialize.time();
            	       $modal.modal();
        		   }
        		   else{
        			   clearInterval(auto_refresh);
       				   auto_refresh=setInterval(startRefreshTopology,one.topology.refreshInterval);
       	               this.data('startRefreshDisabled', !this.data('stopRefreshDisabled'));
 			           this.data('stopRefreshDisabled', !this.data('stopRefreshDisabled'));
 				    }
        	   }
           }, 
           "sep2": "---------",
           "configRefreshTime": {
        	   name: "configRefreshTime", icon: "auto_refresh",
        	   callback: function(key, options){
        	       var $modal = one.topology.setTimeConfig.modal.initialize.time();
        	       $modal.modal();
        	   }
           },
           "sep3": "---------",
           "stopRefresh": {
        	   name: "stopRefresh", icon: "stoprefresh",
        	   disabled: function(key, opt) { 
                   return !this.data('stopRefreshDisabled'); 
               },
        	   callback: function(key, options) {
        		   clearInterval(auto_refresh);
        		   this.data('stopRefreshDisabled', !this.data('startRefreshDisabled'));
				   this.data('startRefreshDisabled', !this.data('startRefreshDisabled'));
    	       }
           }
       }
   });
});

