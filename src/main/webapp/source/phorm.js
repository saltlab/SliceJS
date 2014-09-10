var AjaxVal = "";
var isAjaxing = false;
var md5 = "";
var ss_cur = 0;
var ss_play = 1;
var ss_pid  = new Array();
var ss_ttl  = new Array();
var ss_src  = new Array();
var ss_date = new Array();
var ss_desc = new Array();
var ss_loaded = false;
var ss_smaller = false;
var ss_awaits = 1;

function dg(x) {
	return document.getElementById(x);
}

function setOpac(cur, strength) {
	if (cur.style.MozOpacity)
		cur.style.MozOpacity=strength;
	else
		if (cur.filters)
			cur.filters.alpha.opacity=strength*100;
}

function LightenIt(cur) {
	setOpac(cur, 0.99);
}

function DarkenIt(cur, t) {
	if ((!t) || (t == ''))	t = DarkenVal / 100;
	setOpac(cur, t);
}

function toggleInfo(wut) {
	if ((!wut) || (wut == ''))
		wut = document.getElementById('hin').innerHTML;
	if (wut == 'Show') {
		document.getElementById('hin').innerHTML = 'Hide&nbsp;';
		document.getElementById('photoBoxes').style.display = 'block';
		document.getElementById('theImage').style.cssFloat = 'left';
		document.getElementById('theImage').style.styleFloat = 'left';
		document.getElementById('theImage').style.marginRight = '15px';
		setCookie('hideinfo', 'false');
	}
	else {
		document.getElementById('hin').innerHTML = 'Show';
		document.getElementById('photoBoxes').style.display = 'none';
		document.getElementById('theImage').style.cssFloat = 'none';
		document.getElementById('theImage').style.styleFloat = 'none';
		document.getElementById('theImage').style.marginRight = '55px';
		setCookie('hideinfo', 'true');
	}
}

function cookieVal(cookname) {
	thiscook = document.cookie.split("; ");
	for (i=0; i&lt;thiscook.length; i++)
		if (cookname == thiscook[i].split("=")[0])
			return thiscook[i].split("=")[1];
	return -1;
}

function setCookie(key, val) {
	newd = new Date;
	newd.setMonth(newd.getMonth()+6);
	document.cookie = key+"="+val+";expires=" + newd.toGMTString();
}

function reToggleInfo() {
	toggleInfo((cookieVal('hideinfo') != 'true')?'Show':'Hide');
}

function rand(x) {
	return Math.round(Math.random()*x);
}

function reshuffle() {
	var maxRand = 400-75;
	var n = document.getElementById('thumbscount').value;
	for (var i=0; i&lt;n; i++) {
		document.getElementById('ThumbInBox'+i).style.top = rand(maxRand)+'px';
		document.getElementById('ThumbInBox'+i).style.left = rand(maxRand)+'px';
	}
}

function updateIndic() {
	var v = document.getElementById('indicator').innerHTML;
	var l = v.length;
	var neck = 52;
	if (l > neck)
		v = v.substring(0, l-3*7)
	if ((l%3) == 0)
		document.getElementById('indicator').innerHTML = '&#149;      '+v;
	else
		document.getElementById('indicator').innerHTML = '&nbsp; '+v;
	if (isAjaxing)
		setTimeout("updateIndic();", 500);
	else
		document.getElementById('indicator').innerHTML = '';
}

function alertContents(http_request) {
	if (http_request.readyState == 4)
		try {
			if (http_request.status == 200) {
				AjaxVal = http_request.responseText;
				AjaxVal = AjaxVal.substr(6, AjaxVal.length-13); // to clear &lt;ajax>&lt;/ajax> tag which was added to save XMLity
				AjaxValRead = false; // to avoid duplicate the reading of the content
				if (AjaxVal.substr(0, 4) == 'Done') {
					var confirmMsg = 'Your rating saved!';
					document.getElementById('rateStatus').innerHTML = confirmMsg;
					document.getElementById('sumRate').innerHTML = AjaxVal.substr(4, AjaxVal.length-1);
				}
				if (AjaxVal.substr(0, 6) == 'FakeWV') {
					document.getElementById('wvwimg').src = "wv.php?rand="+rand(10000000);
					md5 = AjaxVal.substr(6, 27);
				}
				if (AjaxVal.substr(0, 6) == 'TrueWV') {
					md5 = md5;
				}
				AjaxVal = "";
				isAjaxing = false;
			}
		} catch (e) {}
}

function makeRequest(url) {
	var http_request = false;
	if (window.XMLHttpRequest) { // Mozilla, Safari,...
		http_request = new XMLHttpRequest();
		if (http_request.overrideMimeType)
			http_request.overrideMimeType('text/xml');
	} else if (window.ActiveXObject) { // IE
		try { http_request = new ActiveXObject("Msxml2.XMLHTTP"); } catch (e) {
			try { http_request = new ActiveXObject("Microsoft.XMLHTTP"); } catch (e) {}
		}
	}
	if (!http_request) {
		alert('Giving up :( Cannot create an XMLHTTP instance');
		return false;
	}
	http_request.onreadystatechange = function() { 
	  try { 
        alertContents(http_request); 
      } catch(e) {
      } 
    };
	http_request.open('GET', url, true);
	http_request.send(null);
}

