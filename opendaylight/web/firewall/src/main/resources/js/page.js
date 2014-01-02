/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

//PAGE firewalls
one.f = {};

// specify dashlets and layouts
one.f.dashlet = {
    firewalls : {
        id : 'firewalls',
        name : 'Firewall Entries'
    },
    nodes : {
        id : 'nodes',
        name : 'Nodes'
    },
    detail : {
        id : 'detail',
        name : 'Firewall Detail'
    }
};

one.f.menu = {
    left : {
        top : [
            one.f.dashlet.firewalls
        ],
        bottom : [
            one.f.dashlet.nodes
        ]
    },
    right : {
        top : [],
        bottom : [
            one.f.dashlet.detail
        ]
    }
};

one.f.address = {
    root : "/controller/web/firewall",
    firewalls : {
        main : "/main",
        firewall : "/firewall",
        modifyFirewall : "/modifyFirewall",
        deleteFirewalls:"/firewall/deleteFirewalls",
        setStatus : "/setStatus",
        firewall_lists : "/firewall_lists",
        toggle : "/firewall/toggleFirewalls",
        ////////////
        nodes : "/node-ports",
        firewalls : "/node-firewalls"
        //////////////
    }
}

/** NODES **/
one.f.nodes = {
//    id : {
//        dashlet: {
//            datagrid: "one_f_nodes_id_dashlet_datagrid"
//        }
//    },
//    registry : {},
//    dashlet : function($dashlet) {
//        var $h4 = one.lib.dashlet.header("Nodes");
//        $dashlet.append($h4);
//
//        one.f.nodes.ajax.dashlet(function(data) {
//            var $gridHTML = one.lib.dashlet.datagrid.init(one.f.nodes.id.dashlet.datagrid, {
//                searchable: true,
//                filterable: false,
//                pagination: true,
//                flexibleRowsPerPage: true
//                }, "table-striped table-condensed");
//            $dashlet.append($gridHTML);
//            var dataSource = one.f.nodes.data.nodesDataGrid(data);
//            $("#" + one.f.nodes.id.dashlet.datagrid).datagrid({dataSource: dataSource});
//        });
//    },
//    ajax : {
//        dashlet : function(callback) {
//            $.getJSON(one.f.address.root+one.f.address.firewalls.firewalls, function(data) {
//                callback(data);
//            });
//        }
//    },
//    data : {
//        nodesDataGrid: function(data) {
//            var gridData = [];
//            $.each(data, function(nodeName, firewall) {
//                var nodeFirewallObject = {};
//                nodeFirewallObject["nodeName"] = nodeName;
//                nodeFirewallObject["firewalls"] = firewall;
//                nodeFirewallObject["rowData"] = nodeName + firewall + "-foobar";
//                gridData.push(nodeFirewallObject);
//            });
//
//            var source = new StaticDataSource({
//                    columns: [
//                        {
//                            property: 'nodeName',
//                            label: 'Node',
//                            sortable: true
//                        },
//                        {
//                            property: 'firewalls',
//                            label: 'Firewalls',
//                            sortable: true
//                        }
//                    ],
//                    data: gridData,
//                    delay: 0
//                });
//            return source;
//        }
//    },
//    body : {
//        dashlet : function(body, callback) {
//            var attributes = ['table-striped', 'table-bordered', 'table-hover', 'table-condensed'];
//            var $table = one.lib.dashlet.table.table(attributes);
//
//            var headers = ['Node', 'Firewalls'];
//            var $thead = one.lib.dashlet.table.header(headers);
//            $table.append($thead);
//
//            var $tbody = one.lib.dashlet.table.body(body);
//            $table.append($tbody);
//
//            return $table;
//        }
//    }
}

