function GuideController(){


//GLOBALS:
	GRIDINTERVAL=30*60*1000;
	EPG_DATA={};					//Incoming EPG data object
	STATION_DATA=[];				//Incoming Station data
	CHANNELLIST_DATA=[];			//Converted/Sorted station data
	udpdateEPGinProgress=false;
	stationDataAvailable=false;
	epgDataAvailable=false;
	favoritesAvailable=false;
	currentChannelGridOffset=0;
	gridTimeStart=0;

//END GLOBALS
	var GUIDE_FETCH_REQUEST_TIMER=1000;
	var STATION_FETCH_REQUEST_INT=5;
	var GUIDE_WEBSOCKET_TIMER=200;


	var TIMELIST_DATA=[];
	var currentAvailableDataRange;			//tracks the start and end times of all the channels
	var updateTimeNext=true;				//flag to decide whether to update the data for current set of 5 channels or go to next set of channels
	var updateChannelStart=0				//Tracks the start to the group of channels to update next
	var wsopen=false;						//one time flag for opening the websocket;

    var ws=new WebsocketController(); if (!ws.supported) return;
    var gc=new GridController(ws);
	var originalGridTimeStart=gc.initialize();
	var MAXTTIME=10*24*3600*1000;	//start with 10 days of maximum data!

	guideWebsocketControllerTimer(); //TODO switch to callbacks for efficiency
	var waitTooLong=0;
	function guideWebsocketControllerTimer(){

		if (!wsopen && ws.open){
			console.log("open");
			stationDataFetchRequest();
			wsopen=true;
		}else if (stationDataAvailable){
			console.log("sort");
			waitTooLong=0;
			sortChannelListData();
			stationDataAvailable=false;
			guideDataFetchRequest();
//		}else if (favoritesAvailable){
//			console.log("favorites");
//			waitTooLong=0;
//			favoritesAvailable=false;
//			guideDataFetchRequest();
		}else if (epgDataAvailable){
			console.log("epgData");
			waitTooLong=0;
			epgDataAvailable=false;
			guideDataReceived();
		}else if (waitTooLong>15000/GUIDE_WEBSOCKET_TIMER){
			console.log("Wait Too Long: "+waitTooLong);
			waitTooLong=0;
			guideDataFetchRequest();
		}
		waitTooLong++;
		setTimeout(guideWebsocketControllerTimer,GUIDE_WEBSOCKET_TIMER);
	}



	function sortChannelListData(){

		//initialize the channelist data if needed.
		//TODO: use preferences to turn random STATION_DATA into customized STATION_DATA (could be done from Java side too!)
/****************************Sort STATION_DATA by ChannelNumber into CHANNELLIST_DATA****************/
//			for (var i=0; i<STATION_DATA.length;i++){
//				var channelNumber=parseInt(STATION_DATA[i].channelNumber);
//				if (CHANNELLIST_DATA.length>0){
//					if (parseInt(CHANNELLIST_DATA[CHANNELLIST_DATA.length-1].channelNumber) < channelNumber ){
//						CHANNELLIST_DATA[CHANNELLIST_DATA.length]=STATION_DATA[i];
//					}else{
//						var j=CHANNELLIST_DATA.length;
//						do{
//							j--;
//							CHANNELLIST_DATA[j+1]=CHANNELLIST_DATA[j];
//						} while (j>0 && parseInt(CHANNELLIST_DATA[j-1].channelNumber) >= channelNumber)
//
//						CHANNELLIST_DATA[j]=STATION_DATA[i];
//
//					}
//
//
//				}else{
//					CHANNELLIST_DATA[0]=STATION_DATA[i];
//				}
//			}
/****************************Sort STATION_DATA by ChannelID into CHANNELLIST_DATA****************/
//			for (var i=0; i<STATION_DATA.length;i++){
//				var channelNumber=parseInt(STATION_DATA[i].channelId);
//				if (CHANNELLIST_DATA.length>0){
//					if (parseInt(CHANNELLIST_DATA[CHANNELLIST_DATA.length-1].channelId) < channelNumber ){
//						CHANNELLIST_DATA[CHANNELLIST_DATA.length]=STATION_DATA[i];
//					}else{
//						var j=CHANNELLIST_DATA.length;
//						do{
//							j--;
//							CHANNELLIST_DATA[j+1]=CHANNELLIST_DATA[j];
//
//						} while (j>0 && parseInt(CHANNELLIST_DATA[j-1].channelId) >= channelNumber)
//						CHANNELLIST_DATA[j]=STATION_DATA[i];
//
//					}
//				}else{
//					CHANNELLIST_DATA[0]=STATION_DATA[i];
//				}
//
//			}
/****************************Don't Sort STATION_DATA ****************/
			console.log("Sorting the channel data");
			for (var i=0; i<STATION_DATA.length;i++){
				CHANNELLIST_DATA[i]=i;
			}

			if (null==currentAvailableDataRange){
				currentAvailableDataRange=new Array(CHANNELLIST_DATA.length);
				for (var i=0; i<CHANNELLIST_DATA.length; i++){
						currentAvailableDataRange[i]={"firstProgramStart":originalGridTimeStart, "endProgramStart":originalGridTimeStart}; //initialize
				}
			}

//
//			for (var i=0; i<CHANNELLIST_DATA.length; i++) { console.log(CHANNELLIST_DATA[i].channelId); }
////			lastChannelAvailable=CHANNELLIST_DATA.length-1;
	}

	function favoriteChannelRequest(){
		if (!udpdateEPGinProgress){
			ws.send("getFavorites", true);
		}else{
			setTimeout(favoriteChannelRequest(),1000);
		}
	}

//	function setFavorites(){
//		console.log("Setting favorites");
//		for (var i=0; i<STATION_DATA.length;i++){
//			CHANNELLIST_DATA[i]=STATION_DATA[i];
//			if (CHANNELLIST_DATA[i].favorite==true){
//				console.log("Favorites: "+CHANNELLIST_DATA[i].channelId);
//			}
//		}
//	}



	function guideDataReceived(){


		for (var i=0; i<CHANNELLIST_DATA.length; i++){
			if (null!=EPG_DATA[STATION_DATA[CHANNELLIST_DATA[i]].channelId]){
				var m=EPG_DATA[STATION_DATA[CHANNELLIST_DATA[i]].channelId].metadata;
				var l=m.length;

				if (null!=currentAvailableDataRange[i]){
					var start= Math.max(m[0].start, currentAvailableDataRange[i].firstProgramStart);
					var end= Math.max(m[l-1].start, currentAvailableDataRange[i].endProgramStart );

				}else{
					var start =parseInt(m[0].start);
					var end= parseInt(m[l-1].start);
				}
				currentAvailableDataRange[i]={"firstProgramStart":start, "endProgramStart":end};
			}
		}
	}

	function stationDataFetchRequest(){
			console.log("Fetching station data");
			if (!udpdateEPGinProgress){
    			if (CHANNELLIST_DATA.length==0){
    				ws.send("browseEPGStations", true);
    			}
			}
	}

	var getStations=STATION_FETCH_REQUEST_INT;
	function guideDataFetchRequest(){

		console.log("Fetching guide data");

		if (!udpdateEPGinProgress){
			//getStations--;
//			if (getStations<=0){
//				getStations=STATION_FETCH_REQUEST_INT;
//				ws.send("browseEPGStations", true);
//				return;
//			}else{
			var updateRequest=getNextTimeAndChannelList();
	//				if (null!=updateRequest){
				if (updateRequest.vis){
					console.log("Visible update");
					var msg=JSON.stringify({"browseEPGData":updateRequest.req});
				}else{
					console.log("non Visible background data update");
					var msg=JSON.stringify({"searchEPGCache":updateRequest.req});
				}

				console.log("msg:"+msg);
				ws.send(msg, true);
//				}
//			}

		}

		setTimeout(guideDataFetchRequest,GUIDE_FETCH_REQUEST_TIMER);
	}




	var maxTimeLast=originalGridTimeStart+2*GRIDINTERVAL*10;
	var toggle=false;
	function getNextTimeAndChannelList(){
		var channelData=[];
		var timeData=[];
		var end=gridTimeStart+GRIDINTERVAL*10;
		var index=0;
		var mxt=getTime().ms+MAXTTIME;
		var start=mxt;
		var visibleDataRequest=false;
//		toggle=!toggle;
		if (toggle){
			for (var i=0; i<Math.min(5, (CHANNELLIST_DATA.length -	currentChannelGridOffset)); i++){
//					if ((i+currentChannelGridOffset)<updateChannelStart || (i+currentChannelGridOffset)>updateChannelStart+5){
				try{
					var m=EPG_DATA[STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId].metadata;

					if (m.length==0){
						start = originalGridTimeStart;
						channelData[index]=STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId;
						console.log("Visible Channel: No data: " +STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelNumber);
						index++;


					}
					else if (m.length==1){

						if ( parseInt(m[0].start)>originalGridTimeStart || (parseInt(m[0].start)+parseInt(m[0].length)) < end){
							start = originalGridTimeStart;
							channelData[index]=STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId;

							console.log("Visible Channel: start or end missing: "+STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelNumber);
							console.log(" start: "+parseInt(m[0].start)+"original start: "+originalGridTimeStart + " end: "+(parseInt(m[0].start)+parseInt(m[0].length))+ "grid end: "+ end);
							index++;
						}

					}else if ((parseInt(m[m.length-1].start)+parseInt(m[m.length-1].length)) < end){
							start = originalGridTimeStart;
							channelData[index]=STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId;

							console.log("Visible Channel: <END:: "+STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelNumber);
							console.log(" data end: "+(parseInt(m[m.length-1].start)+parseInt(m[m.length-1].length))+" end: "+end);
							index++;


					}else{
						var hole=false;

						for (var j=1; j<m.length; j++){
							if ( parseInt(m[j].start)<end && (parseInt(m[j-1].start)+parseInt(m[j-1].length))>originalGridTimeStart && parseInt(m[j].start)>(parseInt(m[j-1].start)+parseInt(m[j-1].length)+10*60*1000)){		//channel to channel holes with 10 minute EPG error allowance
								hole=true;
//								if (end<(parseInt(m[j].start)+parseInt(m[j].length))){
//									end=parseInt(m[j].start)+parseInt(m[j].length);
//								}
							}
						}
						if (hole){
							start = originalGridTimeStart;
							channelData[index]=STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId;
							console.log("Visible Channel: Hole: "+STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelNumber+
							  	"start: "+ getTime(originalGridTimeStart).time
							   +" end: "+getTime(end).time);
//							console.log("start: "+parseInt(m[j-1].start)+" length: "+parseInt(m[j].length)+" next start: "+parseInt(m[j].start)+ " diff: "+(parseInt(m[j].start)-(parseInt(m[j-1].start)+parseInt(m[j-1].length))));
							index++;
						}
					}
				}catch(err){
					start = originalGridTimeStart;
					channelData[index]=STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId;
					index++;
					console.log("Visible Channel: No data: " +channelData[index] + " through try catch err: "+err);

				}
			}
			if (index>0){
				timeData[0]=start;
				timeData[1]=end;
				var jo={"CHANNELLIST":channelData, "TIMELIST":timeData};
				for (var i=0; i<channelData.length; i++){
					console.log("Visible Channels: channels"+channelData[i] + "time[0]: "+getTime(timeData[0]).date + "  " + getTime(timeData[0]).time+"  time[1]: "+getTime(timeData[1]).date+"  "+getTime(timeData[1]).time);
				}



				var updateRequest={"req":jo,"vis":true};
				return updateRequest;
			}

		}

		if (maxTimeLast>(getTime().ms+MAXTTIME)){
				var jo={"CHANNELLIST":channelData, "TIMELIST":timeData};
				var updateRequest={"req":jo,"vis":false};
				return updateRequest;
		}

		var end=maxTimeLast;
		console.log("Update by visible channels NOT");
		var numChannels=Math.min(5, (currentAvailableDataRange.length-updateChannelStart));

		for (var i=0; i<numChannels; i++){
			if (currentAvailableDataRange[i+updateChannelStart].endProgramStart<start){
				start=parseInt( currentAvailableDataRange[i+updateChannelStart].endProgramStart );
			}
			channelData[i]=STATION_DATA[CHANNELLIST_DATA[i+updateChannelStart]].channelId;

		}
//		var index=i;
//
//		for (var i=0; i<Math.min(5, (currentAvailableDataRange.length -	currentChannelGridOffset)); i++){
//				if ((i+currentChannelGridOffset)<updateChannelStart || (i+currentChannelGridOffset)>updateChannelStart+5){
//
//					if (currentAvailableDataRange[i+currentChannelGridOffset].endProgramStart<start){
//						start=currentAvailableDataRange[i+currentChannelGridOffset].endProgramStart;
//					}
//
//					if (currentAvailableDataRange[i+currentChannelGridOffset].endProgramStart<end){
//						channelData[index]=STATION_DATA[CHANNELLIST_DATA[i+currentChannelGridOffset]].channelId;
//						index++;
//					}
//				}
//		}

		timeData[0]=start;
		timeData[1]=end;


		if ((updateChannelStart+5)>=currentAvailableDataRange.length){
			updateChannelStart=0;
			maxTimeLast+=GRIDINTERVAL*10;
		}else{
			updateChannelStart+=5;
		}


		var jo={"CHANNELLIST":channelData, "TIMELIST":timeData};
		var updateRequest={"req":jo,"vis":false};
		return updateRequest;


	}


}