function InputListener() {
	var self = this;
	var connection;
	var keybindings = COMMANDS.keyBindings;

	// For some keys, we need to know when the user releases it:
  var needsReleased = [COMMANDS.left, COMMANDS.right, COMMANDS.jump];

  function keyDown(evt) {
    var type = keybindings[evt.keyCode];
		if (type != null && type != undefined)
      self.connection.send({'type': type});
  }

  function keyUp(evt) {
    var type = keybindings[evt.keyCode];
    if (UTIL.contains(needsReleased, type)) {
      self.connection.send({'type': "STOP-"+type});
    }
  }

  this.bindTo = function(connection) {
    self.connection = connection;
    document.body.onkeydown = keyDown;
    document.body.onkeyup = keyUp;
  }
}
