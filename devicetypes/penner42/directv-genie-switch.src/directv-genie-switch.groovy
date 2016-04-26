/**
 *  DirecTV Genie Switch
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
metadata {
	definition (name: "DirecTV Genie Switch", namespace: "penner42", author: "Alan Penner") {
		capability "Actuator"
        capability "Switch"
//        capability "Momentary"
//        capability "Sensor"
        
        attribute "receiver", "string"
        attribute "command", "string"
        attribute "channel", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2) {
	    multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'Push', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff"
//				attributeState "on",  label:'Push', action:"momentary.push", icon:"st.lights.philips.hue-multi"
			}
		}
    }
	main "switch"
    details(["switch"])
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def push() {
	log.debug("push")
	def channel = this.device.currentValue("channel")
	def receiver = this.device.currentValue("receiver")    
	def command = this.device.currentValue("command").toLowerCase()

/*	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
	sendEvent(name: "switch", value: "off", isStateChange: true, display: false)
	sendEvent(name: "momentary", value: "pushed", isStateChange: true)    
  */  
    switch(command) {
		case "channel": 
        	parent.setChannel(receiver, channel)
            break
		case "pause":
        	log.debug "pause"
			parent.runCommand(receiver, command)
            break
	}
    
}

def on() {
	push()
}

def off() {
	push()
}