/** FIREWALL DETAIL **/
one.f.detail = {
    id : {},
    registry : {},
    dashlet : function($dashlet, details) {
        var $h4 = one.lib.dashlet.header("Firewall Details");
        $dashlet.append($h4);

        // details
        if (details == undefined) {
            var $none = $(document.createElement('div'));
            $none.addClass('none');
            var $p = $(document.createElement('p'));
            $p.text('Please select a firewall');
            $p.addClass('text-center').addClass('text-info');

            $dashlet.append($none)
                .append($p);
        }
    },
    data : {
        dashlet : function(data) {
            var body = [];
            var tr = {};
            var entry = [];

            entry.push(data['firewall']['id']);
            entry.push(data['firewall']['name']);
            entry.push(data['firewall']['action']);
            entry.push(data['firewall']['priority']);
            entry.push(data['firewall']['status']);
            entry.push(data['firewall']['description']);
            tr.entry = entry;
            body.push(tr);
            return body;
        },
        description : function(data) {
            var body = [];
            var tr = {};
            var entry = [];
            entry.push(data['node']);
            entry.push(data['firewall']['ingressPort']);
            entry.push(data['firewall']['etherType']);
            entry.push(data['firewall']['dlSrc']);
            entry.push(data['firewall']['dlDst']);
            entry.push(data['firewall']['nwSrc']);
            entry.push(data['firewall']['nwDst']);
            entry.push(data['firewall']['tpSrc']);
            entry.push(data['firewall']['tpDst']);
            entry.push(data['firewall']['protocol']);

            tr.entry = entry;
            body.push(tr);
            return body;
        }
    },
    body : {
        dashlet : function(body) {
            // create table
            var header = ['ID', 'Firewall Name', 'Action', 'Priority', 'Status','Description'];
            var $thead = one.lib.dashlet.table.header(header);
            var attributes = ['table-striped', 'table-bordered', 'table-condensed'];
            var $table = one.lib.dashlet.table.table(attributes);
            $table.append($thead);

            var $tbody = one.lib.dashlet.table.body(body);
            $table.append($tbody);

            return $table;
        },
        description : function(body) {
            var header = ['Node','Input Port', 'Ethernet Type', 'Source MAC', 'Dest MAC', 'Source IP', 'Dest IP', 'Source Port', 'Dest Port', 'Protocol'];
            var $thead = one.lib.dashlet.table.header(header);
            var attributes = ['table-striped', 'table-bordered', 'table-condensed'];
            var $table = one.lib.dashlet.table.table(attributes);
            $table.append($thead);

            var $tbody = one.lib.dashlet.table.body(body);
            $table.append($tbody);

            return $table;
        }
    }
}
/** NEW FIREWALL ENTRIES **/
one.f.firewalls = {
		id : {
        status : "one_f_firewalls_id_status",
		dashlet : {
			add : "one_f_firewalls_id_dashlet_add",
			removeMultiple : "one_f_firewalls_id_dashlet_removeMultiple",
			remove : "one_f_firewalls_id_dashlet_remove",
            datagrid : "one_f_firewalls_id_dashlet_datagrid",
            toggle : "one_f_firewalls_id_dashlet_toggle",
            selectAllFirewalls : "one_f_firewalls_id_dashlet_selectAllFirewalls",
            status : "one_f_firewalls_id_dashlet_status"
		},
		modal : {
            install : "one_f_firewalls_id_modal_install",
            edit : "one_f_firewalls_id_modal_edit",
            add : "one_f_firewalls_id_modal_add",
            close : "one_f_firewalls_id_modal_close",
            modal : "one_f_firewalls_id_modal_modal",
            dialog : {
                modal : "one_f_firewalls_id_modal_dialog_modal",
                remove : "one_f_firewalls_id_modal_dialog_remove",
                close : "one_f_firewalls_id_modal_dialog_close"
            },
	        form : {
	        	id : "one_f_firewalls_id_modal_form_id",
	        	name : "one_f_firewalls_id_modal_form_name",
	        	node : "one_f_firewalls_id_modal_form_node",
	        	port : "one_f_firewalls_id_modal_form_port",
	        	srcInport : "one_f_firewalls_id_modal_form_srcInport",
	        	dlSrc : "one_f_firewalls_id_modal_form_dlSrc",
	        	dlDst : "one_f_firewalls_id_modal_form_dlDst",
	        	etherType : "one_f_firewalls_id_modal_form_etherType",
	        	nwSrc : "one_f_firewalls_id_modal_form_nwSrc",
	        	nwDst : "one_f_firewalls_id_modal_form_nwDst",
	        	protocol : "one_f_firewalls_id_modal_form_protocol",
	        	tpSrc : "one_f_firewalls_id_modal_form_tpSrc",
	        	tpDst : "one_f_firewalls_id_modal_form_tpDst",
	        	priority : "one_f_firewalls_id_modal_form_priority",
	        	action : "one_f_firewalls_id_modal_form_action",
	        	description : "one_f_firewalls_id_modal_form_description",
	        	status : "one_f_firewalls_id_modal_form_status",
	        	actionAdd : "one_f_firewalls_id_modal_form_actionAdd"
	        }
		}
	},
	registry : {},
	dashlet : function($dashlet, callback) {
		var $h4 = one.lib.dashlet.header("Firewall Entries");
		$dashlet.append($h4);
		one.f.firewalls.ajax.dashlet(function(data) {
			if (one.f.firewalls.registry.privilege === 'WRITE') {
				 //Add firewall entry 
				 var button = one.lib.dashlet.button.single("Add Firewall Entry", one.f.firewalls.id.dashlet.add, "btn-primary", "btn-mini");
	             var $button = one.lib.dashlet.button.button(button);

	             $button.click(function() {
	                 var $modal = one.f.firewalls.modal.modifyFirewall();
	                 $modal.modal();
	             });
	             $dashlet.append($button);
	             //Remove firewall entry
	             var button = one.lib.dashlet.button.single("Remove Firewall Entry", one.f.firewalls.id.dashlet.removeMultiple, "btn-danger", "btn-mini");
	                var $button = one.lib.dashlet.button.button(button);

	                $button.click(function() {
	                    var checkedCheckBoxes = $('.firewallEntry[type=checkbox]:checked');
	                    if (checkedCheckBoxes.size() === 0) {
	                        return false;
	                    }
	                    var requestData = [];
	                    checkedCheckBoxes.each(function(index, value) {
	                        var firewallEntry = {};
	                        firewallEntry['name'] = checkedCheckBoxes[index].name;
	                        firewallEntry['firewallid'] = checkedCheckBoxes[index].getAttribute("firewallid");
	                        requestData.push(firewallEntry);
	                    });
	                    one.f.firewalls.modal.removeMultiple.dialog(requestData);
	                });
	                $dashlet.append($button);

	             //set status button
	             var button = one.lib.dashlet.button.single("Firewall Enabled", one.f.firewalls.id.dashlet.status, "btn-primary", "btn-mini");
				 var $button = one.lib.dashlet.button.button(button);
	             $button.click(function() {
	            	 var resource = {};
	            	 resource['status'] = one.f.firewalls.registry['moduleStatus']=="1"?"0":"1";
	            	 one.f.firewalls.ajax.firewallenable(resource,one.f.firewalls.setStatus);
	             });
	             $dashlet.append($button);
	             one.f.firewalls.setStatus(data.moduleStatus);
			}
			
            var $gridHTML = one.lib.dashlet.datagrid.init(one.f.firewalls.id.dashlet.datagrid, {
                searchable: true,
                filterable: false,
                pagination: true,
                flexibleRowsPerPage: true
                }, "table-striped table-condensed");
            $dashlet.append($gridHTML);
            var dataSource = one.f.firewalls.data.firewallsDataGrid(data);
            $("#" + one.f.firewalls.id.dashlet.datagrid).datagrid({dataSource: dataSource}).on("loaded", function() {
                    $("#"+one.f.firewalls.id.dashlet.datagrid.selectAllFirewalls).click(function() {
                                $("#" + one.f.firewalls.id.dashlet.datagrid).find(':checkbox').prop('checked',
                                        $("#"+one.f.firewalls.id.dashlet.datagrid.selectAllFirewalls).is(':checked'));
                    });
                    
                    $("#" + one.f.firewalls.id.dashlet.datagrid).find("tbody tr").each(function(index, tr) {
                    $tr = $(tr);
                    $span = $("td span", $tr);
                    var firewallstatus = $span.data("firewallstatus");
                    if(firewallstatus == "1") {
                        $tr.addClass("success");
                    } else {
                        $tr.addClass("warning");
                    }
                    // attach mouseover to show pointer cursor
                    $tr.mouseover(function() {
                        $(this).css("cursor", "pointer");
                    });
                    // attach click event
                    $tr.click(function() {
                        var $td = $($(this).find("td")[1]);
                        var id = $td.text();
                        var action = $td.find("span").data("action");
                        one.f.firewalls.detail(id);
                    });
                    $(".firewallEntry").click(function(e){
                                if (!$('.firewallEntry[type=checkbox]:not(:checked)').length) {
                            $("#"+one.f.firewalls.id.dashlet.datagrid.selectAllFirewalls)
                                .prop("checked",
                              true);
                        } else {
                            $("#"+one.f.firewalls.id.dashlet.datagrid.selectAllFirewalls)
                                .prop("checked",
                             false);
                        }
                        e.stopPropagation();
                    });
                });
            });
		});
        // details callback
        if(callback != undefined) callback();
	},
	detail : function(id) {
        // clear flow details
        var $detailDashlet = one.main.dashlet.right.bottom;
        $detailDashlet.empty();
        var $h4 = one.lib.dashlet.header("Firewall Overview");
        $detailDashlet.append($h4);

        // details
        var firewalls = one.f.firewalls.registry.firewalls;
        one.f.firewalls.registry['selectedId'] = id;
        var firewall;
        $(firewalls).each(function(index, value) {
          if (value.id == id ) {
        	  firewall = value;
          }
        });
        if (one.f.firewalls.registry.privilege === 'WRITE') {
            // remove button
            var button = one.lib.dashlet.button.single("Remove Firewall", one.f.firewalls.id.dashlet.remove, "btn-danger", "btn-mini");
            var $button = one.lib.dashlet.button.button(button);
            $button.click(function() {
                var requestData = [];
                var firewallEntry = {};
                firewallEntry['name'] = firewall.name;
                firewallEntry['firewallid'] = firewall.id;
                requestData.push(firewallEntry);
                one.f.firewalls.modal.removeMultiple.dialog(requestData);
            });
            // edit button
            var editButton = one.lib.dashlet.button.single("Edit Firewall", one.f.firewalls.id.dashlet.edit, "btn-primary", "btn-mini");
            var $editButton = one.lib.dashlet.button.button(editButton);
            $editButton.click(function() {
                var $modal = one.f.firewalls.modal.modifyFirewall(true);
                $modal.modal().on('shown',function(){
                    var $port = $('#'+one.f.firewalls.id.modal.form.port);
                    $('#'+one.f.firewalls.id.modal.form.node).trigger("change");
                });
            });
            // toggle button
            var toggle;
            if ( firewall['firewall']['status'] == '1') {
                toggle = one.lib.dashlet.button.single("Disable Firewall", one.f.firewalls.id.dashlet.toggle, "btn-warning", "btn-mini");
            } else {
                toggle = one.lib.dashlet.button.single("Enable Firewall", one.f.firewalls.id.dashlet.toggle, "btn-success", "btn-mini");
            }
            var $toggle = one.lib.dashlet.button.button(toggle);
            $toggle.click(function() {
            	var value = firewall['firewall']['status'] == "1"?"0":"1"
                one.f.firewalls.ajax.togglefirewall(id,value, function(data) {
                    if(data == "Success") {
                        one.main.dashlet.right.bottom.empty();
                        one.f.detail.dashlet(one.main.dashlet.right.bottom);
                        one.main.dashlet.left.top.empty();
                        one.f.firewalls.dashlet(one.main.dashlet.left.top, function() {
                           // checks are backwards due to stale registry
                           if(firewall['firewall']['status'] == '1') {
                               one.lib.alert('Disable Firewall');
                           } else {
                               one.lib.alert('Enable Firewall');
                           }
                           one.f.firewalls.detail(id);
                        });
                    } else {
                        one.lib.alert('Cannot toggle firewalls: '+data);
                    }
                });
            });

            $detailDashlet.append($button).append($editButton).append($toggle);
        }
        // append details
        var body = one.f.detail.data.dashlet(firewall);
        var $body = one.f.detail.body.dashlet(body);
        $detailDashlet.append($body);
        var body = one.f.detail.data.description(firewall);
        var $body = one.f.detail.body.description(body);
        $detailDashlet.append($body);
    },
    ajax : {
	    dashlet : function(callback) {
        	$.getJSON(one.f.address.root+one.f.address.firewalls.main,function(data){
        		one.f.firewalls.registry['firewalls'] = data.firewalls;
        		one.f.firewalls.registry['privilege'] = data.privilege;
        		one.f.firewalls.registry['moduleStatus'] = data.moduleStatus;
                one.f.firewalls.ajax.getLists(function(){});
                one.f.firewalls.ajax.nodes(function(){});
        		callback(data);
        	});            
	    },
	    firewallenable : function(status,callback){
	        	$.getJSON(one.f.address.root+one.f.address.firewalls.setStatus,status,function(data){
	        		one.f.firewalls.registry['moduleStatus'] = data;
	        		callback(data);
	        	});
	    },
	    getLists : function(callback){
	    	$.getJSON(one.f.address.root+one.f.address.firewalls.firewall_lists,function(data){
                one.f.firewalls.registry['etherTypes'] = data.etherTypes;
                one.f.firewalls.registry['protocols'] = data.protocols;
                one.f.firewalls.registry['actions'] = data.actions;
                one.f.firewalls.registry['statuses'] = data.statuses;
        		callback(data);
        	});
	    },
	    nodes : function(successCallback) {
            $.getJSON(one.f.address.root+one.f.address.firewalls.nodes, function(data) {
                var nodes = one.f.firewalls.data.nodes(data);
                var nodeports = data;
                one.f.firewalls.registry['nodes'] = nodes;
                one.f.firewalls.registry['nodeports'] = nodeports;

                successCallback(nodes, nodeports);
            });
        },
        savefirewall : function(resource, callback) {
            $.post(one.f.address.root+one.f.address.firewalls.firewall, resource, function(data) {
                callback(data);
            });
        },
        togglefirewall : function(ids, firewallStatus,callback) {
            resource = {};
            resource['firewallListIds'] = ids;
            resource['firewallStatus']=firewallStatus;
            $.post(one.f.address.root+one.f.address.firewalls.toggle, resource, function(data) {
                callback(data);
            });
        }
    },
    modal : {
	    removeMultiple : {
	        dialog: function(firewalls) {
	            var h3 = 'Remove firewall Entry';
	            var firewallList = [];
	            var firewallListIds = "";
	            for (var i = 0; i < firewalls.length; i++) {
	                firewallList.push(firewalls[i]["name"]);
	                firewallListIds = firewallListIds + firewalls[i]["firewallid"] + ";";
	            }
	            var footer = one.f.firewalls.modal.removeMultiple.footer();
	            var $body = one.f.firewalls.modal.removeMultiple.body(firewallList);
	            var $modal = one.lib.modal.spawn(one.f.firewalls.id.modal.dialog.modal, h3, $body, footer);
	
	            // bind close button
	            $('#'+one.f.firewalls.id.modal.dialog.close, $modal).click(function() {
	                $modal.modal('hide');
	            });
	
	            // bind remove rule button
	            $('#'+one.f.firewalls.id.modal.dialog.remove, $modal).click(this, function(e) {
	                var resource = {};
	                resource['firewallListIds'] = firewallListIds;
	
	                $.post(one.f.address.root+one.f.address.firewalls.deleteFirewalls, resource, function(response) {
	                    $modal.modal('hide');
	                    if(response == "Success") {
	                        one.lib.alert("Firewall Entry(s) successfully removed");
	                    } else {
	                        one.lib.alert(response);
	                    }
	                    one.main.dashlet.right.bottom.empty();
	                    one.f.detail.dashlet(one.main.dashlet.right.bottom);
	                    one.main.dashlet.left.top.empty();
	                    one.f.firewalls.dashlet(one.main.dashlet.left.top);
	                });
	            });
	            $modal.modal();
	        },
	        footer : function() {
	            var footer = [];
	            var remove = one.lib.dashlet.button.single('Remove Firewall Entry',one.f.firewalls.id.modal.dialog.remove, 'btn-danger', '');
	            var $remove = one.lib.dashlet.button.button(remove);
	            footer.push($remove);
	
	            var cancel = one.lib.dashlet.button.single('Cancel', one.f.firewalls.id.modal.dialog.close, '', '');
	            var $cancel = one.lib.dashlet.button.button(cancel);
	            footer.push($cancel);
	
	            return footer;
	        },
	        body : function (firewalls) {
	            var $p = $(document.createElement('p'));
	            var p = 'Remove the following Firewall Entry(s)?';
	            //creata a BS label for each rule and append to list
	            $(firewalls).each(function(){
	                var $span = $(document.createElement('span'));
	                $span.append(this);
	                p += '<br/>' + $span[0].outerHTML;
	            });
	            $p.append(p);
	            return $p;
	        }
    	},
    	modifyFirewall : function(edit){
            var h3;
            if(edit) {
                h3 = "Edit Firewall Entry";
            } else {
                h3 = "Add Firewall Entry";
            }
            var footer = one.f.firewalls.modal.footer();
            var $modal = one.lib.modal.spawn(one.f.firewalls.id.modal.modal, h3, "", footer);

            // bind close button
            $('#'+one.f.firewalls.id.modal.close, $modal).click(function() {
                $modal.modal('hide');
            });

            // bind edit firewall button
            $('#'+one.f.firewalls.id.modal.edit, $modal).click(function() {
                one.f.firewalls.modal.save($modal,edit);
            });

            var nodes = one.f.firewalls.registry.nodes;
            var etherTypes = one.f.firewalls.registry.etherTypes;
            var protocols = one.f.firewalls.registry.protocols;
            var actions = one.f.firewalls.registry.actions;
            var statuses = one.f.firewalls.registry.statuses;
            var nodeports = one.f.firewalls.registry.nodeports;
            var $body = one.f.firewalls.modal.modifyFirewallBody(nodes,nodeports,etherTypes,protocols, actions,statuses,edit);
            one.lib.modal.inject.body($modal, $body,edit);

            return $modal;
    	},
    	modifyFirewallBody : function(nodes,nodeports,etherTypes,protocols, actions,statuses,edit){
    		 var $form = $(document.createElement('form'));
             var $fieldset = $(document.createElement('fieldset'));
             var existingFirewall;
             //init existingFirewall
             if(edit) {
                 var firewalls = one.f.firewalls.registry.firewalls;
                 $(firewalls).each(function(index, value) {
                   if (value.id == one.f.firewalls.registry.selectedId) {
                	   existingFirewall = value.firewall;
                   }
                 });
             }
             // firewall description
             var $legend = one.lib.form.legend("");
             $legend.css('visibility', 'hidden');
             $fieldset.append($legend);
             //id
             var $label;
             if(edit){
            	 $label = one.lib.form.label("ID : "+existingFirewall.id);
            	 $label.val(existingFirewall.id);
             }else{
            	 $label = one.lib.form.label("ID : 0");
            	 $label.val('0');
             }
             $label.attr('id',one.f.firewalls.id.modal.form.id);
             
             $fieldset.append($label);

             // name
             var $label = one.lib.form.label("Name");
             var $input = one.lib.form.input("Firewall Name");
             $input.attr('id', one.f.firewalls.id.modal.form.name);
             if(edit) {
                 $input.val(existingFirewall.name);
             }
             $fieldset.append($label).append($input);

             // node
             var $label = one.lib.form.label("Node");
             var $select = one.lib.form.select.create(nodes);
             one.lib.form.select.prepend($select, { '' : 'Please Select a Node' });
             $select.val($select.find("option:first").val());
             $select.attr('id', one.f.firewalls.id.modal.form.node);
             if(edit) {
            	 if(existingFirewall.node != null){
            		 $select.val(existingFirewall.node.type + "|"+ existingFirewall.node.nodeIDString);
            	 }
             }
             $fieldset.append($label).append($select);

             // bind onchange
             $select.change(function() {
                 // retrieve port value
                 var node = $(this).find('option:selected').attr('value');
                 var $port = $('#'+one.f.firewalls.id.modal.form.port);
                 if (node == '') {
                     one.lib.form.select.inject($port, {});
                     one.lib.form.select.prepend($port, { '' : 'Please Select a Port' });
                     return;
                 }
                 one.f.firewalls.registry['currentNode'] = node;
                 var ports = nodeports[node]['ports'];
                 one.lib.form.select.inject($port, ports);
                 one.lib.form.select.prepend($port, { '' : 'Please Select a Port' });
                 $port.val($port.find("option:first").val());
                 if(edit){
                	 if( node == existingFirewall.node.type + "|"+ existingFirewall.node.nodeIDString) {
                		 $port.val( existingFirewall.ingressPort );
                	 }
                 }
             });
             $fieldset.append($label).append($select);

             // input port
             var $label = one.lib.form.label("Input Port");
             var $select = one.lib.form.select.create();
             one.lib.form.select.prepend($select, { '' : 'Please Select a Port' });
             $select.attr('id', one.f.firewalls.id.modal.form.port);
             $fieldset.append($label).append($select);
             //dlSrc
             var $label = one.lib.form.label("Source MAC Address");
             var $input = one.lib.form.input("3c:97:0e:75:c3:f7");
             $input.attr('id', one.f.firewalls.id.modal.form.dlSrc);
             $fieldset.append($label).append($input);
             if(edit) {
                 $input.val(existingFirewall.dlSrc);
             }
             //dlDst
             var $label = one.lib.form.label("Destination MAC Address");
             var $input = one.lib.form.input("7c:d1:c3:e8:e6:99");
             $input.attr('id', one.f.firewalls.id.modal.form.dlDst);
             $fieldset.append($label).append($input);
             if(edit) {
                 $input.val(existingFirewall.dlDst);
             }
             
             //etherType
             var $label = one.lib.form.label("Ether Type");
             var $select = one.lib.form.select.create(etherTypes);
             $select.attr('id', one.f.firewalls.id.modal.form.etherType);
             one.lib.form.select.prepend($select, { '' : 'Please Select a Ether Type' });
             $select.val($select.find("option:first").val());
             if(edit) {
            	 $select.val(existingFirewall.etherType);
             }
             $fieldset.append($label).append($select);
             
             // nwSrc
             var $label = one.lib.form.label("Source IP Address");
             var $input = one.lib.form.input("192.168.3.128");
             $input.attr('id', one.f.firewalls.id.modal.form.nwSrc);
             $fieldset.append($label).append($input);
             if(edit) {
                 $input.val(existingFirewall.nwSrc);
             }
             // nwDst
             var $label = one.lib.form.label("Destination IP Address");
             var $input = one.lib.form.input("192.168.3.128");
             $input.attr('id', one.f.firewalls.id.modal.form.nwDst);
             $fieldset.append($label).append($input);
             if(edit) {
                 $input.val(existingFirewall.nwDst);
             }
             //protocol
             var $label = one.lib.form.label("Network Protocol");
             var $select = one.lib.form.select.create(protocols);
             $select.attr('id', one.f.firewalls.id.modal.form.protocol);
             one.lib.form.select.prepend($select, { '' : 'Please Select a NW-Proto' });
             $select.val($select.find("option:first").val());
             if(edit) {
            	 $select.val(existingFirewall.protocol);
             }
             $fieldset.append($label).append($select);
             //tpSrc
             var $label = one.lib.form.label("Sorce Port");
             var $input = one.lib.form.input("0");
             $input.attr('id', one.f.firewalls.id.modal.form.tpSrc);
             var $help = one.lib.form.help("Range: 0 - 65535");
             $fieldset.append($label).append($input).append($help);;
             if(edit) {
                 $input.val(existingFirewall.tpSrc);
             }
             //tpDst
             var $label = one.lib.form.label("Destination Port");
             var $input = one.lib.form.input("0");
             $input.attr('id', one.f.firewalls.id.modal.form.tpDst);
             var $help = one.lib.form.help("Range: 0 - 65535");
             $fieldset.append($label).append($input).append($help);;
             if(edit) {
                 $input.val(existingFirewall.tpDst);
             }
             //priority
             var $label = one.lib.form.label("Priority");
             var $input = one.lib.form.input("0");
             $input.attr('id', one.f.firewalls.id.modal.form.priority);
             $fieldset.append($label).append($input);
             if(edit) {
                 $input.val(existingFirewall.priority);
             }
             //action
             var $label = one.lib.form.label("Action");
             var $select = one.lib.form.select.create(actions);
             $select.attr('id', one.f.firewalls.id.modal.form.action);
             one.lib.form.select.prepend($select, { '' : 'Please Select Action' });
             $select.val($select.find("option:first").val());

             // bind onchange
             $select.change(function() {
                 // retrieve port value
                 var action = $(this).find('option:selected').attr('value');
                 var $add = $('#'+one.f.firewalls.id.modal.form.actionAdd);
                 //If choice has add
                 if(action.indexOf(":") >= 0){
                	 $add.removeAttr("disabled");
                	 if(edit){
	                	 var lists = existingFirewall.action.split(':');
	                	 if( action === lists[0]+":" ){
	                		 $add.val(lists[1]);
	                	 }else{
	                		 $add.val("");
	                	 }
                	 }
                	 else{
                		 $add.val("");
                	 }
                 }else{
                	 $add.attr('disabled', 'disabled');
                	 $add.val("");
                 }

             });
             //action add
             var $label2 = one.lib.form.label("Action parameters");
             var $input = one.lib.form.input("...");
             $input.attr('disabled', 'disabled');
             $input.attr('id', one.f.firewalls.id.modal.form.actionAdd);
             if(edit) {
            	 if(existingFirewall.action.indexOf(":") >= 0){
            		 $input.removeAttr("disabled");
            		 var lists = existingFirewall.action.split(':');
            		 $select.val(lists[0]+":");
            		 $input.val(lists[1]);
            	 }else{
            		 $select.val(existingFirewall.action);
            	 }
             }
             $fieldset.append($label).append($select);
             $fieldset.append($label2).append($input);
             
             //description
             var $label = one.lib.form.label("Description");
             var $input = one.lib.form.input("...");
             $input.attr('id', one.f.firewalls.id.modal.form.description);
             $fieldset.append($label).append($input);
             if(edit) {
                 $input.val(existingFirewall.description);
             }
             //status
             var $label = one.lib.form.label("Status");
             var $select = one.lib.form.select.create(statuses);
             $select.attr('id', one.f.firewalls.id.modal.form.status);
             if(edit) {
            	 $select.val(existingFirewall.status);
             }else{
            	 //default
            	 $select.val("1");
             }
            
             $fieldset.append($label).append($select);

             // return
             $form.append($fieldset);
             return $form;
    	},
	    save : function($modal,edit){
	        var result = {};
	        result['id'] = $('#'+one.f.firewalls.id.modal.form.id, $modal).val();
	        result['name'] = $('#'+one.f.firewalls.id.modal.form.name, $modal).val();
	        result['ingressPort'] = $('#'+one.f.firewalls.id.modal.form.port, $modal).val();
	        result['dlSrc'] = $('#'+one.f.firewalls.id.modal.form.dlSrc, $modal).val();
	        result['dlDst'] = $('#'+one.f.firewalls.id.modal.form.dlDst, $modal).val();
	        result['etherType'] = $('#'+one.f.firewalls.id.modal.form.etherType, $modal).val();
	        result['nwSrc'] = $('#'+one.f.firewalls.id.modal.form.nwSrc, $modal).val();
	        result['nwDst'] = $('#'+one.f.firewalls.id.modal.form.nwDst, $modal).val();
	        result['protocol'] = $('#'+one.f.firewalls.id.modal.form.protocol, $modal).val();
	        result['tpSrc'] = $('#'+one.f.firewalls.id.modal.form.tpSrc, $modal).val();
	        result['tpDst'] = $('#'+one.f.firewalls.id.modal.form.tpDst, $modal).val();
	        result['priority'] = $('#'+one.f.firewalls.id.modal.form.priority, $modal).val();
	        //action
	        var actionTmp = $('#'+one.f.firewalls.id.modal.form.action, $modal).val();
	        if(actionTmp.indexOf(':') >= 0){
	        	actionTmp = actionTmp + $('#'+one.f.firewalls.id.modal.form.actionAdd, $modal).val();
	        }
	        result['action'] = actionTmp;
	        
	        //result['action'] = $('#'+one.f.firewalls.id.modal.form.action, $modal).val();
	        result['description'] = $('#'+one.f.firewalls.id.modal.form.description, $modal).val();
	        result['status'] = $('#'+one.f.firewalls.id.modal.form.status, $modal).val();
	        var nodeId = $('#'+one.f.firewalls.id.modal.form.node, $modal).val();
	
	        //delete the unselected key
	        $.each(result, function(key, value) {
	            if (value == "") delete result[key];
	        });
	
	        // frontend validation
	        if (result['name'] == undefined) {
	            alert('Need firewall name');
	            return;
	        }
	        if(result['priority'] == undefined || isNaN(result['priority'])){
	        	alert('Need priority and priority must be number');
	        	return;
	        }
	
	        // package for ajax call
	        var resource = {};
	        resource['body'] = JSON.stringify(result);
	        resource['nodeId'] = nodeId;
	        
	        if(edit) {
	        	one.f.firewalls.ajax.savefirewall(resource, function(data){
	        		if (data == "Success") {
	                    $modal.modal('hide').on('hidden', function () {
	                        one.f.firewalls.detail(result['id']);
	                    });
	                    one.lib.alert('Firewall Entry edited');
	                    one.main.dashlet.left.top.empty();
	                    one.f.firewalls.dashlet(one.main.dashlet.left.top);
	        		}else{
	        			alert('Could not add firewall: '+data);
	        		}
	        	});
	        } else {
	        	one.f.firewalls.ajax.savefirewall(resource, function(data){
	        		if (data == "Success") {
	        			$modal.modal('hide');
	        			one.lib.alert('Firewall Entry added');
	        			one.main.dashlet.left.top.empty();
	        			one.f.firewalls.dashlet(one.main.dashlet.left.top);
	        		} else {
	        			alert('Could not add firewall: '+data);
	        		}
	        	});
	        }
	    },
	    
        footer : function() {
            var footer = [];

            var editButton = one.lib.dashlet.button.single("Save Firewall", one.f.firewalls.id.modal.edit, "btn-success", "");
            var $editButton = one.lib.dashlet.button.button(editButton);
            footer.push($editButton);

            var closeButton = one.lib.dashlet.button.single("Close", one.f.firewalls.id.modal.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);

            return footer;
        },
    },
    data : {
        firewallsDataGrid: function(data) {
            var source = new StaticDataSource({
                    columns: [
                        {
                            property: 'selector',
                            label: "<input type='checkbox' id='"+one.f.firewalls.id.dashlet.datagrid.selectAllFirewalls+"'/>",
                            sortable: false
                        },
                        {
                            property: 'id',
                            label: "ID",
                            sortable: true
                        },
                        {
                            property: 'name',
                            label: 'Firewall Name',
                            sortable: true
                        },
                        {
                            property: 'action',
                            label: 'Action',
                            sortable: true
                        }
                    ],
                    data: data.firewalls,
                    formatter: function(items) {
                        $.each(items, function(index, item) {
                            var $checkbox = document.createElement("input");
                            $checkbox.setAttribute("type", "checkbox");
                            $checkbox.setAttribute("firewallid", item.id);
                            $checkbox.setAttribute("name", item.name);
                            $checkbox.setAttribute('class','firewallEntry')
                            item.selector = $checkbox.outerHTML;
                          item["name"] = '<span data-firewallstatus=' + item["firewall"]["status"] + 
                        ' data-action=' + item["action"] + '>' + item["name"] + '</span>';
                        });

                    },
                    delay: 0
                });
            return source;
        },
        dashlet : function(data) {
            var body = [];
            $(data).each(function(index, value) {
                var tr = {};
                var entry = [];

                
                entry.push(value['name']);
                entry.push(value['node']);
                if ( value['firewall']['status'] == 'Success')
                    tr['type'] = ['success'];
                else 
                    tr['type'] = ['warning'];
                tr['entry'] = entry;
                tr['id'] = value['nodeId'];

                body.push(tr);
            });
            return body;
        },
        nodes : function(data) {
            result = {};
            $.each(data, function(key, value) {
                result[key] = value['name'];
            });
            return result;
        }
    },
    //public function
	setStatus : function(status){
		var button = document.getElementById(one.f.firewalls.id.dashlet.status);
	   	 if(status == "0"){
			 button.classList.remove("btn-primary");
			 button.classList.add("btn-danger");
		     button.textContent = "Firewall Disabled";
		 }else{
			 button.classList.remove("btn-danger");
			 button.classList.add("btn-primary");
			 button.textContent = "Firewall Enabled";
		 }
	}
}


