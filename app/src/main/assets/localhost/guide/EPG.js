var canvas;
var width;
var height;
var ctx;
var img;
var mouse;
var selected_program;


var CHANNEL_INDEX = 0, NUM_CHANNELS = 0, CHANNEL_INC = 5
var TIME_INDEX = 0, TIME_LENGTH = 0, TIME_INC = 30*60*1000
var PARSE_DATA;


function init() {

canvas = document.getElementById('canvas');
width = canvas.width;
height = canvas.height;
ctx = canvas.getContext('2d');
ctx.font = "30px arial";
img = new Image();
mouse = new Object(); mouse.x = 0; mouse.y = 0; mouse.flag = 0; selected_program = null;
img.src = 'img/background.png';

PARSE_DATA=new Object();
PARSE_DATA.time_start=11*60*1000;
PARSE_DATA.end_start=16*60*1000;

PARSE_DATA.startdate;
PARSE_DATA.enddate;

}

function getNodeByName(parent,name) {
	if (parent == null) return null
	for (var j =0; j< parent.childNodes.length; j++) { var node = parent.childNodes[j];
		if (node.nodeName == name) return node 
	}
	return null
}

function getNodeAttribute(node,name) {
	if (node == null) return null
	for (var j = 0; j<node.attributes.length; j++) {
		var attr = node.attributes[j]
		if (attr.name == name) return attr.value;
	}
	return null
}

function parseDate(s) {
	return new Date(Date.UTC(parseInt(s.substr(0,4),10),parseInt(s.substr(5,2),10)-1,parseInt(s.substr(8,2),10),parseInt(s.substr(11,2),10),parseInt(s.substr(14,2),10),parseInt(s.substr(17,2),10),0))
}

function startUI(){

	NUM_CHANNELS=PARSE_DATA.channels.length;
	TIME_LENGTH = (PARSE_DATA['time_end']-PARSE_DATA['time_start'])/(TIME_INC);
	setTimeout(UI(ctx),10);


}

function parseXML(xmlDoc) {
	console.log('parsing data...')
	var xtvd = xmlDoc.getElementsByTagName('GetGridScheduleResult')[0]
	PARSE_DATA.time_start = parseDate(getNodeAttribute(xtvd,'StartDate'))
	PARSE_DATA.time_end = new Date(PARSE_DATA.time_start.getTime()+parseInt(getNodeAttribute(xtvd,'Duration'),10)*60*1000)
	TIME_LENGTH = (PARSE_DATA['time_end']-PARSE_DATA['time_start'])/(TIME_INC)
	//get stations
	var stations = getNodeByName(xtvd,'GridChannels')
	PARSE_DATA.channels = []
	for (var j =0; j< stations.childNodes.length; j++) { var node = stations.childNodes[j];
		var channel = new Object()
		channel.channelId = getNodeAttribute(node,'Channel')
		channel.name=getNodeAttribute(node,'DisplayName')
		channel.icon_url = getNodeAttribute(getNodeByName(getNodeByName(node,'ChannelImages'),'ImageGrid'),'ImageUrl')
		channel.icon = null
		try { 	
			var icon = document.createElement('img')
			icon.src = 'image.php?width=75&height=75&URL="'+channel.icon_url+'"'
			channel.icon = icon
			
		} catch (err) {}
		channel.programs = []
		var airings = getNodeByName(node,'Airings')
		for (var k =0; k< airings.childNodes.length; k++) { var a_node = airings.childNodes[k];
			var program = new Object()
			program.title = getNodeAttribute(a_node,'Title')
			program.episode = getNodeAttribute(a_node,'EpisodeTitle')
			program.programId = getNodeAttribute(a_node,'ProgramId')
			program.time_start = parseDate(getNodeAttribute(a_node,'AiringTime'))
			program.time_end = new Date(program.time_start.getTime()+parseInt(getNodeAttribute(a_node,'Duration'),10)*60*1000)
			channel.programs.push(program)
		}
		PARSE_DATA.channels.push(channel)
	}
	NUM_CHANNELS = PARSE_DATA.channels.length
	console.log('parsed data! number of channels: ',NUM_CHANNELS)
	if (USE_NODE()) { setTimeout(UI(ctx),10); }
	else setTimeout("UI(ctx)",10);
}

function parse() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4) {
			if (USE_NODE()) {
				parseXML(xml.parseFromString(this.responseText))
			}
			else parseXML(this.responseXML.documentElement)
		}
	}
	xmlhttp.open("GET","http://localhost/EPG/listings.php")
	xmlhttp.send()

}

function keyDown(e) {
	if (e.keyIdentifier == 'Left') {
		TIME_INDEX-=3; if (TIME_INDEX < 0) TIME_INDEX = 0;
	} 
	else if (e.keyIdentifier=='Up') {
		CHANNEL_INDEX-=CHANNEL_INC; if (CHANNEL_INDEX < 0) CHANNEL_INDEX = 0
	} 
	else if (e.keyIdentifier=='Right') {
		TIME_INDEX+=3; if (TIME_INDEX >= TIME_LENGTH) TIME_INDEX = TIME_LENGTH
	} 
	else if (e.keyIdentifier=='Down') {
		CHANNEL_INDEX+=CHANNEL_INC; if (CHANNEL_INDEX > (NUM_CHANNELS-CHANNEL_INC)) CHANNEL_INDEX = NUM_CHANNELS-CHANNEL_INC
	} 
}

