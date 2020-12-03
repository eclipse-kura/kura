/*******************************************************************************
 *  Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *   Eurotech
 ******************************************************************************/

var ExtensionRegistry = function () {
	this.extensions = {}
	this.consumers = []
}

ExtensionRegistry.prototype.registerExtension = function (extension) {
	this.extensions[extension.id] = extension
    for (var i = 0; i < this.consumers.length; i++) {
        (this.consumers[i])(extension)
    }
}

ExtensionRegistry.prototype.unregisterExtension = function (extension) {
    delete this.extensions[extension.id]
}

ExtensionRegistry.prototype.addExtensionConsumer = function (consumer) {
	this.consumers.push(consumer)
    for (var p in this.extensions) {
        consumer(this.extensions[p])
    }
}

ExtensionRegistry.prototype.getExtensions = function () {
    var result = []
    for (var p in this.extensions) {
        result.push(this.extensions[p])
    }
    return result
}

if (!window.top.extensionRegistry) {
	window.top.extensionRegistry = new ExtensionRegistry();
}