/** INIT **/
// populate nav tabs
$(one.f.menu.left.top).each(function(index, value) {
    var $nav = $(".nav", "#left-top");
    one.main.page.dashlet($nav, value);
});

$(one.f.menu.left.bottom).each(function(index, value) {
    var $nav = $(".nav", "#left-bottom");
    one.main.page.dashlet($nav, value);
});

$(one.f.menu.right.bottom).each(function(index, value) {
    var $nav = $(".nav", "#right-bottom");
    one.main.page.dashlet($nav, value);
});

one.f.populate = function($dashlet, header) {
    var $h4 = one.lib.dashlet.header(header);
    $dashlet.append($h4);
};

// bind dashlet nav
$('.dash .nav a', '#main').click(function() {
    // de/activation
    var $li = $(this).parent();
    var $ul = $li.parent();
    one.lib.nav.unfocus($ul);
    $li.addClass('active');
    // clear respective dashlet
    var $dashlet = $ul.parent().find('.dashlet');
    one.lib.dashlet.empty($dashlet);
    // callback based on menu
    var id = $(this).attr('id');
    var menu = one.f.dashlet;
    switch (id) {
        case menu.firewalls.id:
            one.f.firewalls.dashlet($dashlet);
            break;
        case menu.nodes.id:
            one.f.nodes.dashlet($dashlet);
            break;
        case menu.detail.id:
            one.f.detail.dashlet($dashlet);
            break;
//        case menu.firewalls.id:
//        	one.f.firewalls.dashlet($dashlet);
//        	break;
    };
});

// activate first tab on each dashlet
$('.dash .nav').each(function(index, value) {
    $($(value).find('li')[0]).find('a').click();
});
