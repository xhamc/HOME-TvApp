	function start(){

		var stations;
		var epg;

		 function WebSocketTest()
		 {
			if ("WebSocket" in window)
			{
			   console.log("WebSocket is supported by your Browser!");

			   // Let us open a web socket
			   var ip="ws://"+location.host;
				console.log("IP address: "+ip);
			   ws= new WebSocket(ip);
			   var id;
			   ws.onopen = function()
			   {
				updateGuideData();
			   	//var intervalTimer=setInterval(pingWebSocket,2000);
			   	var guideUpdateTimer=setInterval(updateGuideData,1000);
			   };

			   ws.onmessage = function (evt)
			   {
				  var received_msg = evt.data;
				  console.log("Message is received..." + received_msg);
				  if (received_msg.startsWith("{") && received_msg.endsWith("}")){

						var obj=JSON.parse(received_msg);
						var keyChannel, keyDate, keyTime, keyMetadata, stations, keyStation;
						for (key in obj){
							if (key=="EPG"){
								epg=obj[key];
                                for (keyChannel in epg){
                                    console.log("channel:" + keyChannel);
                                    var dateArray=epg[keyChannel];
									for (keyDate in dateArray){
										console.log("  date: "+keyDate);
										var timeArray=dateArray[keyDate];
										for (keyTime in timeArray){
											console.log("      time: "+keyTime);
											var metadata=timeArray[keyTime];
											for (keyMetadata in metadata){
												console.log("        "+keyMetadata +"->"+metadata[keyMetadata]);
											}
											if (null==EPG_DATA[keyChannel]){
												EPG_DATA[keyChannel]={};
											}
											if (null==EPG_DATA[keyChannel].metadata){

												EPG_DATA[keyChannel].metadata=[metadata];

											}
											if (metadata.start<EPG_DATA[keyChannel].metadata[0].start){
												var l=EPG_DATA[keyChannel].metadata.length;
												for (var i=l; i>0; i--){

													EPG_DATA[keyChannel].metadata[i]=EPG_DATA[keyChannel].metadata[i-1];
												}
												EPG_DATA[keyChannel].metadata[0]=metadata;

											}else if (metadata.start>EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length-1].start){

												EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length]=metadata;

											}
										}

										console.log("EPG_DATA length:"+EPG_DATA[keyChannel].metadata.length);
										console.log("EPG_DATA start:"+EPG_DATA[keyChannel].metadata[0].start);
										console.log("EPG_DATA end:"+EPG_DATA[keyChannel].metadata[EPG_DATA[keyChannel].metadata.length-1].start);
									}

									var channelArray=[];
									var last=currentChannelGridOffset+4;
									if (lastChannelAvailable<last) last=lastChannelAvailable;
									for (var i=currentChannelGridOffset; i<=last;i++){
										channelArray.push(STATION_DATA[i].channelId);
									}
									setChannelListData(channelArray);
									udpdateEPGinProgress=false;
                                }

							}else if (key=="STATIONS"){
								var stationData;
								stations= obj[key];

								for (keyStation in stations){
									console.log(keyStation);
									for (stationData in stations[keyStation]){
											console.log("   "+stationData+" -> " + stations[keyStation][stationData]);

									}
									var channel=new Object();
									channel.name=stations[keyStation]["callSign"];
									channel.icon_url = stations[keyStation]["channelIcon"];
									channel.channelId = stations[keyStation]['channelID'];
									var img=new Image();
									img.src=channel.icon_url;
									channel.icon = img;

									var l=STATION_DATA.length;
									var newStation=true;
									for (var i=0; i<l; i++){
										if (STATION_DATA[i].channelId == channel.channelId ){

											newStation=false;
											break;
										}
									}
									if (newStation){

										STATION_DATA[l]=channel;

									}
									updateChannelList=false;
								}
								if (CHANNELLIST_DATA.length==0){
									for (var i=0; i<5;i++){
										CHANNELLIST_DATA[i]=STATION_DATA[i].channelId;

									}
									setChannelListData(CHANNELLIST_DATA);
									ws.send("browseEPGchannel");
									udpdateEPGinProgress=true;
									lastChannelAvailable=4;


								}
							}else if (key=="TIMELIST"){
								var timeListArray=obj["TIMELIST"];
								if (TIMELIST_DATA[0]>timeListArray[0]){
									TIMELIST_DATA[0]=timeListArray[0];
									console.log(" TIMELIST DATA[0]:"+TIMELIST_DATA[0] );
								}
								if (TIMELIST_DATA[1]<timeListArray[1]){
									TIMELIST_DATA[1]=timeListArray[1];
									console.log(" TIMELIST DATA[1]:"+TIMELIST_DATA[1] );
								}

								if (null==offScreenLastGridPoint && TIMELIST_DATA[1]!=0){
									offScreenLastGridPoint=TIMELIST_DATA[1]-60000;
								}
								updateTimeList=false;


							}else if (key=="CHANNELLIST"){
								var channelListArray=obj["CHANNELLIST"];
								for (var i=0; i<channelListArray.length; i++){
									CHANNELLIST_DATA[i]=channelListArray[i];
								}

								updateChannelList=false;
							}
						}
					}


				};


			   ws.onclose = function()
			   {
				  // websocket is closed.
				  console.log("Connection is closed...");

			   };

			   function pingWebSocket(){
			   	ws.send("ping");
			   	console.log("Ping sent");
			   }
			}

			else
			{
			   // The browser doesn't support WebSocket
			   console.log("WebSocket NOT supported by your Browser!");
			}

		}
		initialize();
		WebSocketTest();

	}

	var background;

	var stationDisplay=[];

	var GRIDINTERVAL=30*60*1000;
	var VERTCELLSIZE=(1070-160)/10;
	var HORIZCELLSIZE=(1905-300)/5;
	var ctx;
	var gridTimeStartIntervalTimer;
	var gridUpdateTimer;
	var EPG_DATA={};
	var STATION_DATA=[];
	var TIMELIST_DATA=[];
	var CHANNELLIST_DATA=[];
	var ws;
	var originalGridTimeStart;
	var gridTimeStart;
	var lastGridPoint;
	var midSelectionPoint;
	var offScreenLastGridPoint;
	var currentProgramSelected;
	var currentChannelGridOffset=0;
	var lastChannelAvailable=0;
	var udpdateEPGinProgress=false;
	var updateTimeList=false;
	var updateChannelList=false;

	function initialize(){
		canvas = document.getElementById('canvas');
		window.addEventListener("keypress",keyDown, false);
		width = canvas.width;
		height = canvas.height;
		ctx = canvas.getContext('2d');
		ctx.font = "30px arial";
		background = new Image();
		mouse = new Object(); mouse.x = 0; mouse.y = 0; mouse.flag = 0; selected_program = null;
		background.src = 'img/background.png';
		gridTimeStartUpdate();
		gridTimeStartIntervalTimer=setInterval('gridTimeStartUpdate()',1000*60*1);
		gridUpdateTimer=setInterval('grid()',100);
		TIMELIST_DATA[0]=NaN;
		TIMELIST_DATA[1]=0;


    }

    function updateGuideData(){
		ws.send("UpdateStations");
		ws.send("UpdateEPG");
		updateTimeList=true;
		getTimeList();
		getChannelList();
    }

    function getTimeList(){
    	ws.send("getTimeList");
    	updateTimeList=true;
    }

	function getChannelList(){
		ws.send("getChannelList");
		updateChannelList=true;
	}


	function setTimeList(t1, t2){
		var ja=[t1,t2];
		var jo={"setTimeList":ja};
		ws.send(JSON.stringify(jo));
	}

	function setChannelListData(channelData){

		var jo={"setChannelList":channelData};
		console.log("setChannelList: "+jo.setChannelList[0]+"  "+jo.setChannelList[1]+"  "+jo.setChannelList[2]+"  "+jo.setChannelList[3]+"  "+jo.setChannelList[4])
		ws.send(JSON.stringify(jo));
//		ws.send("browseEPGchannel");
	}



	function grid(){
		ctx.drawImage(background, 0, 0, width, height);
		ctx.save();
		ctx.font = "50px arial";
		ctx.fillStyle='#FFFFFF';
		var now=getTime();
		ctx.fillText ('TODAY  |   '+ now.date+ '    '+now.time, 120,80);

		ctx.strokeStyle='#FFFFFF';
		ctx.globalAlpha=0.5;
		ctx.lineWidth=2;
		ctx.strokeRect(5,5,1905,1065);
		ctx.globalAlpha=0.1;
		ctx.beginPath();
		ctx.moveTo(5,160);
		ctx.lineTo(1910,160);
		for (var i=0; i<5; i++){
				ctx.moveTo(300+i*HORIZCELLSIZE,160);
        		ctx.lineTo(300+i*HORIZCELLSIZE,1070);
		}

			ctx.moveTo(5,160+VERTCELLSIZE);
			ctx.lineTo(1905,160+VERTCELLSIZE);

		for (var i=2; i<10; i++){
			ctx.moveTo(300-10,160+i*VERTCELLSIZE);
			ctx.lineTo(300,160+i*VERTCELLSIZE);

		}
		ctx.stroke();
		ctx.globalAlpha=1;
		ctx.font = "40px arial";
		ctx.textBaseline="middle";

		for (var i=2; i<10; i++){

			var displayTimeOnGrid=getTime(gridTimeStart+i*GRIDINTERVAL);

			ctx.fillText (displayTimeOnGrid.time, 80, 160+i*VERTCELLSIZE);
		}

		ctx.strokeStyle='#FF0000';
		ctx.globalAlpha=0.75;
		ctx.beginPath();

		var nowGridOffset= (getTime().ms - getTime(gridTimeStart).ms)* VERTCELLSIZE /GRIDINTERVAL;

		if (nowGridOffset>VERTCELLSIZE){			//display starts one cell past the gridstart
			ctx.moveTo(300, 160 + nowGridOffset);
			ctx.lineTo(1905, 160 + nowGridOffset);
			ctx.stroke();
		}
		ctx.restore();
		drawPrograms();

	}

	function drawPrograms(){
		ctx.save();
		if (null!=STATION_DATA ){
			ctx.font = "50px arial";
			ctx.fillStyle='#FFFFFF';
			var gridMax=Math.min(currentChannelGridOffset+5, STATION_DATA.length);
			for (var i=currentChannelGridOffset; i<gridMax; i++){
				ctx.fillText (STATION_DATA[i].name, 300+(i-currentChannelGridOffset)*HORIZCELLSIZE+100, 140+VERTCELLSIZE);
				ctx.drawImage(STATION_DATA[i].icon, 300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10, 160 + VERTCELLSIZE-10-75, 75,75);

			}

			if (null!=EPG_DATA){

				for (var i=currentChannelGridOffset; i<gridMax; i++){

					if (null!=EPG_DATA[STATION_DATA[i].channelId]){
	//					console.log("found  program in station:" + STATION_DATA[i].channelId );

						var j=0;
						var m=EPG_DATA[STATION_DATA[i].channelId].metadata;
	//					console.log("program metadata time length:" + m.length	 );


						while (j<m.length){
	//						console.log("program start: " + m[j].start +"  gridstart: " +gridTimeStart	+ "count: "+j );
							var start =parseInt(m[j].start);
							var end =parseInt(m[j].start) + parseInt(m[j].length);
							if (end>gridTimeStart) break;
							j++;
						}

						lastGridPoint=gridTimeStart+GRIDINTERVAL*10;
	//					console.log("last grid point:" +lastGridPoint);

						ctx.fillStyle='#0000FF';
						ctx.strokeStyle='#FFFFFF';
						ctx.lineWidth=1;
						ctx.globalAlpha=0.2;
						ctx.fillStyle='#7F7F7F';

						while (j<m.length )
						{

							var start =parseInt(m[j].start);
							var end =parseInt(m[j].start) + parseInt(m[j].length);

							if (start<lastGridPoint  && end>gridTimeStart){
								var truncated =false;
								if (start<(gridTimeStart+GRIDINTERVAL)){
									start = (gridTimeStart+GRIDINTERVAL);
									truncated=true;
								}
								if (end>lastGridPoint) {
									end = lastGridPoint;
									truncated=true;
								}

								var y=160+ (start-gridTimeStart)*VERTCELLSIZE/GRIDINTERVAL;



								var now=getTime().ms;

								if (now>start && now < end){		//cell is current so shade

										ctx.fillRect(301+(i-currentChannelGridOffset)*HORIZCELLSIZE, y+1,HORIZCELLSIZE-2, m[j].length*VERTCELLSIZE/GRIDINTERVAL -2);
								}



								if (y>160 && y< lastGridPoint)			//no start line needed (could be before this anyway)
								{
									ctx.beginPath();
									ctx.moveTo(300+(i-currentChannelGridOffset)*HORIZCELLSIZE,y);
									ctx.lineTo(300+((i-currentChannelGridOffset)+1)*HORIZCELLSIZE,y);
									ctx.stroke();
								}

								var space=(end-start)*VERTCELLSIZE/GRIDINTERVAL;


								if (space>VERTCELLSIZE/4 && space<=VERTCELLSIZE/3 && !truncated){
									drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 2, 3 );
								}else if (space>VERTCELLSIZE/3 && space<=VERTCELLSIZE/2 && !truncated){
									drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 1, 2 );
								}else if (space>VERTCELLSIZE/2 && space<=VERTCELLSIZE){
									drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/2, HORIZCELLSIZE-20 ,m[j].title, 0, 1 );
								} else if (space>VERTCELLSIZE){
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+space/3, HORIZCELLSIZE-20 ,m[j].title, 0, 1);
										drawTextInBox(300+(i-currentChannelGridOffset)*HORIZCELLSIZE+10,y+2*space/3, HORIZCELLSIZE -20 ,m[j].programTitle, 1, 2);
								}
							}

							j++;
						}
					}
				}


				if (null==currentProgramSelected){
					if (null!=EPG_DATA && null!=STATION_DATA){
						if (null!=STATION_DATA[currentChannelGridOffset]){
							if (null!=EPG_DATA[STATION_DATA[currentChannelGridOffset].channelId]){
								if (null!=EPG_DATA[STATION_DATA[currentChannelGridOffset].channelId].metadata){
									updateSelection(currentChannelGridOffset);
								}
							}
						}
					}

				} else{
					drawSelection();
				}

			}
			ctx.restore();


		}


	}

	function drawTextInBox(x, y, w, t, largest, smallest){

		ctx.save();
		ctx.fillStyle='#FFFFFF';
		ctx.textBaseline="middle";
		ctx.globalAlpha=1.0;

		var fonts=["40px arial","30px arial","25px arial","20px arial","15px arial", "12px arial", "10px arial", "8px arial"];
		var dim;
		var left;
		var actualWidth;
		var i=largest;
		do{
			ctx.font=fonts[i];
			dim=ctx.measureText(t);
			i++;
			actualWidth=dim.width;
			//console.log("font: "+ ctx.font + "  width: "+ actualWidth);
		}while (actualWidth>w && i<=smallest)

		if (actualWidth>w){
			do{

				t=t.substring(0,t.length - 3)+'..';
				dim=ctx.measureText(t);
				actualWidth=dim.width;

			}while (actualWidth>w)
		}


		left =x+(w-actualWidth)/2;
		ctx.fillText(t,left, y, w);

		ctx.restore();

	}

	function drawSelection(){
		var s=getSelectionParameters();
		ctx.save();
		ctx.strokeStyle='#0000FF';
		ctx.lineWidth=3;
		ctx.globalAlpha=0.5;

		ctx.strokeRect(s.x,s.y, HORIZCELLSIZE, s.h  );
		ctx.restore();
	}

	function getSelectionParameters(){
			index=currentProgramSelected.item;
			channel=currentProgramSelected.channel;

    		var m=EPG_DATA[STATION_DATA[channel].channelId].metadata[index];
			var start =parseInt(m.start);
			var end =parseInt(m.start) + parseInt(m.length);

			if (start<lastGridPoint  && end>gridTimeStart){
				var truncated =false;
				if (start<(gridTimeStart+GRIDINTERVAL)){
					start = (gridTimeStart+GRIDINTERVAL);
					truncated=true;
				}
				if (end>lastGridPoint) {
					end = lastGridPoint;
					truncated=true;
				}


			}
			midSelectionPoint=(start +end)/2;
			var y=160+ (start-gridTimeStart)*VERTCELLSIZE/GRIDINTERVAL;
			var h= (end-start)*VERTCELLSIZE/GRIDINTERVAL;
			var x= 300 +(channel-currentChannelGridOffset)*HORIZCELLSIZE;

			selectionParameter={"x":x,"y":y, "h":h, "mid":midSelectionPoint};
			return selectionParameter;
	}


	function updateSelection(channel, item){
    		var m=EPG_DATA[STATION_DATA[channel].channelId].metadata;
			if (null==item){
    			var time=getTime().ms;

				for (var i=0; i<m.length; i++){
					var start=parseInt(m[i].start);
					var end =start + parseInt(m[i].length);
					if (start<=time && end >time){
						item=i;
						break;
					}
				}
				if (null==item) return;
    		}
			currentProgramSelected={'channel':channel,'item':item};
	}


    function gridTimeStartUpdate(){

		var now=getTime();
		gridTimeStart= Math.floor(now.ms/GRIDINTERVAL)*GRIDINTERVAL - GRIDINTERVAL*3;
		if (null==originalGridTimeStart) originalGridTimeStart=gridTimeStart;
		lastGridPoint=gridTimeStart+GRIDINTERVAL*10;
		console.log("offScreenLastGridPoint: "+ offScreenLastGridPoint+ " TIMELIST_DATA[1]: "+TIMELIST_DATA[1]);
		if (null==offScreenLastGridPoint && TIMELIST_DATA[1]!=0){
			offScreenLastGridPoint=TIMELIST_DATA[1];
		}
		if (null!=currentProgramSelected){



			var s=getSelectionParameters();
			console.log("selection parameters: "+s.mid)
			if (s.mid<(gridTimeStart+GRIDINTERVAL*2)){
				var e={"keyIdentifier":"Down"};
				keyDown(e);
			}

		}
		scrollGridUp();

    }

    function scrollGridUp(){

    		if ( (offScreenLastGridPoint-gridTimeStart-GRIDINTERVAL*15 )<0 && !udpdateEPGinProgress ){
    			var oldoffScreenLastGridPoint=offScreenLastGridPoint;
    			offScreenLastGridPoint+=GRIDINTERVAL*20;
    			setTimeList (oldoffScreenLastGridPoint, offScreenLastGridPoint);
    			console.log("offScreenLastGridPoint: "+offScreenLastGridPoint+ "oldoffScreenLastGridPoint: "+oldoffScreenLastGridPoint);

    			ws.send("browseEPGtime");
    			udpdateEPGinProgress=true;
    		}else if (udpdateEPGinProgress){
    			setTimeout('ScrollGridUp()', 1000);
    		}
	}

	function scrollGridRight(){

			if ( (lastChannelAvailable-currentChannelGridOffset- 4 )<0 && !udpdateEPGinProgress ){

				if (lastChannelAvailable==STATION_DATA.length-1) return;

				lastChannelAvailable+=1;
				getNewChannel(lastChannelAvailable);
				console.log("lastChannelAvailable: "+lastChannelAvailable);
				ws.send("browseEPGchannel");
				udpdateEPGinProgress=true;

			}
	}

	function scrollGridLeft(){

			if ( currentChannelGridOffset>0 && !udpdateEPGinProgress ){
				getNewChannel(currentChannelGridOffset-1);
				console.log("firstChannelAvailable: "+ (currentChannelGridOffset-1));
				ws.send("browseEPGchannel");
				udpdateEPGinProgress=true;
			}
	}

	function getNewChannel(channel){

		console.log("getting new channel: channel" + channel + "station data length: "+ STATION_DATA.length);

		console.log("STATION_DATA: " + STATION_DATA[channel].channelId);
		setTimeList (originalGridTimeStart, offScreenLastGridPoint);
		var channelArray=[STATION_DATA[channel].channelId]
		setChannelListData(channelArray);

	}

	function getTime(ms){
		if (null!=ms)
			var today=new Date(ms);
		else
			var today=new Date();
		var jsonTime;
		var todayUTCstring = today.toUTCString();
		var displayTodayArray=todayUTCstring.split(' ');
		var displayDate=displayTodayArray[0]+' '+displayTodayArray[1]+' '+displayTodayArray[2]+' '+displayTodayArray[3];
		var displayTime=today.getHours()+":";
		var displayMins=today.getMinutes()<10?("0"+today.getMinutes()):(""+today.getMinutes());
		var displayTime=displayTime + displayMins;
		jsonTime={'ms':today.getTime(),"date":displayDate, "time": displayTime};
		return jsonTime;

	}

	function keyDown(e){

		if (e.keyIdentifier == 'Left') {


			if (null!=currentProgramSelected){
				if (currentProgramSelected.channel > 0){
					var channelIndex=currentProgramSelected.channel-1;

					var m=EPG_DATA[STATION_DATA[channelIndex].channelId].metadata;
					for (var i=0; i<m.length; i++){
						var start=parseInt(m[i].start);
						var end=start + parseInt(m[i].length);
						if (start>midSelectionPoint) break;			//first one is already past
						if (start<=midSelectionPoint && end>midSelectionPoint) break;	//ideally
					}
					if (i==m.length) i=m.length-1; //went beyond, go back one;
					var item=i;
					updateSelection(channelIndex, item);

					if ((channelIndex-currentChannelGridOffset)<2 && currentChannelGridOffset>0){
						currentChannelGridOffset--;
						scrollGridLeft();
					}


				}
			}

		}
		else if (e.keyIdentifier=='Up') {

			console.log("Up Pressed");


			if (null!=currentProgramSelected){
//				var m=EPG_DATA[STATION_DATA[currentProgramSelected.channel].channelId].metadata[currentProgramSelected.item];

				if (currentProgramSelected.item>0){

					var m=EPG_DATA[STATION_DATA[currentProgramSelected.channel].channelId].metadata;
					var item = currentProgramSelected.item - 1;

					console.log("incrementing index selection to "+item);

					updateSelection(currentProgramSelected.channel, item);
//					var oldGridTimeStart=gridTimeStart;
					while ((parseInt(m[item].start) - gridTimeStart)/GRIDINTERVAL<2){
						gridTimeStart=gridTimeStart-GRIDINTERVAL;
					}
					if ((oldGridTimeStart-gridTimeStart)>0){

//						setTimeList (oldGridTimeStart, gridTimeStart );
//						ws.send("browseEPGtime");
						var s=getSelectionParameters();
						midSelectionPoint=s.mid;
					}

				}

			}

		}
		else if (e.keyIdentifier=='Right') {

			if (null!=currentProgramSelected){
				if (currentProgramSelected.channel < lastChannelAvailable){
					var channelIndex=currentProgramSelected.channel+1;

					var m=EPG_DATA[STATION_DATA[channelIndex].channelId].metadata;
					for (var i=0; i<m.length; i++){
						var start=parseInt(m[i].start);
						var end=start + parseInt(m[i].length);
						if (start>midSelectionPoint) break;			//first one is already past
						if (start<=midSelectionPoint && end>midSelectionPoint) break;	//ideally
					}
					if (i==m.length) i=m.length-1; //went beyond, go back one;
					var item=i;
					updateSelection(channelIndex, item);

					if ((channelIndex-currentChannelGridOffset)>3){
					 	currentChannelGridOffset++;
						scrollGridRight();
					}


				}
			}


		}
		else if (e.keyIdentifier=='Down') {
		console.log("Down Pressed");


			if (null!=currentProgramSelected){
				var m=EPG_DATA[STATION_DATA[currentProgramSelected.channel].channelId].metadata;

				if (currentProgramSelected.item<(m.length-1)){
					var item = currentProgramSelected.item + 1;
					console.log("incrementing index selection to "+item);

					updateSelection(currentProgramSelected.channel, item);
					var oldGridTimeStart=gridTimeStart;

					while ((parseInt(m[item].start) - gridTimeStart)/GRIDINTERVAL>7){
						gridTimeStart=gridTimeStart+GRIDINTERVAL;
					}
					if ((gridTimeStart-oldGridTimeStart)>0){

						lastGridPoint=gridTimeStart+GRIDINTERVAL*10
						console.log("offScreenLastGridPoint: "+ offScreenLastGridPoint +"lastGridPoint: " + "sum: " +(offScreenLastGridPoint-lastGridPoint-GRIDINTERVAL*4 ));

						scrollGridUp();

						var s=getSelectionParameters();
						midSelectionPoint=s.mid;
//						if ( (offScreenLastGridPoint-gridTimeStart-GRIDINTERVAL*15 )<0 && !udpdateEPGinProgress ){
//							var oldoffScreenLastGridPoint=offScreenLastGridPoint;
//							offScreenLastGridPoint+=GRIDINTERVAL*20;
//							setTimeList (oldoffScreenLastGridPoint, offScreenLastGridPoint);
//							console.log("offScreenLastGridPoint: "+offScreenLastGridPoint+ "oldoffScreenLastGridPoint: "+oldoffScreenLastGridPoint);
//
//							ws.send("browseEPGtime");
//							udpdateEPGinProgress=true;
//						}
					}

				}

			}
		}


	}

