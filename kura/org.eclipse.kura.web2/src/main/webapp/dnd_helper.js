var DropSupport = function (element) {
	this.element = element
	this.dragOverHandler = function () {}
	this.dragExitHandler = function () {}
	this.dropHandler = function() { return false }
	this.attachEventHandlers(element)
}

DropSupport.isDropSupported = function (element) {
	return element['ondragenter'] !== undefined && element['ondrop'] !== undefined
}

DropSupport.addIfSupported = function (element) {
    if (!DropSupport.isDropSupported(element)) {
        return null
    }
    return new DropSupport(element)
}

DropSupport.prototype.attachEventHandlers = function (element) {
	var self = this
	var count = 0
	element.addEventListener('dragover', function (event) {
		if (self.dragOverHandler(event)) {
			event.preventDefault()
			return true
		}
	})
	element.addEventListener('dragleave', function (event) {
		count--
		if (count == 0) {
			self.dragExitHandler(event)
		}
	})
	element.addEventListener('drop', function (event) {
		count = 0
		if (self.dropHandler(event)) {
			event.preventDefault()
		}
	})
	element.addEventListener('dragenter', function (event) {
		count++
	})
}