function mouseDown(e) {
	mouse.x = e.offsetX; mouse.y = e.offsetY; mouse.flag = 1;
}

function pad(number, length) { var str = '' + number; while (str.length < length) { str = '0' + str; } return str; }

function formatTime(s) {
	hr = s.getHours(), hrm = hr%12; if (hrm == 0) hrm = 12
	return hrm + ':'+pad(s.getMinutes(),2)+' '+((hr>12)?'PM':'AM')
}

function parseTime(s) {
	var h = parseInt(s.substr(2,2),10), m = parseInt(s.substr(5.2),10)
	return (h*60+m);
}

function fillText(s,x,y,w,h) {
	var words = s.split(' ')
	var px = x, py = y
	for (var j = 0; j<words.length; j++) {
		ctx.fillText(words[j],px,py)
		mx = ctx.measureText(words[j]).width+10
		px += mx
		if (px > (x + w)) { px = x; py += 32; }
	}
}


var frame = 0, FPS=  0, t1 = new Date(), t2 = new Date();


function UI(ctx) {

	//draw background
	ctx.drawImage(img, 0, 0, width, height)
	//draw date

	var date_str = PARSE_DATA.startdate.toUTCString().split(' ');
	console.log(date_str[0]+date_str[1]+date_str[2]+date_str[3]+date_str[4]);
	//ctx.font = "14px sans-serif"
	date_str=date_str[0]+date_str[1]+date_str[2]+date_str[3];
	ctx.fillStyle = '#ddeeff'
	ctx.fillText(date_str[4],600,60)

	//draw time
	var t_start = new Date(PARSE_DATA.time_start+ TIME_INDEX*TIME_INC)
	t_start = new Date(t_start-t_start%(30*60*1000))
	var t_end = new Date(t_start + 3*TIME_INC)
	ctx.fillStyle = '#ffffff'
	ctx.fillText(t_start.toUTCString().split(' ')[4],10,400)
	for (j = 0; j<3; j++) {
	  var t2 = new Date(t_start + j * TIME_INC)
	  ctx.fillText(formatTime(t2),365+500*j,400)
	}
	
	//draw channels
	for (var j = 0; j<PARSE_DATA.channels.length; j++) {

		var channel = PARSE_DATA.channels[j];
		var callSign = channel.name
		//draw channel number
		ctx.fillStyle = '#113311'
		ctx.fillText(channel.channelId,100,100*j+500)
		//draw channel icon
		ctx.fillStyle = '#223344'
		if (channel.icon) try { ctx.drawImage(channel.icon,220-45,100*j+500-45) } catch (err) { channel.icon = null; } //,channel.icon.width,channel.icon.height)
		else ctx.fillText(channel.name,200,100*j+500)
		//draw programs
		var px = 0, py = 100*j + 500
		for (var si = 0; si<channel.programs.length; si++) {
			var program = channel.programs[si]
			var start = program.time_start;
			var end = program.time_end;
			if (end <= t_start || start >= t_end) continue
			px = 365 + ((Math.max(start,t_start)-t_start)/TIME_INC)*500
			var len = (Math.min(end,t_end)-Math.max(start.getTime(),t_start))/(TIME_INC)
			var rect = [px,py-65,px+500*len-10,py+25]
			if (mouse.flag == 1 && mouse.x >= rect[0] && mouse.x <= rect[2] && mouse.y >= rect[1] && mouse.y <= rect[3]) {
				if (selected_program == program)selected_program = null;
				else selected_program = program; 
				mouse.flag = 0
			}
			if (program == selected_program) ctx.fillStyle = 'rgba(22,99,22,0.4)'; else ctx.fillStyle = 'rgba(66,77,99,0.4)'; 
			ctx.save(); ctx.beginPath(); ctx.rect(rect[0],rect[1],rect[2]-rect[0],rect[3]-rect[1]); 
			ctx.fill(); ctx.clip(); 
			ctx.fillStyle = '#ffffff'; ctx.fillText(program.title,px+5, py) 
			ctx.restore()
		}
	}
	
	//draw selected program
	if (selected_program) {
		ctx.fillStyle = '#88ff88'; ctx.fillText(selected_program.title,650,100)
		var desc = ''
		if (selected_program.episode != null) desc += selected_program.episode
		desc += '  ' + formatTime(selected_program.time_start)+'-'+formatTime(selected_program.time_end)
		ctx.fillStyle = '#dddddd'; fillText(desc,660,150,650,450)
	}
	
	//draw scrollbars
	ctx.fillStyle = '#ffffff'
	ctx.fillRect(1884,436,10,500)
	ctx.fillStyle = '#0000ff'
	ctx.fillRect(363,413,1500*TIME_INDEX/TIME_LENGTH,12)
	ctx.fillRect(1884,436,10,500*CHANNEL_INDEX/NUM_CHANNELS)
	//draw FPS
	ctx.fillStyle = '#ffffff'; ctx.fillText('FPS: '+Math.floor(FPS),1200,60)
	frame += 1
	if (frame == 30) {
		t2 = new Date()
		FPS = 30000/(t2.getTime()-t1.getTime())
		frame = 0; t1 = t2
	}
	if (USE_NODE()) { ctx.flip(); setTimeout(UI(ctx),10); }
	else setTimeout("UI(ctx)",10);
}
