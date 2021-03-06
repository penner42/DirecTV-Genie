/**
 *  DirecTV GENIE
 *
 *  Copyright 2016 Alan Penner
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
        name: "DirecTV GENIE",
        namespace: "penner42",
        author: "Alan Penner",
        description: "DirecTV GENIE Controller",
        category: "",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        singleInstance: true
)


preferences {
    page(name: "mainPage", content: "mainPage")
    page(name: "createReceivers", content: "createReceivers")
    page(name: "manageReceiver", content: "manageReceiver")
	
    page(name: "manageChannels", content: "manageChannels")
	page(name: "addChannel", content: "addChannel") 
    page(name: "createChannel", content: "createChannel")    

    page(name: "manageButtons", content: "manageButtons")
    page(name: "createButton", content: "createButton")    
}

def addChannel(params) {
    log.debug("addchan: "+params)
	
    if (params.receiver_id) {
        state.params = params;
    } else {
        params = state.params;
    }
    
    return dynamicPage(name: "addChannel", title: "Add Channel", install: false) {
    	section() {
        	input(name: "channelName", type: "text", title: "Channel Name")
            input(name: "channelNumber", type: "number", title: "Channel Number")
            href(name: "manageChannels", page: "manageChannels", title: "Create Channel", description: "", params:[receiver_id: "${params.receiver_id}", act: "Create", button: "channel"])
        }
	}
}

def manageReceiver(params) {
    if (params.receiver_id) {
        state.params = params;
    } else {
        params = state.params;
    }
	params.act = "none"
	return dynamicPage(name: "manageReceiver", title: "Manage Receiver", install: true) {
    	section() {
			href(name: "manageButtons", page: "manageButtons", title: "Manage Buttons", description: "", params:params)
			href(name: "manageChannels", page: "manageChannels", title: "Manage Channels", description: "", params:params)
            href(name: "mainPage", page: "mainPage", title: "Back to main", description: "")
        }
    }
}

def manageChannels(params) {
    if (params.receiver_id) {
        state.params = params;
    } else {
        params = state.params;
    }
    
	def receiver = state.receivers.find{it.value.mac == params.receiver_id.split("/")[0]}
	def channels = getChildDevices().findAll{it.currentValue("command") == "channel"}
    
	if (params.act) {
		if (params.act == "Create") {
        log.debug("Create channel")
			def id = params.receiver_id + "/" + settings.channelNumber
			def d = addChildDevice("penner42", "DirecTV Genie Switch", id, receiver.value.hub, [label: settings.channelName])
	        d.sendEvent(name: "receiver", value: params.receiver_id)
	        d.sendEvent(name: "command", value: "channel")
	        d.sendEvent(name: "channel",value: settings.channelNumber)
            channels.add(d)
            params.act = "none"
		} else if (params.act == "Remove") {
 			deleteChildDevice(params.channel)
            channels.remove(channels.find{it.deviceNetworkId == params.channel})
            params.act = "none"
        }
    }    

	return dynamicPage(name: "manageChannels", title: "Manage Channels", install: true) {
    	section("") {
			href(name: "manageReceiver", page: "manageReceiver", title: "Back to Receiver", description: "", params:params)
			href(name: "addChannel", page: "addChannel", title: "Add Channel", description: "", params:params)
        }
        section("Existing Channels") {
        	channels.sort{it.displayName}.each {
				href(name: "manageChannels ${it.deviceNetworkId}", page:"manageChannels", title: "Remove ${it.displayName} [${it.currentValue("channel")}]", description: "", params:[receiver_id: "${params.receiver_id}", act: "Remove", channel: "${it.deviceNetworkId}"])
			}
		}
    }
}

def manageButtons(params) {
    if (params.receiver_id) {
        state.params = params;
    } else {
        params = state.params;
    }

	def receiver = state.receivers.find{it.value.mac == params.receiver_id.split("/")[0]}
	def buttons = ["Pause", "Previous"]
    def createButtons = [:]
    def removeButtons = [:]
    buttons.each { 
		def id = params.receiver_id + "/" + it
        def d = getChildDevice(id)
        if (d) {
        	removeButtons[it] = [act: "Remove", switch_id: id, switch_name: "${it}"]
        } else {
        	createButtons[it] = [act: "Create", switch_id: id, switch_name: "${it}"]
        }
    }

    if (params.act) {
		if (params.act == "Create") {
			def d = addChildDevice("penner42", "DirecTV Genie Switch", createButtons[params.button].switch_id, receiver.value.hub, [label: createButtons[params.button].switch_name])
	        d.sendEvent(name: "receiver", value: params.receiver_id)
	        d.sendEvent(name: "command", value: params.button)
	        d.sendEvent(name: "channel",value: "")
            removeButtons[params.button] = [act: "Remove", switch_id: createButtons[params.button].id, switch_name: createButtons[params.button].switch_name]
            createButtons.remove(params.button)
			params.act = "none"
		} else if (params.act == "Remove") {
			deleteChildDevice(removeButtons[params.button].switch_id)
            createButtons[params.button] = [act: "Create", switch_id: removeButtons[params.button].id, switch_name: removeButtons[params.button].switch_name]
            removeButtons.remove(params.button)
			params.act = "none"            
        }
    }

	return dynamicPage(name: "manageButtons", title: "Manage Buttons", install: true) {
    	section("") {
			href(name: "manageReceiver", page: "manageReceiver", title: "Back to Receiver", description: "", params:params)        
        }
    	section("New Buttons") {
			createButtons.each {
            	log.debug(it)
	        	def title = "${it.value.act} ${it.value.switch_name} Button"
	        	def act = it.value.act
				href(name: "manageButtons ${title}", page: "manageButtons", title: "${title}", description: "", params:[receiver_id: "${params.receiver_id}", act: "${act}", button: "${it.value.switch_name}"])
			}
		}
        section("Existing Buttons") {
        	removeButtons.each {
	            	def title = "${it.value.act} ${it.value.switch_name} Button"
	                def act = it.value.act
					href(name: "manageButtons ${title}", page: "manageButtons", title: "${title}", description: "", params:[receiver_id: "${params.receiver_id}", act: "${act}", button: "${it.value.switch_name}"])
            }
        }
    }
}

def createReceivers() {
	
	def selectedReceivers = settings.selectedReceivers ?: []
    
    if (!state.inCreation) {
        state.inCreation = true
        selectedReceivers.each { r ->
            def receiver = state.receivers.find{it.value.mac == r.split("/")[0]}
            def d = getChildDevice(r)
            if (!d) {
                try {
                    d = addChildDevice("penner42", "DirecTV Genie", r, receiver.value.hub, [label: receiver.value.displayName])
                    def host = convertHexToIP(receiver.value.networkAddress)
                    d.sendEvent(name: "networkAddress", value: host)
                } catch (e) {
                    log.debug e
                }
            }
        }
        state.inCreation = false
    }
    
    return dynamicPage(name: "mainPage", title: "Receiver creation complete", nextPage: "mainPage") {
    	section("Please tap Next to return to main page") {}
    }
}

def mainPage() {
    state.receivers = state.receivers ?: [:]
    int receiverRefreshCount = !state.receiverRefreshCount ? 0 : state.receiverRefreshCount as int
    state.receiverRefreshCount = receiverRefreshCount + 1
    def refreshInterval = 3

    if (!state.subscribed) {
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribed = true
    }

	def createdReceivers = [:]
    settings.selectedReceivers.each { r -> 
		def receiver = state.receivers.find{it.value.mac == r.split("/")[0]}
		def d = getChildDevice(r)
        if (d) {
        	createdReceivers[r] = receiver.value.displayName
        }
	}

	if ((state.receiverRefreshCount % 5) == 1) {
        sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", physicalgraph.device.Protocol.LAN))
    } else {
        // if we're not sending bridge discovery, verify bridges instead
        verifyReceivers()
        getNames()
    }

    def foundReceivers = state.receivers.findAll { it.value.named == true }
    def options = [:]
    foundReceivers.each {
        options[it.value.mac + "/" + it.value.clientAddr] = it.value.displayName
    }
    
	def selectedReceivers = settings.selectedReceivers ?: []
	if (createdReceivers.size() < selectedReceivers.size()) {
    	return dynamicPage(name: "createReceivers", title: "Create receivers", nextPage: "createReceivers") {
        	section("Please tap Next to create receiver devices.") { 
	        	input "selectedReceivers", "enum", required: false, title: "Select Receviers (${options.size()} found)", submitOnChange: true, multiple:true, options: options.sort{it.value}
			}
        }
    }

	dynamicPage(name: "mainPage", title: "Discovering Receivers", refreshInterval: refreshInterval, install:true, uninstall:true) {
        section() {
            input "selectedReceivers", "enum", required: false, title: "Select Receviers (${options.size()} found)", submitOnChange: true, multiple:true, options: options.sort{it.value}
        }
    	if (createdReceivers.size() > 0) {
        	section("Manage receivers") {
	        	createdReceivers.each { 
	            	href(name: "manageReceiver ${it.key}", page: "manageReceiver", title: it.value, description: "", params:[receiver_id: it.key])
	            }
			}                
        }
    }
}

def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:MediaRenderer:1") &&
            parsedEvent?.ssdpPath?.contains("/4/description.xml")) {
        def receiver = state.receivers.find({it?.key?.contains(parsedEvent.ssdpUSN)})
        if (!receiver) {
            state.receivers << ["${parsedEvent.ssdpUSN}":parsedEvent]
        } else {
            log.debug "Already discovered receiver."
        }
    }
}

def verifyReceivers() {
    def devices = state.receivers.findAll { it?.value?.verified != true }
    devices.each {
        def host = convertHexToIP(it.value.networkAddress) + ":" + convertHexToInt(it.value.deviceAddress)
        log.debug("Sending verify request for ${it.value.mac} (${host})")
        getLAN(host, "/4/description.xml", "processVerifyResponse")
    }
}

def getNames() {
    def devices = state.receivers.findAll{ it?.value?.verified == true && it?.value.named == false }
    devices.each {
        def host = convertHexToIP(it.value.networkAddress) + ":8080"
        log.debug("Sending name request for ${it.value.mac} (${host})")
        getLAN(host, "/info/getLocations", "processLocations")
    }
}

def processLocations(evt) {
    log.debug("Processing location response.")
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
    def name = body?.locations.find { it.clientAddr == "0" }.locationName

    def receiver = state.receivers.find{it.value.mac == evt.mac}
    if (receiver) {
        receiver.value.named = true
        receiver.value.displayName = name
        receiver.value.clientAddr = 0
    }
}

def processVerifyResponse(evt) {
    log.debug("Processing verify response.")
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    def body = new XmlSlurper().parseText(parsedEvent.body)
    if (body?.device?.modelDescription?.text().startsWith("DIRECTV")) {
        log.debug(body?.device?.UDN?.text())
        def receiver = state.receivers.find({it?.key?.contains(body?.device?.UDN?.text())})
        if (receiver) {
            log.debug("found receiver!")
            receiver.value << [name:body?.device?.modelDescription?.text(), verified: true, named: false]
        } else {
            log.error "/4/description.xml returned a receiver that didn't exist"
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
}

def getLAN(host, url, callback) {
    sendHubCommand(new physicalgraph.device.HubAction("GET ${url} HTTP/1.1\r\nHOST: ${host}\r\n\r\n",
            physicalgraph.device.Protocol.LAN, "${host}", [callback: callback]))
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def runCommand(receiver, command) {
	log.debug("runcommand")
	command = command.toLowerCase()
    def d = getChildDevice(receiver)
    def host = d.currentValue("networkAddress") + ":8080"
    def url = "/remote/processKey?key=${command}"
    if (d) { 
    	getLAN(host, url, "")
    }
}

def setChannel(receiver, channel) {
    def d = getChildDevice(receiver)
    def host = d.currentValue("networkAddress") + ":8080"
    def url = "/tv/tune?major=${channel}"
    if (d) { 
    	getLAN(host, url, "")
    }
}