function SaveRating(pid, rate) {
	if (rate == 0) {
		alert('Select your rate among the other options!');
		return;
	}
	isAjaxing = true;
	var inProgress = 'Saving your rate ';
	document.getElementById('rateStatus').innerHTML = inProgress;
	updateIndic();
	var requestUrl = "./?cmd=rate&p="+pid+"&rate="+rate+"&r="+Math.round(Math.random()*100000);
	makeRequest(requestUrl); // to avoid unwanted caching
}

function prepareBody() {
	try {
		reToggleInfo();
	} catch(e) {}
	try {
		isAjaxing = true;
		makeRequest("./?cmd=wvcheck&md5="+md5.substr(0, 20)+"&r="+Math.round(Math.random()*100000)); // to avoid unwanted caching
	} catch(e) {}
}

function confirmDelete(x) {
	return confirm('Are you sure you want to delete "'+x+'"?');
}

function hideElem(x) {
	try {
		document.getElementById(x).style.display = 'none';
	} catch(e) {}
}

function showElem(x) {
	try {
		document.getElementById(x).style.display = 'block';
	} catch(e) {}
}

function inlineElem(x) {
	try {
		document.getElementById(x).style.display = 'inline';
	} catch(e) {}
}

function tableRowElem(x) {
	try {
		document.getElementById(x).style;
		document.getElementById(x).style.display = 'table-row';
	} catch (e) {
		if (document.getElementById(x))
			document.getElementById(x).style.display = 'inline';
	}
}

function checkWV() {
	re = /^\d{5}$/;
	if (! re.test(document.getElementById('wvinput').value)) {
		alert('Word Verification box should have an string of length 5 with digits');
		return false;
	}
	if (document.getElementById('cmntTextArea').value.length == 0) {
		alert('Empty comment is not allowed!');
		return false;
	}
	return true;
}

function doReply(x) {
	x = parseInt(x);
	document.getElementById('cmntReply').value = x;
	document.getElementById('viewComment').setAttribute('href', '#'+x);
	if (x == 0)
		hideElem('ComReplyTR');
	else
		tableRowElem('ComReplyTR');
}

function toggle(w, c, t) {
	var block = (document.getElementById(w).style.display == 'block');
	document.getElementById(w).style.display = (block)?'none':'block';
	document.getElementById(c).style.display = (!block)?'none':'block';
	t.innerHTML = (!block)?"Hide'em again":"Show'em All";
}

function ss_next() {
	ss_cur++;
	ss_update();
}

function ss_prev() {
	ss_cur--;
	ss_update();
}

function ss_update() {
	ss_cur = Math.max(ss_cur, 0);

	if (ss_cur >= ss_date.length) {
		hideElem('ss_link2');
		showElem('ss_theend');
		ss_cur = ss_date.length;
		var message = "Final";
		document.getElementById('ss_n').innerHTML = message;
		if (ss_play)
			ss_playpause();
	}
	else {
		hideElem('ss_theend');
		inlineElem('ss_link2');

		ss_loaded = (document.getElementById('ss_photo').src == ss_src[ss_cur]);

		link = ".?p="+ss_pid[ss_cur];
		src = ss_src [ss_cur];
		src = ss_smaller?src_smaller(src):src;

		document.getElementById('ss_photo')	.src 		= src;
		document.getElementById('ss_date')	.innerHTML 	= ss_date[ss_cur];
		document.getElementById('ss_title')	.innerHTML 	= ss_ttl [ss_cur];
		document.getElementById('ss_desc')	.innerHTML 	= ss_desc[ss_cur];
		document.getElementById('ss_n')		.innerHTML  = 1+ss_cur;
		document.getElementById('ss_link1').setAttribute('href', link);
		document.getElementById('ss_link2').setAttribute('href', link);
		if (ss_cur &lt; ss_date.length) {
			preimg = new Image;
			preimg.src 					= ss_src [Math.min(ss_cur+1, ss_date.length - 1)];
		}
	}
}

function ss_loaddone() {
	ss_loaded = true;
}

function ss_playpause() {
	ss_play = !ss_play;
	document.getElementById('ss_playpause_link').innerHTML = (ss_play)?'Pause it':'Play it';
	document.getElementById('ss_playpause_link2').innerHTML = dg('ss_playpause_link').innerHTML;
	ss_run();
}

function src_smaller(x) {
	if (x.charAt(x.length-1) == ")")
		x = x.substr(3, x.length-4);
	return x.substr(0, x.length-5)+"4.jpg";
}

function ss_toggleSmaller() {
	ss_smaller = !ss_smaller;
	document.getElementById('ss_smaller_link').innerHTML = (ss_smaller)?'Larger Size':'Smaller Size';
	document.getElementById('ss_photo').src = ss_smaller?src_smaller(dg('ss_photo').src):ss_src[ss_cur];
}

function ss_run() {
	if ((ss_play) && (ss_awaits &lt;= 0)) {
		ss_awaits++;
		var lookup = parseInt(document.getElementById('ss_refresh').value);
		setTimeout("ss_slideshow();", lookup);
	}
}

function ss_slideshow() {
	ss_awaits--;
	if (ss_play == 1 && ss_loaded) {
		ss_cur--;
		ss_update();
	}
	ss_run();
}


