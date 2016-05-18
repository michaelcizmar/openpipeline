/**
 * Send an ajax request to a server, and specify the callback method
 * that should get called asynchronously when the XML returns.
 */
function ajaxFetch(urlStr, params, callback, method) {
    var xmlHttpReq = false;
    var self = this;
    // Mozilla/Safari
    if (window.XMLHttpRequest) {
        self.xmlHttpReq = new XMLHttpRequest();
    }
    // IE
    else if (window.ActiveXObject) {
        self.xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
    }
    self.xmlHttpReq.open(method, urlStr, true);
    self.xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    self.xmlHttpReq.onreadystatechange = callback;
    self.xmlHttpReq.send(params);
}

/**
 * Alternate between the images src1 and src2 for a give image id.
 */
function toggleImage(id, src1, src2) {
   if(document.images && document.images[id]){
        image1 = new Image;
        image1.src = src1;
        if(document.images[id].src == image1.src){
            document.images[id].src = src2;
        } else {
	        document.images[id].src = image1.src
        }
   }
}

/**
 * Hide/show the element with the given id.
 */
function toggleElement(id) {
	var element = document.getElementById(id);
	if (element.style.display == '') {
		element.style.display = 'none';
	} else {
		element.style.display = '';
	}
}

/**
 * Hide/show the elements with the given name.
 */
function toggleElementsByName(name) {
	var elements = document.getElementsByName(name);
	for (var i = 0; i < elements.length; i++) {
		var element = elements.item(i);
		if (element.style.display == '') {
			element.style.display = 'none';
		} else {
			element.style.display = '';
		}
	}
}

/**
 * Add the selected items from the source to the destination list
 */
function addSrcToDest(srcId, destId) {
	addSrcToDest(srcId, destId, true);
}


/**
 * Add the selected items from the source to the destination list
 * srcId -- id of the source SELECT control
 * destId -- id of the destination SELECT control
 * checkDest -- if true, check to see if the dest already contains the element, and if so,
 *              don't add it.
 */
function addSrcToDest(srcId, destId, checkDest) {
	var srcList = document.getElementById(srcId);
	var destList = document.getElementById(destId);

	var destLen = destList.length;
	for (var i = 0; i < srcList.length; i++) {
	
		if ((srcList.options[i] != null) && (srcList.options[i].selected)) {

			var srcText = srcList.options[i].text;
			var okToAdd = true;

			if (checkDest) {
				for (var j = 0; j < destList.length; j++) {
					if (destList.options[j] != null) {
						if (srcText == destList.options[j].text) {
							okToAdd = false;
							break;
      					}
   					}
				}
			}

			if (okToAdd) {
				// add the option
				destList.options[destList.length] = new Option(srcList.options[i].text, srcList.options[i].value); 
       		}
      	}
   	}
}

/**
 * Removes an option from a select list
 */
function removeFromList(listId) {
	var list = document.getElementById(listId);
	
	// we have to count down, instead of up, to make it work correctly
	for (var i = list.length - 1; i >= 0; i--) {
		if ((list.options[i] != null) && (list.options[i].selected == true)) {
			list.options[i] = null;
      	}
   	}
}

/**
 * Move an option in a select list up one row if 'up' is true, else move it down.
 */
function moveOption(listId, up) {
	var list = document.getElementById(listId);

	// find the row to move
	var rowNum;
	for (var i = 0; i < list.length; i++) {
		if ((list.options[i] != null) && (list.options[i].selected == true)) {
			rowNum = i;
      	}
   	}
   	
   	if (up) {
   		if (rowNum > 0) { 
   			swap(list, rowNum, rowNum - 1);
   		}
   		
   	} else { // down
   		if (rowNum < list.length - 1) { 
   			swap(list, rowNum, rowNum + 1);
   		}
   	}
}


/**
 * Swap two options in a select box.
 */
function swap(list, row0, row1) {
	var tmp = list.options[row0].text;
	list.options[row0].text = list.options[row1].text;
	list.options[row1].text = tmp;

	tmp = list.options[row0].value;
	list.options[row0].value = list.options[row1].value;
	list.options[row1].value = tmp;

	// move the highlight as well
	list.options[row0].selected = false;
	list.options[row1].selected = true;
}


/**
 * Select all the options in a select list.
 */
function selectAll(listId) {
	var list = document.getElementById(listId);
	for (var i = 0; i < list.options.length; i++) {
		list.options[i].selected = true;
	}
}

/**
 * Gathers all options in a listbox and concatenates them into
 * a single string, separated by separator, and sets the value
 * of the hidden field equal to the string. Useful because
 * a browser doesn't submit options in a list box to the server
 * unless they're all selected. This is an easy way to send them 
 * all in one parameter.
 */
function gatherOptions(listId, hiddenFieldId, separator) {
	var out = "";
	var list = document.getElementById(listId);
	for (var i = 0; i < list.options.length; i++) {
		if (out.length > 0) {
			out = out + separator;
		}
		out = out + list.options[i].value;
	}
	var hiddenField = document.getElementById(hiddenFieldId);
	hiddenField.value = out;
}

 /**
  * Set all of the checkboxes on a form to make them match the state
  * of a global checkbox.
  * @param globalCheckBox
  * @param editform the form that contains the checkboxes
  * @return
  */
function toggleCheckboxes(globalCheckBox, editform){
	for (i = 0; i < document.editform.elements.length; i++) {
		if (editform.elements[i].type=="checkbox" && 
				editform.elements[i] != globalCheckBox.name) {
			document.editform.elements[i].checked = globalCheckBox.checked;
	   	}
	}
} 

